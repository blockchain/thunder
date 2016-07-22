package network.thunder.core.helper;

import com.google.common.primitives.UnsignedBytes;
import com.google.gson.internal.LinkedTreeMap;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Utils;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScriptTools {

    final static Map<String, String> scriptMap = new LinkedTreeMap<>();

    static {
        scriptMap.put("HASH160", "A9");
        scriptMap.put("DUP", "76");
        scriptMap.put("EQUAL", "87");
        scriptMap.put("EQUALVERIFY", "88");
        scriptMap.put("SWAP", "7C");
        scriptMap.put("ADD", "93");
        scriptMap.put("NOTIF", "64");
        scriptMap.put("ELSE", "67");
        scriptMap.put("ENDIF", "68");
        scriptMap.put("IF", "63");
        scriptMap.put("CLTV", "B1");
        scriptMap.put("CSV", "B3");
        scriptMap.put("2DROP", "6D");
        scriptMap.put("DROP", "75");
        scriptMap.put("VERIFY", "69");
        scriptMap.put("CHECKSIGVERIFY", "AD");
        scriptMap.put("CHECKMULTISIGVERIFY", "AF");
        scriptMap.put("CHECKSIG", "AC");
        scriptMap.put("CHECKMULTISIG", "AE");
        scriptMap.put("OP_0", "00");
        scriptMap.put("OP_1", "51");
        scriptMap.put("OP_2", "52");
    }

    //Due to some policy, miners won't include OP_NOP until they are activated. Not even on testnet.
    public static final boolean CLTV_CSV_ENABLED = true;

    //Templates are like normal scripts but with 0xFF as placeholders for parameters

    /////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// SCRIPT TEMPLATES //////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////

    //Anchor is just a 2-of-2 multisig
    public static final byte[] ANCHOR_OUTPUT_SCRIPT =
            scriptStringToByte("OP_2 FF FF OP_2 CHECKMULTISIG");

    //Spending from the anchor one just has to satisfy the multisig script.
    //All P2SH scripts need an additional FF at the end for the redeem script
    public static final byte[] COMMIT_INPUT_SCRIPT =
            Tools.hexStringToByteArray("00 FF FF FF");

    /*
     * Script for the balance of the channel going to the owner of the channel tx.
     * The counterparty can claim these funds using a revocation hash.
     *
     * Arguments:
     *      (1) revocation hash
     *      (2) public key of party that can claim funds using revocation
     *      (3) revocation delay
     *      (4) public key of owner of that output
     */
    public static final byte[] REVOCATION_OUTPUT = scriptStringToByte(
            "HASH160 FF EQUAL " +
                    "IF" +
                    "FF" +
                    "ELSE" +
                    "FF CSV DROP FF" +
                    "ENDIF" +
                    "CHECKSIG");

    public static Script getChannelTxOutputRevocation (RevocationHash revocationHash, ECKey revocableKey, ECKey counterpartyKey, int revocationDelay) {
        return produceScript(
                REVOCATION_OUTPUT,
                revocationHash.secretHash,
                counterpartyKey.getPubKey(),
                bigIntToByteArray(revocationDelay),
                revocableKey.getPubKey());
    }

    /*
     * Script for the sending part of a payment
     * The payment goes to a 2-of-2 if
     *      the CLTV timeout is over
     * and to the counterparty if
     *      it can provide the revocation hash
     *      it can provide the payment secret
     *
     * Arguments:
     *      (1) revocation hash
     *      (2) payment hash
     *      (3) public key of receiving party
     *      (4) absolute payment timeout
     *      (5) public key of receiving party
     *      (6) public key of sending party
     */
    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_SENDING_ONE =
            scriptStringToByte(
                    "HASH160 DUP FF EQUAL SWAP FF EQUAL ADD " +
                            "IF " + //If revocation or payment hash is supplied, allow taking directly
                            "FF CHECKSIG " +
                            "ELSE " + //Else pay to refund tx after timeout
                            "FF CLTV DROP OP_2 FF FF OP_2 CHECKMULTISIG " +
                            "ENDIF");

    public static Script getChannelTxOutputPaymentSending (ECKey revocableKey, ECKey counterpartyKey,
                                                           RevocationHash revocationHash, PaymentSecret secret, int refundTimeout) {
        return produceScript(CHANNEL_TX_OUTPUT_PAYMENT_SENDING_ONE, revocationHash.secretHash, secret.hash,
                counterpartyKey.getPubKey(), bigIntToByteArray(refundTimeout), revocableKey.getPubKey(), counterpartyKey.getPubKey());
    }

    //TODO not very efficient yet with 3 times client pubkey...
    //Script for the receiving part of a payment
    //The payment goes to a 2-of-2 if
    //      the receiver can provide the payment secret
    //and to the counterparty if
    //      it can provide the revocation hash
    //      the CLTV timeout is over
    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_RECEIVING_ONE =
            scriptStringToByte(
                    "HASH160 DUP FF EQUAL " +
                            "IF " + //If revocation or payment hash is supplied, allow taking directly
                            "DROP FF CHECKSIG " +
                            "ELSE " +
                            "FF EQUAL" +
                            "IF " + //Pay to redeem tx if payment hash is supplied
                            "OP_2 FF FF OP_2 CHECKMULTISIG " +
                            "ELSE " + //Pay to sender directly after timeout
                            "FF CLTV DROP FF CHECKSIG " +
                            "ENDIF " +
                            "ENDIF ");

    public static Script getChannelTxOutputPaymentReceiving (ECKey revocableKey, ECKey counterpartyKey,
                                                             RevocationHash revocationHash, PaymentSecret secret, int refundTimeout) {
        return produceScript(CHANNEL_TX_OUTPUT_PAYMENT_RECEIVING_ONE, revocationHash.secretHash, counterpartyKey.getPubKey(),
                secret.hash, revocableKey.getPubKey(), counterpartyKey.getPubKey(), bigIntToByteArray(refundTimeout), counterpartyKey.getPubKey());
    }

    public static Script getChannelTxOutputPayment (Channel channel, PaymentData paymentData) {
        return getChannelTxOutputPayment(channel, paymentData, channel.revoHashServerNext);
    }

    public static Script getChannelTxOutputPayment (Channel channel, PaymentData paymentData, RevocationHash revocationHash) {
        Script script;
        if (paymentData.sending) {
            script = ScriptTools.getChannelTxOutputPaymentSending(
                    channel.keyServer, channel.keyClient,
                    revocationHash,
                    paymentData.secret,
                    paymentData.timestampRefund);
        } else {
            script = ScriptTools.getChannelTxOutputPaymentReceiving(
                    channel.keyServer, channel.keyClient,
                    revocationHash,
                    paymentData.secret,
                    paymentData.timestampRefund);
        }
        return script;
    }

    public static PaymentSecret retrievePaymentSecret (Script script, PaymentSecret target) {
        for (ScriptChunk s : script.getChunks()) {
            if (s.isPushData()) {
                byte[] data = s.data;
                if (data.length == 20 && Arrays.equals(Tools.hashSecret(data), target.hash)) {
                    return new PaymentSecret(data);
                }
            }
        }
        return null;
    }

    public static Script scriptToP2SH (Script script) {
        Script translated = ScriptBuilder.createP2SHOutputScript(script);
        return translated;
    }

    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_SENDING_TIMEOUT = scriptStringToByte("00 FF FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_SENDING_REDEEM = scriptStringToByte("FF FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_SENDING_STEAL = scriptStringToByte("FF FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_RECEIVING_TIMEOUT = scriptStringToByte("00 00 FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_RECEIVING_REDEEM = scriptStringToByte("00 FF FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_RECEIVING_STEAL = scriptStringToByte("FF FF FF");

    public static final byte[] INPUT_ENCUMBERED_CLAIM = scriptStringToByte("00 FF FF FF");
    public static final byte[] INPUT_ENCUMBERED_TIMEOUT = scriptStringToByte("00 FF FF FF");

    public static Script getChannelTxOutputPaymentSteal (RevocationHash revocationHash, TransactionSignature transactionSignature, Script redeemingScript) {
        return produceScript(revocationHash.secret, transactionSignature.encodeToBitcoin(), redeemingScript.getProgram());
    }

    public static Script getPaymentTxOutput (ECKey keyReceiver, ECKey keyRevocation, RevocationHash revocationHash, int revocationTimeout) {
        return produceScript(REVOCATION_OUTPUT, revocationHash.secretHash, keyRevocation.getPubKey(),
                bigIntToByteArray(revocationTimeout), keyReceiver.getPubKey());
    }

    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_SENDING_ONEa = Tools.hexStringToByteArray("A9 FF 87 63 FF AC 67 B1 FF 75 52 FF FF 52 AE 68");
    public static final byte[] CHANNEL_TX_OUTPUT_MULTISIG1 = Tools.hexStringToByteArray("A9 FF 87 63 FF AC 67 52 FF FF 52 AE 68");

    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_SENDING = Tools.hexStringToByteArray("A9 76 FF 87 7C FF 87 93 63 FF 67 FF B1 FF B3 6D FF 68 AC");
    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_RECEIVING = Tools.hexStringToByteArray("A9 76 FF 87 63 FF B3 6D FF 67 FF 87 64 FF B1 75 68 FF 68 AC");

    public static Script getAnchorOutputScript (ECKey keyClient, ECKey keyServer) {
        List<byte[]> keyList = new ArrayList<>();
        keyList.add(keyClient.getPubKey());
        keyList.add(keyServer.getPubKey());

        keyList.sort(UnsignedBytes.lexicographicalComparator());

        return produceScript(ANCHOR_OUTPUT_SCRIPT, keyList.get(0), keyList.get(1));
    }

    public static Script getAnchorOutputScriptP2SH (ECKey keyClient, ECKey keyServer) {
        return ScriptTools.scriptToP2SH(getAnchorOutputScript(keyClient, keyServer));
    }

    /*
     * Input Script to spend the anchor
     */

    public static Script getCommitInputScript (byte[] signatureClient, byte[] signatureServer, ECKey keyClient, ECKey keyServer) {
        byte[] redeemscript = getAnchorOutputScript(keyClient, keyServer).getProgram();
        List<byte[]> keyList = new ArrayList<>();
        keyList.add(keyClient.getPubKey());
        keyList.add(keyServer.getPubKey());
        keyList.sort(UnsignedBytes.lexicographicalComparator());
        boolean inverted = !Arrays.equals(keyList.get(0), keyClient.getPubKey());
        if (!inverted) {
            return produceScript(COMMIT_INPUT_SCRIPT, signatureClient, signatureServer, redeemscript);
        } else {
            return produceScript(COMMIT_INPUT_SCRIPT, signatureServer, signatureClient, redeemscript);
        }
    }

    public static Script getVersionReturnScript (int version) {
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        scriptBuilder.op(106);
        scriptBuilder.data(integerToByteArray(version));
        return scriptBuilder.build();
    }

    //Script for the encumbered change payment of the commitment
    //The payment goes to the counterparty if
    //      it can provide the revocation hash
    //and to the owner of the commitment tx if
    //      the revocation delay passed

    public static Script produceScript (byte[] template, byte[]... parameters) {
        try {

            ScriptBuilder builder = new ScriptBuilder();

            int parameter = 0;
            for (byte chunk : template) {
                int op = chunk;
                if (op < 0) {
                    op = op + 256;
                }
                if (op == 255) {
                    builder.data(parameters[parameter]);
                    parameter++;
                } else if (op == 0) {
                    builder.data(new byte[0]);
                } else {
                    builder.op(op);
                }
            }

            //Bug in bitcoinJ when dealing with OP_0. Gets solved by reserializing.
            Script s = builder.build();
            return new Script(s.getProgram());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean testScript (byte[] script, byte[] template, byte[]... parameters) {

        Script s = new Script(script);
        List<ScriptChunk> chunks = s.getChunks();

        int parameter = 0;

        for (int i = 0; i < chunks.size(); i++) {
            boolean correct = false;
            ScriptChunk chunk = chunks.get(i);
            byte templateChunk = template[i];

            int op = templateChunk;
            if (op < 0) {
                op = op + 256;
            }
            if (op == 255) {
                if (chunk.isPushData()) {
                    if (Arrays.equals(chunk.data, parameters[parameter])) {
                        parameter++;
                        correct = true;
                    }
                }
            } else {
                if (chunk.opcode == op) {
                    correct = true;
                }
            }
            if (!correct) {
                return false;
            }

        }

        return true;
    }

    public static byte[] bigIntToByteArray (int i) {
        BigInteger integer = BigInteger.valueOf(i);

        return Utils.reverseBytes(Utils.bigIntegerToBytes(integer, 5));
    }

    public static byte[] integerToByteArray (int i) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(i);
        buffer.flip();
        return buffer.array();
    }

    public static int getVersionOutOfReturnOutput (Script script) {
        return byteToInteger(script.getChunks().get(1).data);
    }

    public static int byteToInteger (byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        return buffer.getInt();
    }

    public static byte[] scriptStringToByte (String script) {
        for (Map.Entry<String, String> a : scriptMap.entrySet()) {
            script = script.replaceAll(a.getKey(), a.getValue());
        }
        return Tools.hexStringToByteArray(script);
    }

}
