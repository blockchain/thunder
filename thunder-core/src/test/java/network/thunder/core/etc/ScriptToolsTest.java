package network.thunder.core.etc;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.junit.Before;

import java.security.NoSuchAlgorithmException;

//TODO need to rewrite the tests for wrong signatures / parameters after switching to 2-of-2 anchor
//TODO rewrite all tests once bitcoinJ is able to verify witnesses
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

}