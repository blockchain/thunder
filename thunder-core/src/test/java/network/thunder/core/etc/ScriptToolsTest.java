package network.thunder.core.etc;

import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by matsjerratsch on 04/11/15.
 */
public class ScriptToolsTest {

    @Test
    public void shouldProduceCorrectScript () throws Exception {

        byte[] p1 = Tools.hexStringToByteArray("aa bb cc dd ee");
        byte[] p2 = Tools.hexStringToByteArray("04 92 84 00 82 71 00 74 38 92 48 17");
        byte[] p3 = Tools.hexStringToByteArray("08 79 66 88");
        byte[] p4 = Tools.hexStringToByteArray("09 87 09 38 27 aa 98 00 83 a9 ff 89 11 39 47 89 28 11 99 02 83 99 04 28 40 66 78 09 65");

        byte[] script = ScriptTools.produceScript(ScriptTools.ANCHOR_OUTPUT_SCRIPT, p1, p2, p3, p4);

        assertTrue(Arrays.equals(script, Tools.hexStringToByteArray
                ("A905AABBCCDDEE87630C049284008271007438924817670408796688527C1D0987093827AA980083A9FF89113947892811990283990428406678096552AE")));

    }

    @Test
    public void testScriptShouldReturnTrue () throws Exception {
        byte[] script = Tools.hexStringToByteArray
                ("A905AABBCCDDEE87630C049284008271007438924817670408796688527C1D0987093827AA980083A9FF89113947892811990283990428406678096552AE");

        byte[] p1 = Tools.hexStringToByteArray("aa bb cc dd ee");
        byte[] p2 = Tools.hexStringToByteArray("04 92 84 00 82 71 00 74 38 92 48 17");
        byte[] p3 = Tools.hexStringToByteArray("08 79 66 88");
        byte[] p4 = Tools.hexStringToByteArray("09 87 09 38 27 aa 98 00 83 a9 ff 89 11 39 47 89 28 11 99 02 83 99 04 28 40 66 78 09 65");

        assertTrue(ScriptTools.testScript(script, ScriptTools.ANCHOR_OUTPUT_SCRIPT, p1, p2, p3, p4));

    }

    @Test
    public void testScriptShouldReturnFalse () throws Exception {
        //Changed one byte of the original sequence
        byte[] script = Tools.hexStringToByteArray
                ("A905AABBCCDDEE87630C049284008271007438924817570408796688527C1D0987093827AA980083A9FF89113947892811990283990428406678096552AE");

        byte[] p1 = Tools.hexStringToByteArray("aa bb cc dd ee");
        byte[] p2 = Tools.hexStringToByteArray("04 92 84 00 82 71 00 74 38 92 48 17");
        byte[] p3 = Tools.hexStringToByteArray("08 79 66 88");
        byte[] p4 = Tools.hexStringToByteArray("09 87 09 38 27 aa 98 00 83 a9 ff 89 11 39 47 89 28 11 99 02 83 99 04 28 40 66 78 09 65");

        assertFalse(ScriptTools.testScript(script, ScriptTools.ANCHOR_OUTPUT_SCRIPT, p1, p2, p3, p4));

    }
}