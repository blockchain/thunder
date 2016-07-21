package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
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
import java.util.Collections;
import java.util.List;

import static network.thunder.core.communication.layer.high.payments.LNPaymentLogic.SIDE.CLIENT;
import static network.thunder.core.communication.layer.high.payments.LNPaymentLogic.SIDE.SERVER;

public class LNPaymentLogicImpl implements LNPaymentLogic {

    public static final int SIGNATURE_SIZE = 146;

    @Override
    public Transaction getChannelTransaction (TransactionOutPoint anchor, Channel channel) {
        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(anchor.getHash(), anchor.getIndex(), Tools.getDummyScript());

        transaction.addOutput(Coin.valueOf(0),
                ScriptTools.scriptToP2SH(
                        ScriptTools.getChannelTxOutputRevocation(
                                channel.revoHashServerNext,
                                channel.keyServer,
                                channel.keyClient,
                                channel.csvDelay)));

        transaction.addOutput(Coin.valueOf(0), channel.addressClient);
        transaction = addPayments(transaction, channel);
        transaction.addOutput(Coin.ZERO, ScriptTools.getVersionReturnScript(channel.revoHashServerNext.index));

        //Missing two signatures, max 146B
        long fee = (long) Math.ceil((transaction.getMessageSize() + SIGNATURE_SIZE) * channel.feePerByte / 2);
        transaction.getOutput(0).setValue(Coin.valueOf(channel.amountServer - fee));
        transaction.getOutput(1).setValue(Coin.valueOf(channel.amountClient - fee));

        return transaction;
    }

    private static Transaction addPayments
            (Transaction transaction, Channel channelStatus) {
        Iterable<PaymentData> allPayments = new ArrayList<>(channelStatus.paymentList);

        for (PaymentData payment : allPayments) {
            Coin value = Coin.valueOf(payment.amount);
            Script script = ScriptTools.getChannelTxOutputPayment(channelStatus, payment);
            transaction.addOutput(value, ScriptTools.scriptToP2SH(script));
        }
        return transaction;
    }

    @Override
    public ChannelSignatures getSignatureObject (Channel channel, ECKey keyToSign, Transaction channelTransaction, List<Transaction> paymentTransactions) {
        ChannelSignatures channelSignatures = new ChannelSignatures();
        channelSignatures.channelSignatures = Tools.getChannelSignatures(channel, keyToSign, channelTransaction);
        List<TransactionSignature> signatureList = new ArrayList<>();

        int index = 0;
        for (Transaction t : paymentTransactions) {
            PaymentData paymentData = channel.paymentList.get(index).cloneObject();
            Script script = ScriptTools.getChannelTxOutputPayment(channel, paymentData);

            TransactionSignature sig = Tools.getSignature(t, 0, script.getProgram(), keyToSign);

            signatureList.add(sig);
            index++;
        }

        channelSignatures.paymentSignatures = signatureList;
        return channelSignatures;
    }

    @Override
    public List<Transaction> getPaymentTransactions (Sha256Hash parentTransactionHash, Channel channel) {

        List<PaymentData> allPayments = new ArrayList<>(channel.paymentList);
        List<Transaction> transactions = new ArrayList<>(allPayments.size());

        int index = 2;

        for (PaymentData payment : allPayments) {

            Transaction transaction = new Transaction(Constants.getNetwork());
            transaction.addInput(parentTransactionHash, index, Tools.getDummyScript());

            Coin value = Coin.valueOf(payment.amount);
            Script script = ScriptTools.getPaymentTxOutput(channel.keyServer, channel.keyClient, channel.revoHashServerNext, channel.csvDelay);
            transaction.addOutput(value, ScriptTools.scriptToP2SH(script));
            if (payment.sending) {
                Tools.setTransactionLockTime(transaction, payment.timestampRefund);
            }

            transactions.add(transaction);
            index++;
        }

        return transactions;
    }

    @Override
    public void checkUpdate (LNConfiguration configuration, Channel channel, ChannelUpdate channelUpdate) {
        //We can have a lot of operations here, like adding/removing payments. We need to verify if they are correct.
        Channel oldStatus = channel;
        Channel newStatus = oldStatus.copy();
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
            (Channel channel, ECKey keyToVerify, ChannelSignatures channelSignatures, Transaction channelTransaction, List<Transaction> paymentTransactions) {

        Sha256Hash hash1 = channelTransaction.hashForSignature(
                0,
                ScriptTools.getAnchorOutputScript(channel.keyClient, channel.keyServer),
                Transaction.SigHash.ALL,
                false);

        //We only have one anchor for now..
        if (!keyToVerify.verify(hash1, channelSignatures.channelSignatures.get(0))) {
            throw new LNPaymentException("Anchor signature is not correct..");
        }

        List<PaymentData> allPayments = new ArrayList<>(channel.paymentList);

        if (allPayments.size() != channelSignatures.paymentSignatures.size()) {
            throw new LNPaymentException("Size of payment signature list is incorrect");
        }

        for (int i = 0; i < allPayments.size(); ++i) {
            Transaction transaction = paymentTransactions.get(i);
            PaymentData paymentData = allPayments.get(i);

            TransactionSignature signature = channelSignatures.paymentSignatures.get(i);
            Script scriptPubKey = ScriptTools.getChannelTxOutputPayment(channel, paymentData);

            Sha256Hash hash = transaction.hashForSignature(0, scriptPubKey, Transaction.SigHash.ALL, false);

            if (!keyToVerify.verify(hash, signature)) {
                throw new LNPaymentException("Payment Signature " + i + "  is not correct..");
            }
        }
    }

    @Override
    public Transaction getSignedChannelTransaction (Channel channel) {
        Transaction transaction = this.getChannelTransaction(channel, SERVER);

        ChannelSignatures theirSignatures = channel.channelSignatures;
        ChannelSignatures ourSignatures = this.getSignatureObject(channel, channel.keyServer, transaction, Collections.emptyList());

        Script inputScript = ScriptTools.getCommitInputScript(
                theirSignatures.channelSignatures.get(0).encodeToBitcoin(),
                ourSignatures.channelSignatures.get(0).encodeToBitcoin(),
                channel.keyClient,
                channel.keyServer);

        transaction.getInput(0).setScriptSig(inputScript);

        return transaction;
    }

    @Override
    public List<Transaction> getSignedPaymentTransactions (Channel channel) {
        return null;
    }

    @Override
    public Transaction getChannelTransaction (Channel channel, SIDE side) {
        if (side == SERVER) {
            return this.getChannelTransaction(
                    new TransactionOutPoint(Constants.getNetwork(), 0, channel.anchorTxHash),
                    channel);
        } else {
            return this.getChannelTransaction(
                    new TransactionOutPoint(Constants.getNetwork(), 0, channel.anchorTxHash),
                    channel.reverse());
        }
    }

    @Override
    public ChannelSignatures getSignatureObject (Channel channel) {
        Transaction channelTx = getChannelTransaction(channel, CLIENT);
        List<Transaction> paymentTx = getPaymentTransactions(channel, CLIENT);
        return getSignatureObject(channel.reverse(), channel.keyServer, channelTx, paymentTx);
    }

    @Override
    public List<Transaction> getPaymentTransactions (Channel channel, SIDE side) {
        Transaction channelTx = getChannelTransaction(channel, side);
        if (side == SERVER) {
            return this.getPaymentTransactions(
                    channelTx.getHash(),
                    channel);
        } else {
            return this.getPaymentTransactions(
                    channelTx.getHash(),
                    channel.reverse());
        }
    }

    @Override
    public void checkSignatures (Channel channel, ChannelSignatures channelSignatures, SIDE side) {
        Transaction channelTx = getChannelTransaction(channel, side);
        List<Transaction> paymentTx = getPaymentTransactions(channel, side);

        if (side == SERVER) {
            this.checkSignatures(
                    channel, channel.keyClient,
                    channelSignatures,
                    channelTx,
                    paymentTx);
        } else {
            this.checkSignatures(
                    channel.reverse(), channel.keyServer,
                    channelSignatures,
                    channelTx,
                    paymentTx);
        }
    }

    private static void checkPaymentsInNewStatus (Channel oldStatus, ChannelUpdate channelUpdate) {

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
            //We reversed the Channel, so if we are receiving, he is sending
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
