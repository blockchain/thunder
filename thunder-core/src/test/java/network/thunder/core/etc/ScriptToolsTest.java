package network.thunder.core.etc;

import network.thunder.core.helper.ScriptTools;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

//TODO need to rewrite the tests for wrong signatures / parameters after switching to 2-of-2 anchor
public class ScriptToolsTest {

    ECKey keyServer = ECKey.fromPrivate(Tools.hexStringToByteArray("95b9ba99ce547d0346ae69adeef116b51c77f3285fe8941c9bfe83a0857d606e"));
    ECKey keyClient = ECKey.fromPrivate(Tools.hexStringToByteArray("514651734ecf29afc45e520854842ad0aff937418be72b7e5b688b0fc0ef8661"));
    ECKey keyServerA = ECKey.fromPrivate(Tools.hexStringToByteArray("ea827f0b72fad12d46ce11da5db814433711fa15bcd834b9a94de5fb3319b71d"));
    ECKey keyClientA = ECKey.fromPrivate(Tools.hexStringToByteArray("1ae1c7ca3d0e0d8372d7111a231a0d45bf7abeff22342db7ab3d4d200b7f4ce5"));

    Transaction transactionOutput;
    Transaction transactionInput;

    byte[] revocationServer = Tools.hexStringToByteArray("E3411336485A2E167EC4CB8BE4A8C75E19255CB0");
    byte[] revocationClient = Tools.hexStringToByteArray("E65AA748832A949DDF1854E6206620F07F8C49ED");

    @Before
    public void setup () throws NoSuchAlgorithmException {
        Context.getOrCreate(Constants.getNetwork());

        transactionOutput = new Transaction(Constants.getNetwork());
        transactionInput = new Transaction(Constants.getNetwork());
    }

    @Test
    public void shouldCreateSpendableAnchorTransactionWithCommitmentTransaction () {

        Script outputScript = ScriptTools.getAnchorOutputScript(keyClient, keyServer);
        Script outputScriptP2SH = ScriptTools.getAnchorOutputScriptP2SH(keyClient, keyServer);

        transactionInput.addInput(Sha256Hash.wrap("6d651fd23456606298348f7e750321cba2e3e752d433aa537ea289593645d2e4"), 0, Tools.getDummyScript());
        transactionInput.addOutput(Coin.valueOf(999000), Tools.getDummyScript());

        TransactionSignature signatureClient = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyClient);
        TransactionSignature signatureServer = Tools.getSignature(transactionInput, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getCommitInputScript(signatureClient.encodeToBitcoin(), signatureServer.encodeToBitcoin(), keyClient, keyServer);

        transactionInput.getInput(0).setScriptSig(inputScript);

        inputScript.correctlySpends(transactionInput, 0, outputScriptP2SH);
    }
}