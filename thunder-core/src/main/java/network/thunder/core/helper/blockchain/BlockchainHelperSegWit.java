package network.thunder.core.helper.blockchain;

import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.json.JSONObject;

import java.net.URL;
import java.util.Scanner;

/**
 * BlockchainHelper based upon connecting directly to p2p network with bitcoinJ.
 * <p>
 * Would be great to have another implementation in the future that only connects to a local bitcoind,
 * to greatly improve privacy and reduce dependency on third parties.
 */
public class BlockchainHelperSegWit extends BlockchainHelperImpl {

    @Override
    public Transaction getTransaction (Sha256Hash hash) {
        try {
            //TODO dependent on web API still, add another implementation that uses a full node
            //This is not a security problem per-se though, as it can't provide us invalid data
            String out = new Scanner(new URL("http://tbtc.blockr.io/api/v1/tx/raw/" + hash.toString()).openStream(), "UTF-8").useDelimiter("\\A").next();
            JSONObject jsonObject = new JSONObject(out);
            String payload = (String) jsonObject.getJSONObject("data").getJSONObject("tx").get("hex");
            Transaction t = new Transaction(Constants.getNetwork(), Tools.hexStringToByteArray(payload));

            if (!t.getHash().equals(hash)) {
                throw new RuntimeException("Got wrong tx hash?" + t);
            } else {
                return t;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
