package network.thunder.core.helper.blockchain.bciapi.pushtx;

import network.thunder.core.helper.blockchain.bciapi.APIException;
import network.thunder.core.helper.blockchain.bciapi.HttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class reflects the functionality provided at
 * https://blockchain.info/pushtx. It allows users to broadcast hex encoded
 * transactions to the bitcoin network.
 */
public class PushTx {
    /**
     * Pushes a hex encoded transaction to the network.
     *
     * @param tx Hex encoded transaction
     * @throws APIException If the server returns an error (malformed tx etc.)
     */
    public static void pushTx (String tx) throws APIException, IOException {
        pushTx(tx, null);
    }

    /**
     * Pushes a hex encoded transaction to the network.
     *
     * @param tx      Hex encoded transaction
     * @param apiCode Blockchain.info API code (optional, nullable)
     * @throws APIException If the server returns an error (malformed tx etc.)
     */
    public static void pushTx (String tx, String apiCode) throws APIException, IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("tx", tx);

        if (apiCode != null) {
            params.put("api_code", apiCode);
        }

        HttpClient.getInstance().post("pushtx", params);
    }
}
