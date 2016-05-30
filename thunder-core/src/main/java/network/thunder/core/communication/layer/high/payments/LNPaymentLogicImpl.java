package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.ScriptTools;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.List;

import static network.thunder.core.communication.layer.high.payments.LNPaymentLogic.SIDE.*;

public class LNPaymentLogicImpl implements LNPaymentLogic {

    public static final int SIGNATURE_SIZE = 146;

    @Override
    public Transaction getChannelTransaction (TransactionOutPoint anchor, ChannelStatus channelStatus, ECKey client, ECKey server) {
        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(anchor.getHash(), anchor.getIndex(), Tools.getDummyScript());
        transaction.addOutput(Coin.valueOf(0),
                ScriptTools.getChannelTxOutputRevocation(
                        channelStatus.revoHashServerNext,
                        server,
                        client,
                        channelStatus.csvDelay));

        transaction.addOutput(Coin.valueOf(0), channelStatus.addressClient);
        transaction = addPayments(transaction, channelStatus, channelStatus.revoHashServerNext, server, client);

        //Missing two signatures, max 146B
        long fee = (long) Math.ceil((transaction.getMessageSize() + SIGNATURE_SIZE) * channelStatus.feePerByte / 2);
        transaction.getOutput(0).setValue(Coin.valueOf(channelStatus.amountClient - fee));
        transaction.getOutput(1).setValue(Coin.valueOf(channelStatus.amountServer - fee));

        return transaction;
    }

    private static Transaction addPayments
            (Transaction transaction, ChannelStatus channelStatus, RevocationHash revocationHash, ECKey keyServer, ECKey keyClient) {
        Iterable<PaymentData> allPayments = new ArrayList<>(channelStatus.paymentList);

        for (PaymentData payment : allPayments) {
            Coin value = Coin.valueOf(payment.amount);
            Script script;
            if (payment.sending) {
                script = ScriptTools.getChannelTxOutputPaymentSending(keyServer, keyClient, revocationHash, payment.secret, payment.timestampRefund);
            } else {
                script = ScriptTools.getChannelTxOutputPaymentReceiving(keyServer, keyClient, revocationHash, payment.secret, payment.timestampRefund);
            }
            transaction.addOutput(value, script);
        }
        return transaction;
    }

    @Override
    public ChannelSignatures getSignatureObject (Channel channel, Transaction channelTransaction, List<Transaction> paymentTransactions) {
        ChannelSignatures channelSignatures = new ChannelSignatures();
        channelSignatures.channelSignatures = Tools.getChannelSignatures(channel, channelTransaction);
        List<TransactionSignature> signatureList = new ArrayList<>();

        int index = 2;
        for (Transaction t : paymentTransactions) {
            TransactionSignature sig = Tools.getSignature(t, 0, channelTransaction.getOutput(index).getScriptBytes(), channel.keyServer);
            signatureList.add(sig);
            index++;
        }

        channelSignatures.paymentSignatures = signatureList;
        return channelSignatures;
    }

    @Override
    public List<Transaction> getPaymentTransactions (Sha256Hash parentTransactionHash, ChannelStatus channelStatus, ECKey keyServer, ECKey keyClient) {

        List<PaymentData> allPayments = new ArrayList<>(channelStatus.paymentList);
        List<Transaction> transactions = new ArrayList<>(allPayments.size());

        int index = 2;

        for (PaymentData payment : allPayments) {

            Transaction transaction = new Transaction(Constants.getNetwork());
            transaction.addInput(parentTransactionHash, index, Tools.getDummyScript());

            Coin value = Coin.valueOf(payment.amount);
            Script script = ScriptTools.getPaymentTxOutput(keyServer, keyClient, channelStatus.revoHashServerNext, channelStatus.csvDelay);
            transaction.addOutput(value, script);

            transactions.add(transaction);
            index++;
        }

        return transactions;
    }

    @Override
    public void checkUpdate (LNConfiguration configuration, Channel channel, ChannelUpdate channelUpdate) {
        //We can have a lot of operations here, like adding/removing payments. We need to verify if they are correct.
        ChannelStatus oldStatus = channel.channelStatus;
        ChannelStatus newStatus = oldStatus.copy();
        newStatus.applyUpdate(channelUpdate);



        //Check if the update is allowed..
        checkPaymentsInNewStatus(oldStatus, channelUpdate);
        checkRefundedPayments(channelUpdate);
        checkRedeemedPayments(channelUpdate);

        if (newStatus.amountClient < 0 || newStatus.amountServer < 0) {
            throw new LNPaymentException("Amount is negative: " + newStatus.amountServer + " " + newStatus.amountClient);
        }

        //Sufficient to test new payments
        for (PaymentData payment : channelUpdate.newPayments) {
            int diff = Math.abs(Tools.currentTime() - payment.timestampOpen);
            if (diff > configuration.MAX_DIFF_TIMESTAMPS) {
                throw new LNPaymentException("timestampOpen is too far off. Calibrate your system clock. Diff: " + diff);
            }
            diff = payment.timestampRefund - payment.timestampOpen;
            if (diff > configuration.MAX_OVERLAY_REFUND * configuration.MAX_REFUND_DELAY * OnionObject.MAX_HOPS) {
                throw new LNPaymentException("Refund timeout is too large. Is: " + diff);
            }
            //TODO Think about how we can solve guessing here, about us being the final receiver..
            if (diff < configuration.MIN_OVERLAY_REFUND * configuration.MIN_REFUND_DELAY) {
                throw new LNPaymentException("Refund timeout is too short. Is: " + diff);
            }
        }
        if (newStatus.csvDelay < configuration.MIN_REVOCATION_DELAY || newStatus.csvDelay > configuration.MAX_REVOCATION_DELAY) {
            throw new LNPaymentException("Revocation delay not within allowed boundaries. Is: " + newStatus.csvDelay);
        }
        if (newStatus.feePerByte > configuration.MAX_FEE_PER_BYTE || newStatus.feePerByte < configuration.MIN_FEE_PER_BYTE) {
            throw new LNPaymentException("feePerByte not within allowed boundaries. Is: " + newStatus.feePerByte);
        }
    }

    @Override
    public void checkSignatures
            (ECKey keyServer, ECKey keyClient, ChannelSignatures channelSignatures, Transaction channelTransaction, List<Transaction> paymentTransactions,
             ChannelStatus status) {

        Sha256Hash hash1 = channelTransaction.hashForSignature(0, ScriptTools.getAnchorOutputScript(keyClient, keyServer), Transaction.SigHash.ALL, false);
        //We only have one anchor for now..
        if (!keyClient.verify(hash1, channelSignatures.channelSignatures.get(0))) {
            throw new LNPaymentException("Anchor signature is not correct..");
        }

        List<PaymentData> allPayments = new ArrayList<>(status.paymentList);

        if (allPayments.size() != channelSignatures.paymentSignatures.size()) {
            throw new LNPaymentException("Size of payment signature list is incorrect");
        }

        for (int i = 0; i < allPayments.size(); ++i) {
            Transaction transaction = paymentTransactions.get(i);
            TransactionSignature signature = channelSignatures.paymentSignatures.get(i);
            Script scriptPubKey = channelTransaction.getOutput(i + 2).getScriptPubKey();

            Sha256Hash hash = transaction.hashForSignature(0, scriptPubKey, Transaction.SigHash.ALL, false);

            if (!keyClient.verify(hash, signature)) {
                throw new LNPaymentException("Payment Signature " + i + "  is not correct..");
            }
        }
    }

    @Override
    public Transaction getChannelTransaction (Channel channel, SIDE side) {
        if (side == SERVER) {
            return this.getChannelTransaction(
                    new TransactionOutPoint(Constants.getNetwork(), 0, channel.anchorTxHash),
                    channel.channelStatus,
                    channel.keyClient,
                    channel.keyServer);
        } else {
            return this.getChannelTransaction(
                    new TransactionOutPoint(Constants.getNetwork(), 0, channel.anchorTxHash),
                    channel.channelStatus.reverse(),
                    channel.keyServer,
                    channel.keyClient);
        }
    }

    @Override
    public ChannelSignatures getSignatureObject (Channel channel) {
        Transaction channelTx = getChannelTransaction(channel, CLIENT);
        List<Transaction> paymentTx = getPaymentTransactions(channel, CLIENT);
        return getSignatureObject(channel, channelTx, paymentTx);
    }

    @Override
    public List<Transaction> getPaymentTransactions (Channel channel, SIDE side) {
        Transaction channelTx = getChannelTransaction(channel, side);
        if (side == SERVER) {
            return this.getPaymentTransactions(
                    channelTx.getHash(),
                    channel.channelStatus,
                    channel.keyServer,
                    channel.keyClient);
        } else {
            return this.getPaymentTransactions(
                    channelTx.getHash(),
                    channel.channelStatus.reverse(),
                    channel.keyClient,
                    channel.keyServer);
        }
    }

    @Override
    public void checkSignatures (Channel channel, ChannelSignatures channelSignatures, SIDE side) {
        Transaction channelTx = getChannelTransaction(channel, side);
        List<Transaction> paymentTx = getPaymentTransactions(channel, side);

        if (side == SERVER) {
            this.checkSignatures(
                    channel.keyServer,
                    channel.keyClient,
                    channelSignatures,
                    channelTx,
                    paymentTx,
                    channel.channelStatus);
        } else {
            this.checkSignatures(
                    channel.keyClient,
                    channel.keyServer,
                    channelSignatures,
                    channelTx,
                    paymentTx,
                    channel.channelStatus.reverse());
        }
    }

    private static void checkPaymentsInNewStatus (ChannelStatus oldStatus, ChannelUpdate channelUpdate) {

        oldStatus = oldStatus.copy();
        channelUpdate = channelUpdate.getClone();

        for (PaymentData paymentData : channelUpdate.getRemovedPayments()) {
            //All of these should be in the remaining payments
            if (oldStatus.paymentList.remove(paymentData)) {
                continue;
            }
            throw new LNPaymentException("Removed payment that wasn't part of the remaining payments previously");
        }
    }

    private static void checkRefundedPayments (ChannelUpdate newStatus) {
        for (PaymentData paymentData : newStatus.refundedPayments) {
            //We reversed the ChannelStatus, so if we are receiving, he is sending
            if (!paymentData.sending) {
                throw new LNPaymentException("Trying to refund a sent payment");
            }
        }
    }

    private static void checkRedeemedPayments (ChannelUpdate newStatus) {
        for (PaymentData paymentData : newStatus.redeemedPayments) {
            if (!paymentData.sending) {
                throw new LNPaymentException("Trying to redeem a sent payment?");
            }
            if (!paymentData.secret.verify()) {
                throw new LNPaymentException("Trying to redeem but failed to verify secret.");
            }
        }
    }

}
