package network.thunder.core.communication.layer.high.payments;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.payments.messages.*;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.messages.LNPayment;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.database.DBHandler;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.etc.Constants;
import network.thunder.core.helper.ScriptTools;
import network.thunder.core.etc.Tools;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.LNConfiguration;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.List;

public class LNPaymentLogicImpl implements LNPaymentLogic {

    DBHandler dbHandler;

    Channel channel;
    ChannelStatus statusTemp;

    TransactionSignature signature1;
    TransactionSignature signature2;

    List<TransactionSignature> paymentSignatures = new ArrayList<>();

    RevocationHash revocationHashClient;
    RevocationHash revocationHashServer;

    LNConfiguration configuration = new LNConfiguration();

    LNPaymentMessageFactory messageFactory;

    public LNPaymentLogicImpl (LNPaymentMessageFactory messageFactory, DBHandler dbHandler) {
        this.messageFactory = messageFactory;
        this.dbHandler = dbHandler;
    }

    @Override
    public void initialise (Channel channel) {
        this.channel = channel;
    }

    public Transaction getClientTransaction () {
        Preconditions.checkNotNull(channel);

        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(channel.anchorTxHashClient, 0, Tools.getDummyScript());
        transaction.addInput(channel.anchorTxHashServer, 0, Tools.getDummyScript());
        transaction.addOutput(Coin.valueOf(0), ScriptTools.getChannelTxOutputRevocation(revocationHashClient,
                channel.keyClient, channel.keyServer, Constants.ESCAPE_REVOCATION_TIME));
        transaction.addOutput(Coin.valueOf(statusTemp.amountServer), ScriptTools.getChannelTxOutputPlain(channel.keyServer));
        transaction = addPayments(transaction, statusTemp.getCloneReversed(), revocationHashClient, channel.keyClient, channel.keyServer);

        long amountEncumbered = statusTemp.amountClient - transaction.getMessageSize() * statusTemp.feePerByte;
        transaction.getOutput(0).setValue(Coin.valueOf(amountEncumbered));

        return transaction;
    }

    public Transaction getServerTransaction () {
        Preconditions.checkNotNull(channel);

        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(channel.anchorTxHashServer, 0, Tools.getDummyScript());
        transaction.addInput(channel.anchorTxHashClient, 0, Tools.getDummyScript());
        transaction.addOutput(Coin.valueOf(0), ScriptTools.getChannelTxOutputRevocation(revocationHashServer,
                channel.keyServer, channel.keyClient, Constants.ESCAPE_REVOCATION_TIME));
        transaction.addOutput(Coin.valueOf(statusTemp.amountClient), ScriptTools.getChannelTxOutputPlain(channel.keyClient));
        transaction = addPayments(transaction, statusTemp, revocationHashServer, channel.keyServer, channel.keyClient);

        long amountEncumbered = statusTemp.amountServer - transaction.getMessageSize() * statusTemp.feePerByte;
        transaction.getOutput(0).setValue(Coin.valueOf(amountEncumbered));

        return transaction;
    }

    public List<Transaction> getClientPaymentTransactions () {
        Transaction t = getClientTransaction();
        return getPaymentTransactions(t.getHash(), statusTemp.getCloneReversed(), revocationHashClient, channel.keyClient, channel.keyServer);
    }

    public List<Transaction> getServerPaymentTransactions () {
        Transaction t = getServerTransaction();
        return getPaymentTransactions(t.getHash(), statusTemp, revocationHashServer, channel.keyServer, channel.keyClient);
    }

    public List<TransactionSignature> getChannelSignatures () {
        //The channelTransaction is finished, we just need to produce the signatures..
        TransactionSignature signature1 = Tools.getSignature(getClientTransaction(), 0, channel.getScriptAnchorOutputClient().getProgram(), channel
                .getKeyServer());
        TransactionSignature signature2 = Tools.getSignature(getClientTransaction(), 1, channel.getScriptAnchorOutputServer().getProgram(), channel
                .getKeyServer());

        List<TransactionSignature> channelSignatures = new ArrayList<>();

        channelSignatures.add(signature1);
        channelSignatures.add(signature2);
        return channelSignatures;
    }

    public List<TransactionSignature> getPaymentSignatures () {
        List<Transaction> paymentTransactions = getClientPaymentTransactions();
        List<TransactionSignature> signatureList = new ArrayList<>();

        int index = 2;
        for (Transaction t : paymentTransactions) {
            TransactionSignature sig = Tools.getSignature(t, 0, getClientTransaction().getOutput(index).getScriptBytes(), channel.getKeyServer());
            signatureList.add(sig);
            index++;
        }
        return signatureList;
    }

    private Transaction addPayments (Transaction transaction, ChannelStatus channelStatus, RevocationHash revocationHash, ECKey keyServer, ECKey keyClient) {
        List<PaymentData> allPayments = new ArrayList<>(channelStatus.remainingPayments);
        allPayments.addAll(channelStatus.newPayments);

        for (PaymentData payment : allPayments) {
            //TODO value here should include fees for the next transaction you need to spend it..pa
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

    private List<Transaction> getPaymentTransactions (Sha256Hash parentTransactionHash, ChannelStatus channelStatus, RevocationHash
            revocationHash, ECKey keyServer, ECKey keyClient) {

        List<PaymentData> allPayments = new ArrayList<>(channelStatus.remainingPayments);
        allPayments.addAll(channelStatus.newPayments);

        List<Transaction> transactions = new ArrayList<>(allPayments.size());

        int index = 2;

        for (PaymentData payment : allPayments) {

            Transaction transaction = new Transaction(Constants.getNetwork());
            transaction.addInput(parentTransactionHash, index, Tools.getDummyScript());

            Coin value = Coin.valueOf(payment.amount);
            Script script = ScriptTools.getPaymentTxOutput(keyServer, keyClient, revocationHash, payment.csvDelay);
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
        channel.channelSignature1 = this.signature1;
        channel.channelSignature2 = this.signature2;
        channel.paymentSignatures = this.paymentSignatures;
        return channel;
    }

    @Override
    public ChannelStatus getTemporaryChannelStatus () {
        Preconditions.checkNotNull(channel);

        return statusTemp.getClone();
    }

    @Override
    public LNPaymentAMessage getAMessage (ChannelStatus newStatus) {
        this.statusTemp = newStatus;
        LNPaymentAMessage message = messageFactory.getMessageA(channel, statusTemp);
        this.revocationHashServer = message.newRevocation;
        return message;
    }

    @Override
    public LNPaymentBMessage getBMessage () {
        LNPaymentBMessage message = messageFactory.getMessageB(channel);
        this.revocationHashServer = message.newRevocation;
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
        long amountServer = channel.channelStatus.amountServer;
        long amountClient = channel.channelStatus.amountClient;

        revocationHashClient = null;
        revocationHashServer = null;

        statusTemp = message.channelStatus.getCloneReversed();

        System.out.println("Check received new status: " + statusTemp);
        System.out.println("Old status: " + channel.channelStatus);

        checkPaymentsInNewStatus(channel.channelStatus, statusTemp);
        checkRefundedPayments(statusTemp);
        checkRedeemedPayments(statusTemp);

        for (PaymentData refund : statusTemp.refundedPayments) {
            amountServer += refund.amount;
        }

        for (PaymentData redeem : statusTemp.redeemedPayments) {
            amountClient += redeem.amount;
        }

        for (PaymentData payment : statusTemp.newPayments) {
            amountClient -= payment.amount;
        }

        if (amountClient != statusTemp.amountClient || amountClient < 0) {
            throw new LNPaymentException("amountClient not correct.. Is " + statusTemp.amountClient + " Should be: " + amountClient);
        }

        if (amountServer != statusTemp.amountServer || amountServer < 0) {
            throw new LNPaymentException("amountServer not correct..Is " + statusTemp.amountServer + " Should be: " + amountServer);
        }

        //Sufficient to test new payments
        for (PaymentData payment : statusTemp.newPayments) {
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
        if (statusTemp.csvDelay < configuration.MIN_REVOCATION_DELAY || statusTemp.csvDelay > configuration.MAX_REVOCATION_DELAY) {
            throw new LNPaymentException("Change-Revocation delay not within allowed boundaries. Is: " + statusTemp.csvDelay);
        }
        if (statusTemp.feePerByte > configuration.MAX_FEE_PER_BYTE || statusTemp.feePerByte < configuration.MIN_FEE_PER_BYTE) {
            throw new LNPaymentException("feePerByte not within allowed boundaries. Is: " + statusTemp.feePerByte);
        }

        dbHandler.insertRevocationHash(message.newRevocation);
        revocationHashClient = message.newRevocation;
    }

    private void parseBMessage (LNPaymentBMessage message) {
        if (!message.success) {
            throw new LNPaymentException(message.error);
        }
        revocationHashClient = message.newRevocation;
        dbHandler.insertRevocationHash(message.newRevocation);
    }

    private void parseCMessage (LNPaymentCMessage message) {
        paymentSignatures.clear();
        signature1 = TransactionSignature.decodeFromBitcoin(message.newCommitSignature1, true);
        signature2 = TransactionSignature.decodeFromBitcoin(message.newCommitSignature2, true);

        Transaction channelTransaction = getServerTransaction();

        Sha256Hash hash1 = channelTransaction.hashForSignature(0, channel.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
        Sha256Hash hash2 = channelTransaction.hashForSignature(1, channel.getScriptAnchorOutputClient(), Transaction.SigHash.ALL, false);

        if (!channel.keyClient.verify(hash1, signature1)) {
            throw new LNPaymentException("Signature1 is not correct..");
        }

        if (!channel.keyClient.verify(hash2, signature2)) {
            throw new LNPaymentException("Signature2 is not correct..");
        }

        List<PaymentData> allPayments = new ArrayList<>(statusTemp.remainingPayments);
        allPayments.addAll(statusTemp.newPayments);
        List<Transaction> paymentTransactions = getServerPaymentTransactions();

        if (allPayments.size() != message.newPaymentSignatures.size()) {
            throw new LNPaymentException("Size of payment signature list is incorrect");
        }

        for (int i = 0; i < allPayments.size(); ++i) {
            Transaction transaction = paymentTransactions.get(i);
            PaymentData payment = allPayments.get(i);
            TransactionSignature signature = TransactionSignature.decodeFromBitcoin(message.newPaymentSignatures.get(i), true);
            Script scriptPubKey = channelTransaction.getOutput(i + 2).getScriptPubKey();

            Sha256Hash hash = transaction.hashForSignature(0, scriptPubKey, Transaction.SigHash.ALL, false);
            if (!channel.keyClient.verify(hash, signature)) {
                throw new LNPaymentException("Payment Signature " + i + "  is not correct..");
            }
            paymentSignatures.add(signature);
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

    private void checkPaymentsInNewStatus (ChannelStatus oldStatus, ChannelStatus newStatus) {

        oldStatus = oldStatus.getClone();
        newStatus = newStatus.getClone();

        for (PaymentData paymentData : oldStatus.remainingPayments) {
            //All of these should be in either refund/settled/old
            if (newStatus.remainingPayments.remove(paymentData)) {
                continue;
            }
            if (newStatus.redeemedPayments.remove(paymentData)) {
                continue;
            }
            if (newStatus.refundedPayments.remove(paymentData)) {
                continue;
            }
            throw new LNPaymentException("Old payment that is in neither refund/settle/old");
        }

        if (newStatus.remainingPayments.size() + newStatus.refundedPayments.size() + newStatus.redeemedPayments.size() > 0) {
            throw new LNPaymentException("New payments that are not in newPayments");
        }
    }

    private void checkRefundedPayments (ChannelStatus newStatus) {
        for (PaymentData paymentData : newStatus.refundedPayments) {
            //We reversed the ChannelStatus, so if we are receiving, he is sending
            if (!paymentData.sending) {
                throw new LNPaymentException("Trying to refund a sent payment");
            }
        }
    }

    private void checkRedeemedPayments (ChannelStatus newStatus) {
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
