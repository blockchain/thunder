package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.lightning.RevocationHash;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Created by matsjerratsch on 03/11/15.
 */
public class ScriptTools {
    /*
    A9 HASH160
    76 DUP
    87 EQUAL
    7C SWAP
    93 ADD
    63 IF
    64 NOTIF
    67 ELSE
    68 ENDIF
    B1 CLTV
    B3 CSV
    75 DROP
    6D 2DROP
    AC CHECKSIG
     */

    //Due to some policy, miners won't include OP_NOP until they are activated. Not even on testnet.
    public static final boolean CLTV_CSV_ENABLED = true;

    //Templates are like normal scripts but with 0xFF as placeholders for parameters
    public static final byte[] ANCHOR_OUTPUT_SCRIPT = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF 68 52 7C FF 52 AE");

    public static final byte[] ESCAPE_INPUT_SCRIPT = Tools.hexStringToByteArray("00 FF FF FF FF");
    public static final byte[] COMMIT_INPUT_SCRIPT = Tools.hexStringToByteArray("00 FF FF 00 FF");

    public static final byte[] ESCAPE_OUTPUT_SCRIPT = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF B3 75 FF 68 AC");
    public static final byte[] ESCAPE_OUTPUT_SCRIPT_WITHOUT_CSV = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF 75 FF 68 AC");
    public static final byte[] ESCAPE_INPUT_REVOCATION_SCRIPT = Tools.hexStringToByteArray("FF FF FF");
    public static final byte[] ESCAPE_INPUT_TIMEOUT_SCRIPT = Tools.hexStringToByteArray("FF 00 FF");

    public static final byte[] FAST_ESCAPE_OUTPUT_SCRIPT = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF B3 75 FF 68 AC");
    public static final byte[] FAST_ESCAPE_OUTPUT_SCRIPT_WITHOUT_CSV = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF 75 FF 68 AC");
    public static final byte[] FAST_ESCAPE_INPUT_SECRET_SCRIPT = Tools.hexStringToByteArray("FF FF FF");
    public static final byte[] FAST_ESCAPE_INPUT_TIMEOUT_SCRIPT = Tools.hexStringToByteArray("FF 00 FF");

    public static final byte[] CHANNEL_TX_OUTPUT_REVOCATION = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF B3 75  FF 68 AC");
    public static final byte[] CHANNEL_TX_OUTPUT_PLAIN = Tools.hexStringToByteArray("FF AC");

    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_SENDING = Tools.hexStringToByteArray("A9 76 FF 87 7C FF 87 93 63 FF 67 FF B1 FF B3 6D FF 68 AC");
    public static final byte[] CHANNEL_TX_OUTPUT_PAYMENT_RECEIVING = Tools.hexStringToByteArray("A9 76 FF 87 63 FF B3 6D FF 67 FF 87 64 FF B1 75 68 FF 68 AC");

    public static Script getAnchorOutputScript (byte[] secretServerHash, ECKey keyClient, ECKey keyClientA, ECKey keyServer) {
        return produceScript(ANCHOR_OUTPUT_SCRIPT, secretServerHash, keyClientA.getPubKey(), keyClient.getPubKey(), keyServer.getPubKey());
    }

    /*
     * Input Script to spend the anchor
     */

    public static Script getEscapeInputScript (byte[] signatureClientA, byte[] signatureServer, byte[] secretServer, byte[] secretServerHash, ECKey
            keyClient, ECKey keyClientA, ECKey keyServer) {
        byte[] redeemscript = getAnchorOutputScript(secretServerHash, keyClient, keyClientA, keyServer).getProgram();
        return produceScript(ESCAPE_INPUT_SCRIPT, signatureClientA, signatureServer, secretServer, redeemscript);
    }

    public static Script getCommitInputScript (byte[] signatureClient, byte[] signatureServer, byte[] secretServerHash, ECKey
            keyClient, ECKey keyClientA, ECKey keyServer) {
        byte[] redeemscript = getAnchorOutputScript(secretServerHash, keyClient, keyClientA, keyServer).getProgram();
        return produceScript(COMMIT_INPUT_SCRIPT, signatureClient, signatureServer, redeemscript);
    }

    /*
     * Output scripts to build the escape and fast escape transactions.
     */

    public static Script getEscapeOutputScript (byte[] revocationHashServer, ECKey keyServer, ECKey keyClient, int revocationDelay) {
        if (CLTV_CSV_ENABLED) {
            return produceScript(ESCAPE_OUTPUT_SCRIPT, revocationHashServer, keyClient.getPubKey(), integerToByteArray(revocationDelay), keyServer.getPubKey());
        } else {
            return produceScript(ESCAPE_OUTPUT_SCRIPT_WITHOUT_CSV, revocationHashServer, keyClient.getPubKey(), integerToByteArray(revocationDelay),
                    keyServer.getPubKey());
        }
    }

    public static Script getEscapeInputRevocationScript (byte[] revocationHashServer, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureClient, byte[] revocationClient) {
        byte[] redeemScript = getEscapeOutputScript(revocationHashServer, keyServer, keyClient, revocationDelay).getProgram();
        return produceScript(ESCAPE_INPUT_REVOCATION_SCRIPT, signatureClient, revocationClient, redeemScript);
    }

    public static Script getEscapeInputTimeoutScript (byte[] revocationHashServer, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureServer) {
        byte[] redeemScript = getEscapeOutputScript(revocationHashServer, keyServer, keyClient, revocationDelay).getProgram();
        return produceScript(ESCAPE_INPUT_TIMEOUT_SCRIPT, signatureServer, redeemScript);
    }

    public static Script getFastEscapeOutputScript (byte[] secretHashClient, ECKey keyServer, ECKey keyClient, int revocationDelay) {
        if (CLTV_CSV_ENABLED) {
            return produceScript(FAST_ESCAPE_OUTPUT_SCRIPT, secretHashClient, keyServer.getPubKey(), integerToByteArray(revocationDelay), keyClient.getPubKey
                    ());
        } else {
            return produceScript(FAST_ESCAPE_OUTPUT_SCRIPT_WITHOUT_CSV, secretHashClient, keyServer.getPubKey(), integerToByteArray(revocationDelay),
                    keyClient.getPubKey());
        }
    }

    public static Script getFastEscapeInputSecretScript (byte[] secretClientHash, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureServer, byte[] secretClient) {
        byte[] redeemScript = getFastEscapeOutputScript(secretClientHash, keyServer, keyClient, revocationDelay).getProgram();
        return produceScript(FAST_ESCAPE_INPUT_SECRET_SCRIPT, signatureServer, secretClient, redeemScript);
    }

    public static Script getFastEscapeInputTimeoutScript (byte[] secretClientHash, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureClient) {
        byte[] redeemScript = getFastEscapeOutputScript(secretClientHash, keyServer, keyClient, revocationDelay).getProgram();
        return produceScript(FAST_ESCAPE_INPUT_TIMEOUT_SCRIPT, signatureClient, redeemScript);
    }

    public static Script getChannelTxOutputRevocation (RevocationHash revocationHash, ECKey keyServer, ECKey keyClient, int revocationDelay) {
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

}
