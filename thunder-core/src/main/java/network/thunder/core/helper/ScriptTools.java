package network.thunder.core.helper;

import com.google.common.primitives.UnsignedBytes;
import com.google.gson.internal.LinkedTreeMap;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.jetbrains.annotations.NotNull;

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

    public static final byte[] CHANNEL_TX_OUTPUT_REVOCATION = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF B3 75  FF 68 AC");
    public static final byte[] CHANNEL_TX_OUTPUT_PLAIN = Tools.hexStringToByteArray("FF AC");

    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_SENDING_ONE =
            scriptStringToByte(
                    "HASH160 DUP FF EQUAL SWAP FF EQUAL ADD " +
                            "IF " + //If revocation or payment hash is supplied, allow taking directly
                            "FF CHECKSIG " +
                            "ELSE " + //Else pay to refund tx after timeout
                            "FF CLTV DROP OP_2 FF FF OP_2 CHECKMULTISIG " +
                            "ENDIF");

    public static Script getChannelTxOutputPaymentSending (ECKey keyServer, ECKey keyClient,
                                                           RevocationHash revocationHash, PaymentSecret secret, int refundTimeout) {
        return produceScript(CHANNEL_TX_OUTPUT_PAYMENT_SENDING_ONE, revocationHash.getSecretHash(), secret.hash,
                keyClient.getPubKey(), integerToByteArray(refundTimeout), keyServer.getPubKey(), keyClient.getPubKey());
    }

    //TODO not very efficient yet with 3 times client pubkey...
    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_RECEIVING_ONE =
            scriptStringToByte(
                    "HASH160 FF EQUAL " +
                            "IF " + //If revocation or payment hash is supplied, allow taking directly
                            "FF CHECKSIG " +
                            "ELSE " +
                            "HASH160 FF EQUAL" +
                            "IF " + //Pay to redeem tx if payment hash is supplied
                            "OP_2 FF FF OP_2 CHECKMULTISIG " +
                            "ELSE " + //Pay to sender directly after timeout
                            "FF CLTV DROP FF CHECKSIG " +
                            "ENDIF " +
                            "ENDIF ");

    public static Script getChannelTxOutputPaymentReceiving (ECKey keyServer, ECKey keyClient,
                                                             RevocationHash revocationHash, PaymentSecret secret, int refundTimeout) {
        return produceScript(CHANNEL_TX_OUTPUT_PAYMENT_RECEIVING_ONE, revocationHash.getSecretHash(), keyClient.getPubKey(),
                secret.hash, keyServer.getPubKey(), keyClient.getPubKey(), integerToByteArray(refundTimeout), keyClient.getPubKey());
    }

    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_SENDING_TIMEOUT = scriptStringToByte("00 FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_SENDING_REDEEM = scriptStringToByte("FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_SENDING_STEAL = scriptStringToByte("FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_RECEIVING_TIMEOUT = scriptStringToByte("00 00 FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_RECEIVING_REDEEM = scriptStringToByte("00 FF FF");
    public static final byte[] CHANNEL_TX_INPUT_PAYMENT_RECEIVING_STEAL = scriptStringToByte("FF FF");

    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_TWO =
            scriptStringToByte(
                    "HASH160 FF EQUAL " +
                            "IF " +
                            "FF CHECKSIG " +
                            "ELSE " +
                            "FF CSV DROP FF CHECKSIG " +
                            "ENDIF");

    public static Script getPaymentTxOutput (ECKey keyReceiver, ECKey keyRevocation, RevocationHash revocationHash, int revocationTimeout) {
        return produceScript(CHANNEL_TX_OUTPUT_PAYMENT_TWO, revocationHash.getSecretHash(), keyRevocation.getPubKey(),
                integerToByteArray(revocationTimeout), keyReceiver.getPubKey());
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
        return ScriptBuilder.createP2SHOutputScript(getAnchorOutputScript(keyClient, keyServer));
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

    public static Script getChannelTxOutputRevocation (@NotNull RevocationHash revocationHash, @NotNull ECKey keyServer, ECKey keyClient, int revocationDelay) {
        return produceScript(CHANNEL_TX_OUTPUT_REVOCATION, revocationHash.getSecretHash(), keyClient.getPubKey(), integerToByteArray(revocationDelay),
                keyServer.getPubKey());
    }

    public static Script getChannelTxOutputPlain (ECKey keyClient) {
        return produceScript(CHANNEL_TX_OUTPUT_PLAIN, keyClient.getPubKey());
    }

    public static Script getChannelTxOutputSending (RevocationHash revocationHash, PaymentData paymentData, ECKey keyServer, ECKey keyClient) {
        return produceScript(CHANNEL_TX_OUTPUT_PAYMENT_SENDING, paymentData.secret.hash, revocationHash.getSecretHash(), keyClient.getPubKey(),
                integerToByteArray(paymentData.timestampRefund), integerToByteArray(paymentData.csvDelay), keyServer.getPubKey());
    }

    public static Script getChannelTxOutputReceiving (RevocationHash revocationHash, PaymentData paymentData, ECKey keyServer, ECKey keyClient) {
        return produceScript(CHANNEL_TX_OUTPUT_PAYMENT_RECEIVING, paymentData.secret.hash, integerToByteArray(paymentData.csvDelay),
                keyClient.getPubKey(), revocationHash.getSecretHash(), integerToByteArray(paymentData.timestampRefund), keyServer.getPubKey());
    }

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

    public static byte[] integerToByteArray (int i) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(i);
        buffer.flip();
        return buffer.array();
    }

    public static byte[] scriptStringToByte (String script) {
        for (Map.Entry<String, String> a : scriptMap.entrySet()) {
            script = script.replaceAll(a.getKey(), a.getValue());
        }
        return Tools.hexStringToByteArray(script);
    }

}
