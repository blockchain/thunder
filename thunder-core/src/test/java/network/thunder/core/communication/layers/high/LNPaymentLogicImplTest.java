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
                channel1);

        assertEquals(3, channelTransaction.getOutputs().size());
        assertEquals(1, channelTransaction.getInputs().size());
        assertEquals(channel1.anchorTxHash, channelTransaction.getInput(0).getOutpoint().getHash());
        assertEquals(1, channelTransaction.getInput(0).getOutpoint().getIndex());

        long expectedFee = channel1.feePerByte * (channelTransaction.getMessageSize() + 146) / 2;

        assertEquals(channel1.amountClient - channelTransaction.getOutput(1).getValue().value, expectedFee);
        assertEquals(channel1.amountServer - channelTransaction.getOutput(0).getValue().value, expectedFee);

        assertEquals(
                channelTransaction.getOutput(0).getScriptPubKey(),
                ScriptTools.scriptToP2SH(
                        ScriptTools.getChannelTxOutputRevocation(
                                channel1.revoHashServerNext,
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.csvDelay)));

        assertEquals(
                channelTransaction.getOutput(0).getScriptPubKey(),
                ScriptTools.scriptToP2SH(
                        ScriptTools.getChannelTxOutputRevocation(
                                channel1.revoHashServerNext,
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.csvDelay)));

        assertEquals(
                channelTransaction.getOutput(1).getAddressFromP2PKHScript(Constants.getNetwork()),
                channel1.addressClient);
    }

    @Test
    public void produceCorrectChannelTransactionWithPayments () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1);

        assertEquals(5, channelTransaction.getOutputs().size());

        long expectedFee = channel1.feePerByte * (channelTransaction.getMessageSize() + 146) / 2;

        assertEquals(channel1.amountClient - channelTransaction.getOutput(1).getValue().value, expectedFee);
        assertEquals(channel1.amountServer - channelTransaction.getOutput(0).getValue().value, expectedFee);

        assertEquals(
                channelTransaction.getOutput(2).getScriptPubKey(),
                ScriptTools.scriptToP2SH(
                        ScriptTools.getChannelTxOutputPayment(
                                channel1,
                                sending)));

        assertEquals(
                channelTransaction.getOutput(3).getScriptPubKey(),
                ScriptTools.scriptToP2SH(
                        ScriptTools.getChannelTxOutputPayment(
                                channel1,
                                receiving)));
    }

    @Test
    public void produceCorrectPaymentTransactions () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1);

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
                ScriptTools.scriptToP2SH(
                        ScriptTools.getPaymentTxOutput(
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.revoHashServerNext,
                                channel1.csvDelay)));

        assertEquals(
                paymentTransactions.get(1).getOutput(0).getScriptPubKey(),
                ScriptTools.scriptToP2SH(
                        ScriptTools.getPaymentTxOutput(
                                channel1.keyServer,
                                channel1.keyClient,
                                channel1.revoHashServerNext,
                                channel1.csvDelay)));

    }

    @Test
    public void produceCorrectSignatureObject () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channel1.keyServer, channelTransaction, paymentTransactions);

        assertEquals(1, channelSignatures.channelSignatures.size());
        assertEquals(2, channelSignatures.paymentSignatures.size());

        Sha256Hash channelTxHashForSignature = channelTransaction.hashForSignature(
                0,
                ScriptTools.getAnchorOutputScript(channel1.keyClient, channel1.keyServer),
                Transaction.SigHash.ALL,
                false);

        Sha256Hash paymentTxHashForSignature1 = paymentTransactions.get(0).hashForSignature(
                0,
                ScriptTools.getChannelTxOutputPayment(channel1, sending),
                Transaction.SigHash.ALL,
                false);

        Sha256Hash paymentTxHashForSignature2 = paymentTransactions.get(1).hashForSignature(
                0,
                ScriptTools.getChannelTxOutputPayment(channel1, receiving),
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
                channel1);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channel1.keyServer, channelTransaction, paymentTransactions);

        paymentLogic.checkSignatures(
                channel1, channel1.keyServer,
                channelSignatures,
                channelTransaction,
                paymentTransactions);
    }

    @Test(expected = LNPaymentException.class)
    public void failIncorrectSignature1 () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channel1.keyServer, channelTransaction, paymentTransactions);

        TransactionSignature invalidSig = TestTools.corruptSignature(channelSignatures.channelSignatures.get(0));
        channelSignatures.channelSignatures.set(0, invalidSig);

        paymentLogic.checkSignatures(
                channel1, channel1.keyClient,
                channelSignatures,
                channelTransaction,
                paymentTransactions);
    }

    @Test(expected = LNPaymentException.class)
    public void failIncorrectSignature2 () {
        addPaymentsToChannel();

        Transaction channelTransaction = paymentLogic.getChannelTransaction(
                new TransactionOutPoint(Constants.getNetwork(), 1, channel1.anchorTxHash),
                channel1);

        List<Transaction> paymentTransactions = paymentLogic.getPaymentTransactions(
                channelTransaction.getHash(),
                channel1);

        ChannelSignatures channelSignatures = paymentLogic.getSignatureObject(channel1, channel1.keyServer, channelTransaction, paymentTransactions);

        TransactionSignature invalidSig = TestTools.corruptSignature(channelSignatures.paymentSignatures.get(0));
        channelSignatures.paymentSignatures.set(0, invalidSig);

        paymentLogic.checkSignatures(
                channel1, channel1.keyClient,
                channelSignatures,
                channelTransaction,
                paymentTransactions);
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
        channel1.paymentList.add(sending);
        channel1.paymentList.add(receiving);
        channel1.amountServer -= sending.amount;
        channel1.amountClient -= receiving.amount;
    }
}