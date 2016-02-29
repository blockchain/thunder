package network.thunder.core.communication.processor.implementations.lnpayment;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.ScriptTools;
import network.thunder.core.etc.Tools;
import network.thunder.core.lightning.RevocationHash;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matsjerratsch on 11/01/2016.
 */
public class LNPaymentLogicImpl implements LNPaymentLogic {

    DBHandler dbHandler;

    Channel channel;
    ChannelStatus statusTemp;
    TransactionSignature signature1;
    TransactionSignature signature2;

    RevocationHash revocationHashClient;
    RevocationHash revocationHashServer;

    public LNPaymentLogicImpl (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    @Override
    public void initialise (Channel channel) {
        this.channel = channel;
    }

    @Override
    public Transaction getClientTransaction () {
        Preconditions.checkNotNull(channel);

        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(channel.anchorTxHashClient, 0, Tools.getDummyScript());
        transaction.addInput(channel.anchorTxHashServer, 0, Tools.getDummyScript());
        transaction.addOutput(Coin.valueOf(0), ScriptTools.getChannelTxOutputRevocation(revocationHashClient,
                channel.keyClient, channel.keyServer, Constants.ESCAPE_REVOCATION_TIME));
        transaction.addOutput(Coin.valueOf(0), ScriptTools.getChannelTxOutputPlain(channel.keyServer));

        return addPayments(transaction, statusTemp.getCloneReversed(), revocationHashClient,
                channel.keyClient, channel.keyServer);
    }

    @Override
    public Transaction getServerTransaction () {
        Preconditions.checkNotNull(channel);

        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addInput(channel.anchorTxHashServer, 0, Tools.getDummyScript());
        transaction.addInput(channel.anchorTxHashClient, 0, Tools.getDummyScript());
        transaction.addOutput(Coin.valueOf(0), ScriptTools.getChannelTxOutputRevocation(revocationHashServer,
                channel.keyServer, channel.keyClient, Constants.ESCAPE_REVOCATION_TIME));
        transaction.addOutput(Coin.valueOf(0), ScriptTools.getChannelTxOutputPlain(channel.keyClient));

        return addPayments(transaction, statusTemp, revocationHashServer, channel.keyServer, channel.keyClient);
    }

    private Transaction addPayments (Transaction transaction, ChannelStatus channelStatus, RevocationHash revocationHash,
                                     ECKey keyServer, ECKey keyClient) {
        List<PaymentData> allPayments = new ArrayList<>(channelStatus.remainingPayments);
        allPayments.addAll(channelStatus.newPayments);

        for (PaymentData payment : allPayments) {
            Coin value = Coin.valueOf(payment.amount);
            Script script;
            if (payment.sending) {
                script = ScriptTools.getChannelTxOutputSending(revocationHash, payment, keyServer, keyClient);
            } else {
                script = ScriptTools.getChannelTxOutputReceiving(revocationHash, payment, keyServer, keyClient);
            }
            transaction.addOutput(value, script);
        }
        return transaction;
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
    public void readMessageOutbound (LNPayment message) {
        Preconditions.checkNotNull(channel);

        if (message instanceof LNPaymentAMessage) {
            readOutgoingAMessage((LNPaymentAMessage) message);
        } else if (message instanceof LNPaymentBMessage) {
            readOutgoingBMessage((LNPaymentBMessage) message);
        } else if (message instanceof LNPaymentCMessage) {
            readOutgoingCMessage((LNPaymentCMessage) message);
        } else if (message instanceof LNPaymentDMessage) {
            readOutgoingDMessage((LNPaymentDMessage) message);
        }
    }

    @Override
    public ChannelStatus getTemporaryChannelStatus () {
        Preconditions.checkNotNull(channel);

        return statusTemp.getClone();
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
        //TODO Check the signatures we got from the other party
        signature1 = TransactionSignature.decodeFromBitcoin(message.newCommitSignature1, true);
        signature2 = TransactionSignature.decodeFromBitcoin(message.newCommitSignature2, true);

        Transaction channelTransaction = getServerTransaction();

        Sha256Hash hash1 = channelTransaction.hashForSignature(0, channel.getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
        Sha256Hash hash2 = channelTransaction.hashForSignature(1, channel.getScriptAnchorOutputClient(), Transaction.SigHash.ALL, false);

        if (!channel.keyClient.verify(hash1, signature1)) {
            throw new LNPaymentException("Signature is not correct..");
        }

        if (!channel.keyClient.verify(hash2, signature2)) {
            throw new LNPaymentException("Signature is not correct..");
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

    private void readOutgoingAMessage (LNPaymentAMessage message) {
        this.statusTemp = message.channelStatus;
        this.revocationHashServer = message.newRevocation;
    }

    private void readOutgoingBMessage (LNPaymentBMessage message) {
        this.revocationHashServer = message.newRevocation;
    }

    private void readOutgoingCMessage (LNPaymentCMessage message) {

    }

    private void readOutgoingDMessage (LNPaymentDMessage message) {

    }
}
