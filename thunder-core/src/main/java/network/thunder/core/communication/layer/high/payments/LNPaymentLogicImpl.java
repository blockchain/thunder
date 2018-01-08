package network.thunder.core.communication.layer.high.payments;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.messages.*;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.ScriptTools;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.List;

public class LNPaymentLogicImpl implements LNPaymentLogic {

    DBHandler dbHandler;

    Channel channel;

    ChannelStatus oldStatus;
    ChannelStatus newStatus;
    ChannelUpdate channelUpdate;

    ChannelSignatures channelSignatures;

    LNConfiguration configuration = new LNConfiguration();

    LNPaymentMessageFactory messageFactory;

    public LNPaymentLogicImpl (LNPaymentMessageFactory messageFactory, DBHandler dbHandler) {
        this.messageFactory = messageFactory;
        this.dbHandler = dbHandler;
    }

    @Override
    public Transaction getChannelTransaction (TransactionOutPoint anchor, ChannelStatus channelStatus, ECKey client, ECKey server) {
        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(anchor.getHash(), anchor.getIndex(), Tools.getDummyScript());
        transaction.addOutput(Coin.valueOf(0), ScriptTools.getChannelTxOutputRevocation(channelStatus.revocationHashClient,
                client, server, Constants.ESCAPE_REVOCATION_TIME));
        transaction.addOutput(Coin.valueOf(0), channelStatus.addressServer);
        transaction = addPayments(transaction, channelStatus, channelStatus.revocationHashClient, client, server);


        //Missing two signatures, max 146B
        long fee = (long) Math.ceil((transaction.getMessageSize() + 146) * channelStatus.feePerByte / 2);
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
    public ChannelSignatures getSignatureObject (Channel channel, Transaction channelTransaction) {
        ChannelSignatures channelSignatures = new ChannelSignatures();

        channelSignatures.channelSignatures = Tools.getChannelSignatures(channel, channelTransaction);

        List<Transaction> paymentTransactions = getPaymentTransactions(channelTransaction.getHash(), channel.channelStatus, channel.keyServer, channel.keyClient);
        List<TransactionSignature> signatureList = new ArrayList<>();

        int index = 2;
        for (Transaction t : paymentTransactions) {
            TransactionSignature sig = Tools.getSignature(t, 0, channelTransaction.getOutput(index), channel.getKeyServer());
            signatureList.add(sig);
            index++;
        }

        channelSignatures.paymentSignatures = signatureList;


        return channelSignatures;
    }

    @Override
    public void initialise (Channel channel) {
        this.channel = channel;
    }

    public Transaction getClientTransaction () {
        Preconditions.checkNotNull(channel);
        return getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 0, channel.anchorTxHash),
                newStatus.getCloneReversed(),
                channel.keyClient,
                channel.keyServer);
    }

    public Transaction getServerTransaction () {
        Preconditions.checkNotNull(channel);
        return getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 0, channel.anchorTxHash),
                newStatus,
                channel.keyServer,
                channel.keyClient);
    }

    public List<Transaction> getClientPaymentTransactions () {
        Transaction t = getClientTransaction();
        return getPaymentTransactions(t.getHash(), newStatus.getCloneReversed(), channel.keyClient, channel.keyServer);
    }

    public List<Transaction> getServerPaymentTransactions () {
        Transaction t = getServerTransaction();
        return getPaymentTransactions(t.getHash(), newStatus, channel.keyServer, channel.keyClient);
    }

    public List<TransactionSignature> getChannelSignatures () {
        //The channelTransaction is finished, we just need to produce the signatures..
        return Tools.getChannelSignatures(channel, getClientTransaction());
    }

    public List<TransactionSignature> getPaymentSignatures () {
        List<Transaction> paymentTransactions = getClientPaymentTransactions();
        List<TransactionSignature> signatureList = new ArrayList<>();

        int index = 2;
        for (Transaction t : paymentTransactions) {
            TransactionSignature sig = Tools.getSignature(t, 0, getClientTransaction().getOutput(index), channel.getKeyServer());
            signatureList.add(sig);
            index++;
        }
        return signatureList;
    }

    private static List<Transaction> getPaymentTransactions (Sha256Hash parentTransactionHash, ChannelStatus channelStatus, ECKey keyServer, ECKey keyClient) {

        List<PaymentData> allPayments = new ArrayList<>(channelStatus.paymentList);
        List<Transaction> transactions = new ArrayList<>(allPayments.size());

        int index = 2;

        for (PaymentData payment : allPayments) {

            Transaction transaction = new Transaction(Constants.getNetwork());
            transaction.addInput(parentTransactionHash, index, Tools.getDummyScript());

            Coin value = Coin.valueOf(payment.amount);
            Script script = ScriptTools.getPaymentTxOutput(keyServer, keyClient, channelStatus.revocationHashServer, payment.csvDelay);
            transaction.addOutput(value, script);

            transactions.add(transaction);
            index++;
        }

        return transactions;
    }

    @Override
    public void checkMessageIncoming (LNPayment message) {
        Preconditions.checkNotNull(channel);

        if (message instanceof LNPaymentAMessage) {
            parseAMessage((LNPaymentAMessage) message);
        } else if (message instanceof LNPaymentBMessage) {
            parseBMessage((LNPaymentBMessage) message);
        } else if (message instanceof LNPaymentCMessage) {
            parseCMessage((LNPaymentCMessage) message);
        } else if (message instanceof LNPaymentDMessage) {
            parseDMessage((LNPaymentDMessage) message);
        }
    }

    @Override
    public Channel updateChannel (Channel channel) {
        channel.channelSignatures = channelSignatures;
        return channel;
    }

    @Override
    public ChannelUpdate getChannelUpdate () {
        Preconditions.checkNotNull(channel);
        return channelUpdate;
    }

    @Override
    public LNPaymentAMessage getAMessage (ChannelUpdate update) {
        this.channelUpdate = update;
        this.oldStatus = channel.channelStatus;
        this.newStatus = oldStatus.getClone();
        this.newStatus.applyUpdate(update);
        LNPaymentAMessage message = messageFactory.getMessageA(channel, update);
        this.newStatus.revocationHashServer = message.newRevocation;
        return message;
    }

    @Override
    public LNPaymentBMessage getBMessage () {
        LNPaymentBMessage message = messageFactory.getMessageB(channel);
        this.newStatus.revocationHashServer = message.newRevocation;
        return message;
    }

    @Override
    public LNPaymentCMessage getCMessage () {
        LNPaymentCMessage message = messageFactory.getMessageC(channel, getChannelSignatures(), getPaymentSignatures());
        return message;
    }

    @Override
    public LNPaymentDMessage getDMessage () {
        LNPaymentDMessage message = messageFactory.getMessageD(channel);
        return message;
    }

    private void parseAMessage (LNPaymentAMessage message) {
        //We can have a lot of operations here, like adding/removing payments. We need to verify if they are correct.
        channelUpdate = message.channelStatus.getCloneReversed();
        oldStatus = channel.channelStatus;
        newStatus = checkUpdate(configuration, channel, channelUpdate);

        dbHandler.insertRevocationHash(message.newRevocation);
        newStatus.revocationHashClient = message.newRevocation;
    }

    private static ChannelStatus checkUpdate (LNConfiguration configuration, Channel channel, ChannelUpdate channelUpdate) {
        //We can have a lot of operations here, like adding/removing payments. We need to verify if they are correct.
        ChannelStatus oldStatus = channel.channelStatus;
        ChannelStatus newStatus = oldStatus.getClone();
        newStatus.applyUpdate(channelUpdate);

        System.out.println("Old status: " + oldStatus);
        System.out.println("New status: " + newStatus);

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
            if (payment.csvDelay < configuration.MIN_REVOCATION_DELAY || payment.csvDelay > configuration.MAX_REVOCATION_DELAY) {
                throw new LNPaymentException("Payment-Revocation delay not within allowed boundaries. Is: " + payment.csvDelay);
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
            throw new LNPaymentException("Change-Revocation delay not within allowed boundaries. Is: " + newStatus.csvDelay);
        }
        if (newStatus.feePerByte > configuration.MAX_FEE_PER_BYTE || newStatus.feePerByte < configuration.MIN_FEE_PER_BYTE) {
            throw new LNPaymentException("feePerByte not within allowed boundaries. Is: " + newStatus.feePerByte);
        }
        return newStatus;
    }

    private void parseBMessage (LNPaymentBMessage message) {
        newStatus.revocationHashClient = message.newRevocation;
        dbHandler.insertRevocationHash(message.newRevocation);
    }

    private void parseCMessage (LNPaymentCMessage message) {
        checkSignatures(channel.keyServer, channel.keyClient, message.getChannelSignatures(), getServerTransaction(), newStatus);
        channelSignatures = message.getChannelSignatures();
    }

    @Override
    public void checkSignatures
            (ECKey keyServer, ECKey keyClient, ChannelSignatures channelSignatures, Transaction channelTransaction, ChannelStatus status) {

        Sha256Hash hash1 = channelTransaction.hashForSignature(0, ScriptTools.getAnchorOutputScript(keyClient, keyServer), Transaction.SigHash.ALL, false);
        //We only have one anchor for now..
        if (!keyClient.verify(hash1, channelSignatures.channelSignatures.get(0))) {
            throw new LNPaymentException("Anchor signature is not correct..");
        }

        List<PaymentData> allPayments = new ArrayList<>(status.paymentList);
        List<Transaction> paymentTransactions = getPaymentTransactions(channelTransaction.getHash(), status, keyServer, keyClient);

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

    private void parseDMessage (LNPaymentDMessage message) {
        for (RevocationHash hash : message.oldRevocationHashes) {
            if (!hash.check()) {
                throw new LNPaymentException("hash.check() returned false");
            }
        }
        if (!dbHandler.checkOldRevocationHashes(message.oldRevocationHashes)) {
            throw new LNPaymentException("Could not verify all old revocation hashes..");
        }
    }

    private void saveSignatures () {

    }

    private static void checkPaymentsInNewStatus (ChannelStatus oldStatus, ChannelUpdate channelUpdate) {

        oldStatus = oldStatus.getClone();
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
