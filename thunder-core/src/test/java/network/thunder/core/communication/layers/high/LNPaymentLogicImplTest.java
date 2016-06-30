package network.thunder.core.communication.layers.high;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogicImpl;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.TestTools;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.ScriptTools;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.ScriptBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LNPaymentLogicImplTest {

    Channel channel1;
    Channel channel2;

    ClientObject node1;
    ClientObject node2;

    LNPaymentLogic paymentLogic;

    LNConfiguration configuration = new LNConfiguration();

    PaymentData sending = getPaymentData(true);
    PaymentData receiving = getPaymentData(false);

    @Before
    public void prepare () {
        Context.getOrCreate(Constants.getNetwork());

        node1 = new ClientObject();
        node2 = new ClientObject();

        node1.isServer = false;
        node2.isServer = true;

        node1.name = "LNPayment1";
        node2.name = "LNPayment2";

        channel1 = TestTools.getMockChannel(configuration);
        channel2 = TestTools.getMockChannel(configuration);

        channel1.retrieveDataFromOtherChannel(channel2);
        channel2.retrieveDataFromOtherChannel(channel1);

        paymentLogic = new LNPaymentLogicImpl();
    }

    @Test
    public void produceCorrectChannelTransactionNoPayments () {
        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1.channelStatus,
                channel1.keyClient,
                channel1.keyServer);

        assertEquals(2, channelTransaction.getOutputs().size());
        assertEquals(1, channelTransaction.getInputs().size());
        assertEquals(channel1.anchorTxHash, channelTransaction.getInput(0).getOutpoint().getHash());
        assertEquals(1, channelTransaction.getInput(0).getOutpoint().getIndex());

        long expectedFee = channel1.channelStatus.feePerByte * (channelTransaction.getMessageSize() + 146) / 2;

        assertEquals(channel1.channelStatus.amountClient - channelTransaction.getOutput(0).getValue().value, expectedFee);
        assertEquals(channel1.channelStatus.amountServer - channelTransaction.getOutput(1).getValue().value, expectedFee);

        assertEquals(
                channelTransaction.getOutput(0).getScriptPubKey(),
                ScriptBuilder.createP2SHOutputScript(
                        ScriptTools.getChannelTxOutputRevocation(
                                channel1.channelStatus.revoHashServerNext,
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.channelStatus.csvDelay)));

        assertEquals(
                channelTransaction.getOutput(0).getScriptPubKey(),
                ScriptBuilder.createP2SHOutputScript(
                        ScriptTools.getChannelTxOutputRevocation(
                                channel1.channelStatus.revoHashServerNext,
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.channelStatus.csvDelay)));

        assertEquals(
                channelTransaction.getOutput(1).getAddressFromP2PKHScript(Constants.getNetwork()),
                channel1.channelStatus.addressClient);
    }

    @Test
    public void produceCorrectChannelTransactionWithPayments () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1.channelStatus,
                channel1.keyClient,
                channel1.keyServer);

        assertEquals(4, channelTransaction.getOutputs().size());

        long expectedFee = channel1.channelStatus.feePerByte * (channelTransaction.getMessageSize() + 146) / 2;

        assertEquals(channel1.channelStatus.amountClient - channelTransaction.getOutput(0).getValue().value, expectedFee);
        assertEquals(channel1.channelStatus.amountServer - channelTransaction.getOutput(1).getValue().value, expectedFee);

        assertEquals(
                channelTransaction.getOutput(2).getScriptPubKey(),
                ScriptBuilder.createP2SHOutputScript(
                        ScriptTools.getChannelTxOutputPaymentSending(
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.channelStatus.revoHashServerNext,
                                sending.secret,
                                sending.timestampRefund)));

        assertEquals(
                channelTransaction.getOutput(3).getScriptPubKey(),
                ScriptBuilder.createP2SHOutputScript(
                        ScriptTools.getChannelTxOutputPaymentReceiving(
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.channelStatus.revoHashServerNext,
                                receiving.secret,
                                receiving.timestampRefund)));
    }

    @Test
    public void produceCorrectPaymentTransactions () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1.channelStatus,
                channel1.keyClient,
                channel1.keyServer);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1.channelStatus,
                channel1.keyServer,
                channel1.keyClient);

        assertEquals(2, paymentTransactions.size());

        assertEquals(channelTransaction.getHash(), paymentTransactions.get(0).getInput(0).getOutpoint().getHash());
        assertEquals(channelTransaction.getHash(), paymentTransactions.get(1).getInput(0).getOutpoint().getHash());

        assertEquals(2, paymentTransactions.get(0).getInput(0).getOutpoint().getIndex());
        assertEquals(3, paymentTransactions.get(1).getInput(0).getOutpoint().getIndex());

        assertEquals(1, paymentTransactions.get(0).getInputs().size());
        assertEquals(1, paymentTransactions.get(1).getInputs().size());

        assertEquals(1, paymentTransactions.get(0).getOutputs().size());
        assertEquals(1, paymentTransactions.get(1).getOutputs().size());

        assertEquals(sending.amount, paymentTransactions.get(0).getOutput(0).getValue().value);
        assertEquals(receiving.amount, paymentTransactions.get(1).getOutput(0).getValue().value);

        assertEquals(
                paymentTransactions.get(0).getOutput(0).getScriptPubKey(),
                ScriptTools.getPaymentTxOutput(
                        channel1.keyServer,
                        channel1.keyClient,
                        channel1.channelStatus.revoHashServerNext,
                        channel1.channelStatus.csvDelay));

        assertEquals(
                paymentTransactions.get(1).getOutput(0).getScriptPubKey(),
                ScriptTools.getPaymentTxOutput(
                        channel1.keyServer,
                        channel1.keyClient,
                        channel1.channelStatus.revoHashServerNext,
                        channel1.channelStatus.csvDelay));

    }

    @Test
    public void produceCorrectSignatureObject () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1.channelStatus,
                channel1.keyClient,
                channel1.keyServer);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1.channelStatus,
                channel1.keyServer,
                channel1.keyClient);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channelTransaction, paymentTransactions);

        assertEquals(1, channelSignatures.channelSignatures.size());
        assertEquals(2, channelSignatures.paymentSignatures.size());

        Sha256Hash channelTxHashForSignature = channelTransaction.hashForSignature(
                0,
                ScriptTools.getAnchorOutputScript(channel1.keyClient, channel1.keyServer),
                Transaction.SigHash.ALL,
                false);

        Sha256Hash paymentTxHashForSignature1 = paymentTransactions.get(0).hashForSignature(
                0,
                channelTransaction.getOutput(2).getScriptBytes(),
                Transaction.SigHash.ALL,
                false);

        Sha256Hash paymentTxHashForSignature2 = paymentTransactions.get(1).hashForSignature(
                0,
                channelTransaction.getOutput(3).getScriptBytes(),
                Transaction.SigHash.ALL,
                false);

        assertTrue(channel1.keyServer.verify(channelTxHashForSignature, channelSignatures.channelSignatures.get(0)));
        assertTrue(channel1.keyServer.verify(paymentTxHashForSignature1, channelSignatures.paymentSignatures.get(0)));
        assertTrue(channel1.keyServer.verify(paymentTxHashForSignature2, channelSignatures.paymentSignatures.get(1)));
    }

    @Test
    public void verifyCorrectSignatureObject () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1.channelStatus,
                channel1.keyClient,
                channel1.keyServer);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1.channelStatus,
                channel1.keyServer,
                channel1.keyClient);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channelTransaction, paymentTransactions);

        paymentLogic.checkSignatures(
                channel1.keyClient,
                channel1.keyServer,
                channelSignatures,
                channelTransaction,
                paymentTransactions,
                channel1.channelStatus);
    }

    @Test(expected = LNPaymentException.class)
    public void failIncorrectSignature1 () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1.channelStatus,
                channel1.keyClient,
                channel1.keyServer);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1.channelStatus,
                channel1.keyServer,
                channel1.keyClient);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channelTransaction, paymentTransactions);

        TransactionSignature invalidSig = TestTools.corruptSignature(channelSignatures.channelSignatures.get(0));
        channelSignatures.channelSignatures.set(0, invalidSig);

        paymentLogic.checkSignatures(
                channel1.keyClient,
                channel1.keyServer,
                channelSignatures,
                channelTransaction,
                paymentTransactions,
                channel1.channelStatus);
    }

    @Test(expected = LNPaymentException.class)
    public void failIncorrectSignature2 () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1.channelStatus,
                channel1.keyClient,
                channel1.keyServer);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1.channelStatus,
                channel1.keyServer,
                channel1.keyClient);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channelTransaction, paymentTransactions);

        TransactionSignature invalidSig = TestTools.corruptSignature(channelSignatures.paymentSignatures.get(0));
        channelSignatures.paymentSignatures.set(0, invalidSig);

        paymentLogic.checkSignatures(
                channel1.keyClient,
                channel1.keyServer,
                channelSignatures,
                channelTransaction,
                paymentTransactions,
                channel1.channelStatus);
    }

    private static PaymentData getPaymentData (boolean sending) {
        PaymentData payment = new PaymentData();
        payment.secret = new PaymentSecret(Tools.getRandomByte(20));
        payment.sending = sending;
        payment.timestampOpen = Tools.currentTime();
        payment.amount = Tools.getRandom(900, 1100);
        payment.timestampRefund = Tools.currentTime() + 60 * 60;
        return payment;
    }

    private void addPaymentsToChannel () {
        channel1.channelStatus.paymentList.add(sending);
        channel1.channelStatus.paymentList.add(receiving);
        channel1.channelStatus.amountServer -= sending.amount;
        channel1.channelStatus.amountClient -= receiving.amount;
    }
}