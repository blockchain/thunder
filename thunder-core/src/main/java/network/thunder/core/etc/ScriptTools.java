package network.thunder.core.etc;

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

    //Templates are like normal scripts but with 0x00 as placeholders for parameters
    public static final byte[] ANCHOR_OUTPUT_SCRIPT = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF 68 52 7C FF 52 AE");

    public static final byte[] ESCAPE_INPUT_SCRIPT = Tools.hexStringToByteArray("00 FF FF FF FF");
    public static final byte[] COMMIT_INPUT_SCRIPT = Tools.hexStringToByteArray("00 FF FF 00 FF");

    public static final byte[] ESCAPE_OUTPUT_SCRIPT = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF B2 75 FF 68 AC");
    public static final byte[] ESCAPE_INPUT_REVOCATION_SCRIPT = Tools.hexStringToByteArray("FF FF FF");
    public static final byte[] ESCAPE_INPUT_TIMEOUT_SCRIPT = Tools.hexStringToByteArray("FF 00 FF");

    public static final byte[] FAST_ESCAPE_OUTPUT_SCRIPT = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF B2 75 FF 68 AC");
    public static final byte[] FAST_ESCAPE_INPUT_SECRET_SCRIPT = Tools.hexStringToByteArray("FF FF FF");
    public static final byte[] FAST_ESCAPE_INPUT_TIMEOUT_SCRIPT = Tools.hexStringToByteArray("FF 00 FF");

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

    public static Script getEscapeOutputScript (byte[] secretServerHash, ECKey keyServer, ECKey keyClient, int revocationDelay) {
        return produceScript(ESCAPE_OUTPUT_SCRIPT, secretServerHash, keyClient.getPubKey(), integerToByteArray(revocationDelay), keyServer.getPubKey());
    }

    public static Script getEscapeInputRevocationScript (byte[] secretServerHash, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureClient, byte[] secretServer) {
        byte[] redeemScript = getEscapeOutputScript(secretServerHash, keyClient, keyServer, revocationDelay).getProgram();
        return produceScript(ESCAPE_INPUT_REVOCATION_SCRIPT, signatureClient, secretServer, redeemScript);
    }

    public static Script getEscapeInputTimeoutScript (byte[] secretServerHash, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureServer) {
        byte[] redeemScript = getEscapeOutputScript(secretServerHash, keyClient, keyServer, revocationDelay).getProgram();
        return produceScript(ESCAPE_INPUT_TIMEOUT_SCRIPT, signatureServer, redeemScript);
    }

    public static Script getFastEscapeOutputScript (byte[] secretClientHash, ECKey keyServer, ECKey keyClient, int revocationDelay) {
        return produceScript(FAST_ESCAPE_OUTPUT_SCRIPT, secretClientHash, keyClient.getPubKey(), integerToByteArray(revocationDelay), keyServer.getPubKey());
    }

    public static Script getFastEscapeInputRevocationScript (byte[] secretClientHash, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureServer, byte[] secretClient) {
        byte[] redeemScript = getFastEscapeOutputScript(secretClientHash, keyClient, keyServer, revocationDelay).getProgram();
        return produceScript(FAST_ESCAPE_INPUT_SECRET_SCRIPT, signatureServer, secretClient, redeemScript);
    }

    public static Script getFastEscapeInputTimeoutScript (byte[] secretClientHash, ECKey keyServer, ECKey keyClient, int revocationDelay, byte[]
            signatureClient) {
        byte[] redeemScript = getFastEscapeOutputScript(secretClientHash, keyClient, keyServer, revocationDelay).getProgram();
        return produceScript(FAST_ESCAPE_INPUT_TIMEOUT_SCRIPT, signatureClient, redeemScript);
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
