package network.thunder.core.etc;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;

import java.util.Arrays;
import java.util.List;

/**
 * Created by matsjerratsch on 03/11/15.
 */
public class ScriptTools {

    //Templates are like normal scripts but with 0x00 as placeholders for parameters
    public static final byte[] ANCHOR_OUTPUT_SCRIPT = Tools.hexStringToByteArray("A9 FF 87 63 FF 67 FF 52 7C FF 52 AE");

    public static byte[] getAnchorOutputScript (byte[] secretHash, ECKey keyClient, ECKey keyClientA, ECKey keyServer) {
        return produceScript(ANCHOR_OUTPUT_SCRIPT, secretHash, keyClient.getPubKey(), keyClientA.getPubKey(), keyServer.getPubKey());
    }

    public static byte[] produceScript (byte[] template, byte[]... parameters) {
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
                } else {
                    builder.op(op);
                }
            }

            return builder.build().getProgram();

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

}
