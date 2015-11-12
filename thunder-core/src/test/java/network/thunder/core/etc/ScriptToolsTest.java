package network.thunder.core.etc;

import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by matsjerratsch on 04/11/15.
 */
public class ScriptToolsTest {

    ECKey keyServer = ECKey.fromPrivate(Tools.hexStringToByteArray("95b9ba99ce547d0346ae69adeef116b51c77f3285fe8941c9bfe83a0857d606e"));
    ECKey keyClient = ECKey.fromPrivate(Tools.hexStringToByteArray("514651734ecf29afc45e520854842ad0aff937418be72b7e5b688b0fc0ef8661"));
    ECKey keyServerA = ECKey.fromPrivate(Tools.hexStringToByteArray("ea827f0b72fad12d46ce11da5db814433711fa15bcd834b9a94de5fb3319b71d"));
    ECKey keyClientA = ECKey.fromPrivate(Tools.hexStringToByteArray("1ae1c7ca3d0e0d8372d7111a231a0d45bf7abeff22342db7ab3d4d200b7f4ce5"));

    Transaction transactionOutput;
    Transaction transactionInput;

    byte[] secretServer = Tools.hexStringToByteArray("E34BAF76398A2E167EC4CB8BE4A8C75E19255CB0");
    byte[] secretClient = Tools.hexStringToByteArray("E650A18B973A949DDF1854E6206620F07F8C49ED");
    byte[] secretServerHash = Tools.hashSecret(secretServer);
    byte[] secretClientHash = Tools.hashSecret(secretClient);

    NetworkParameters params = NetworkParameters.prodNet();

    @Before
    public void setup () throws NoSuchAlgorithmException {
        Context.getOrCreate(params);

        transactionOutput = new Transaction(params);
        transactionInput = new Transaction(params);
    }

    @Test
    public void shouldCreateSpendableAnchorTransactionWithEscapeTransaction () {

        Script outputScript = ScriptTools.getAnchorOutputScript(secretServerHash, keyClient, keyClientA, keyServer);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureClientA = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyClientA);
        TransactionSignature signatureServer = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getEscapeInputScript(signatureClientA.encodeToBitcoin(), signatureServer.encodeToBitcoin(), secretServer,
                secretServerHash, keyClient, keyClientA, keyServer);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test
    public void shouldCreateSpendableAnchorTransactionWithCommitmentTransaction () {

        Script outputScript = ScriptTools.getAnchorOutputScript(secretServerHash, keyClient, keyClientA, keyServer);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureClient = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyClient);
        TransactionSignature signatureServer = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getCommitInputScript(signatureClient.encodeToBitcoin(), signatureServer.encodeToBitcoin(),
                secretServerHash, keyClient, keyClientA, keyServer);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test
    public void shouldCreateSpendableEscapeTransactionWithRevocationTransaction () {

        Script outputScript = ScriptTools.getEscapeOutputScript(secretServerHash, keyServer, keyClient, 60 * 60 * 1000);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureClient = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyClient);

        Script inputScript = ScriptTools.getEscapeInputRevocationScript(secretServerHash, keyServer, keyClient, 60 * 60 * 1000, signatureClient
                .encodeToBitcoin(), secretServer);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test
    public void shouldCreateSpendableEscapeTransactionWithTimeoutTransaction () {

        Script outputScript = ScriptTools.getEscapeOutputScript(secretServerHash, keyServer, keyClient, 60 * 60 * 1000);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureServer = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getEscapeInputTimeoutScript(secretServerHash, keyServer, keyClient, 60 * 60 * 1000, signatureServer.encodeToBitcoin());

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test
    public void shouldCreateSpendableFastEscapeTransactionWithSecretTransaction () {

        Script outputScript = ScriptTools.getFastEscapeOutputScript(secretClientHash, keyServer, keyClient, 60 * 60 * 1000);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureServer = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getFastEscapeInputSecretScript(secretClientHash, keyServer, keyClient, 60 * 60 * 1000, signatureServer
                .encodeToBitcoin(), secretClient);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test
    public void shouldCreateSpendableFastEscapeTransactionWithTimeoutTransaction () {

        Script outputScript = ScriptTools.getFastEscapeOutputScript(secretClientHash, keyServer, keyClient, 60 * 60 * 1000);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureClient = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyClient);

        Script inputScript = ScriptTools.getFastEscapeInputTimeoutScript(secretClientHash, keyServer, keyClient, 60 * 60 * 1000, signatureClient
                .encodeToBitcoin());

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test(expected = ScriptException.class)
    public void shouldThrowExceptionBecauseOfWrongSecretWhenTryingToSpendEscapeTransaction () {
        Script outputScript = ScriptTools.getEscapeOutputScript(secretServerHash, keyServer, keyClient, 60 * 60 * 1000);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureClient = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyClient);

        byte[] b = new byte[]{0x00};
        System.arraycopy(b, 0, secretServer, 15, 1); //Copy a wrong byte into the secret, should not work anymore now

        Script inputScript = ScriptTools.getEscapeInputRevocationScript(secretServerHash, keyServer, keyClient, 60 * 60 * 1000, signatureClient
                .encodeToBitcoin(), secretServer);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test(expected = ScriptException.class)
    public void shouldThrowExceptionBecauseOfWrongSecretWhenTryingToSpendFastEscapeTransaction () {
        Script outputScript = ScriptTools.getFastEscapeOutputScript(secretClientHash, keyServer, keyClient, 60 * 60 * 1000);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureServer = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyServer);

        byte[] b = new byte[]{0x00};
        System.arraycopy(b, 0, secretClient, 15, 1); //Copy a wrong byte into the secret, should not work anymore now

        Script inputScript = ScriptTools.getFastEscapeInputSecretScript(secretClientHash, keyServer, keyClient, 60 * 60 * 1000, signatureServer
                .encodeToBitcoin(), secretClient);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test(expected = ScriptException.class)
    public void shouldThrowExceptionBecauseSignatureOrderIsNotCorrect () {

        Script outputScript = ScriptTools.getAnchorOutputScript(secretServerHash, keyClient, keyClientA, keyServer);
        Script outputScriptP2SH = ScriptBuilder.createP2SHOutputScript(outputScript);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureClientA = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyClientA);
        TransactionSignature signatureServer = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getEscapeInputScript(signatureServer.encodeToBitcoin(), signatureClientA.encodeToBitcoin(), secretServer,
                secretServerHash, keyClient, keyClientA, keyServer);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }

    @Test
    public void shouldProduceCorrectScript () throws Exception {

        byte[] p1 = Tools.hexStringToByteArray("aa bb cc dd ee");
        byte[] p2 = Tools.hexStringToByteArray("04 92 84 00 82 71 00 74 38 92 48 17");
        byte[] p3 = Tools.hexStringToByteArray("08 79 66 88");
        byte[] p4 = Tools.hexStringToByteArray("09 87 09 38 27 aa 98 00 83 a9 ff 89 11 39 47 89 28 11 99 02 83 99 04 28 40 66 78 09 65");

        Script script = ScriptTools.produceScript(ScriptTools.ANCHOR_OUTPUT_SCRIPT, p1, p2, p3, p4);

        assertTrue(Arrays.equals(script.getProgram(), Tools.hexStringToByteArray
                ("A905AABBCCDDEE87630C04928400827100743892481767040879668868527C1D0987093827AA980083A9FF89113947892811990283990428406678096552AE")));

    }

    @Test
    public void testScriptShouldReturnTrue () throws Exception {
        byte[] script = Tools.hexStringToByteArray
                ("A905AABBCCDDEE87630C04928400827100743892481767040879668868527C1D0987093827AA980083A9FF89113947892811990283990428406678096552AE");

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