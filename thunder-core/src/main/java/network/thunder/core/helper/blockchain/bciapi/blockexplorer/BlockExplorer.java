package network.thunder.core.helper.blockchain.bciapi.blockexplorer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.thunder.core.helper.blockchain.bciapi.APIException;
import network.thunder.core.helper.blockchain.bciapi.HttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The BlockExplorer class reflects the functionality documented at
 * https://blockchain.info/api/blockchain_api. It can be used to query the block chain,
 * fetch block, transaction and address data, get unspent outputs for an address etc.
 */
public class BlockExplorer {
    private String apiCode;

    public BlockExplorer () {
        this(null);
    }

    /**
     * @param apiCode Blockchain.info API code (optional, nullable)
     */
    public BlockExplorer (String apiCode) {
        this.apiCode = apiCode;
    }

    /**
     * Gets a single transaction based on a transaction index.
     *
     * @param txIndex Transaction index
     * @return An instance of the {@link Transaction} class
     * @throws APIException If the server returns an error
     */
    public Transaction getTransaction (long txIndex) throws APIException, IOException {
        return getTransaction(String.valueOf(txIndex));
    }

    /**
     * Gets a single transaction based on a transaction hash.
     *
     * @param txHash Transaction hash
     * @return An instance of the {@link Transaction} class
     * @throws APIException If the server returns an error
     */
    public Transaction getTransaction (String txHash) throws APIException, IOException {
        String response = HttpClient.getInstance().get("rawtx/" + txHash + "?api_code=" + apiCode, null);
        JsonObject txJson = new JsonParser().parse(response).getAsJsonObject();
        return new Transaction(txJson);
    }

    /**
     * Gets a single block based on a block index.
     *
     * @param blockIndex Block index
     * @return An instance of the {@link Block} class
     * @throws APIException If the server returns an error
     */
    public Block getBlock (long blockIndex) throws APIException, IOException {
        return getBlock(String.valueOf(blockIndex));
    }

    /**
     * Gets a single block based on a block hash.
     *
     * @param blockHash Block hash
     * @return An instance of the {@link Block} class
     * @throws APIException If the server returns an error
     */
    public Block getBlock (String blockHash) throws APIException, IOException {
        String response = HttpClient.getInstance().get("rawblock/" + blockHash + "?api_code=" + apiCode, null);
        JsonObject blockJson = new JsonParser().parse(response).getAsJsonObject();
        return new Block(blockJson);
    }

    /**
     * Gets data for a single address.
     *
     * @param address Base58check or hash160 address string
     * @return An instance of the {@link Address} class
     * @throws APIException If the server returns an error
     */
    public Address getAddress (String address) throws APIException, IOException {
        String response = HttpClient.getInstance().get("rawaddr/" + address + "?api_code=" + apiCode, null);
        JsonObject addrJson = new JsonParser().parse(response).getAsJsonObject();
        return new Address(addrJson);
    }

    /**
     * Gets a list of blocks at the specified height. Normally, only one block will be
     * returned, but in case of a chain fork, multiple blocks may be present.
     *
     * @param height Block height
     * @return A list of blocks at the specified height
     * @throws APIException If the server returns an error
     */
    public List<Block> getBlocksAtHeight (long height) throws APIException, IOException {
        List<Block> blocks = new ArrayList<Block>();

        Map<String, String> params = new HashMap<String, String>();
        params.put("format", "json");
        if (apiCode != null) {
            params.put("api_code", apiCode);
        }

        String response = HttpClient.getInstance().get("block-height/" + height, params);
        JsonObject blocksJson = new JsonParser().parse(response).getAsJsonObject();

        for (JsonElement blockElem : blocksJson.get("blocks").getAsJsonArray()) {
            blocks.add(new Block(blockElem.getAsJsonObject()));
        }

        return blocks;
    }

    /**
     * Gets unspent outputs for a single address.
     *
     * @param address Base58check or hash160 address string
     * @return A list of unspent outputs for the specified address
     * @throws APIException If the server returns an error
     */
    public List<UnspentOutput> getUnspentOutputs (String address) throws APIException, IOException {
        List<UnspentOutput> outputs = new ArrayList<UnspentOutput>();

        Map<String, String> params = new HashMap<String, String>();
        params.put("active", address);
        if (apiCode != null) {
            params.put("api_code", apiCode);
        }

        String response = null;
        try {
            response = HttpClient.getInstance().get("unspent", params);
        } catch (APIException e) {
            // the API isn't supposed to return an error code here. No free outputs is
            // a legitimate situation. We are circumventing that by returning an empty list
            if (e.getMessage().equals("No free outputs to spend")) {
                return outputs;
            } else {
                throw e;
            }
        }

        JsonObject outsJson = new JsonParser().parse(response).getAsJsonObject();

        for (JsonElement outElem : outsJson.get("unspent_outputs").getAsJsonArray()) {
            outputs.add(new UnspentOutput(outElem.getAsJsonObject()));
        }

        return outputs;
    }

    /**
     * Gets the latest block on the main chain (simplified representation).
     *
     * @return An instance of the {@link LatestBlock} class
     * @throws APIException If the server returns an error
     */
    public LatestBlock getLatestBlock () throws APIException, IOException {
        String response = HttpClient.getInstance().get("latestblock?api_code=" + apiCode, null);
        JsonObject blockObj = new JsonParser().parse(response).getAsJsonObject();
        return new LatestBlock(blockObj);
    }

    /**
     * Gets a list of currently unconfirmed transactions.
     *
     * @return A list of unconfirmed {@link Transaction} objects
     * @throws APIException If the server returns an error
     */
    public List<Transaction> getUnconfirmedTransactions () throws APIException, IOException {
        List<Transaction> transactions = new ArrayList<Transaction>();

        Map<String, String> params = new HashMap<String, String>();
        params.put("format", "json");
        if (apiCode != null) {
            params.put("api_code", apiCode);
        }

        String response = HttpClient.getInstance().get("unconfirmed-transactions", params);
        JsonObject txList = new JsonParser().parse(response).getAsJsonObject();

        for (JsonElement txElem : txList.get("txs").getAsJsonArray()) {
            JsonObject txObj = txElem.getAsJsonObject();
            transactions.add(new Transaction(txObj, -1, txObj.get("double_spend").getAsBoolean()));
        }

        return transactions;
    }

    /**
     * Gets a list of blocks mined today by all pools since 00:00 UTC.
     *
     * @return A list of {@link SimpleBlock} objects
     * @throws APIException APIException If the server returns an error
     */
    public List<SimpleBlock> getBlocks () throws APIException, IOException {
        return getBlocks(null);
    }

    /**
     * Gets a list of blocks mined on a specific day.
     *
     * @param timestamp Unix timestamp (without milliseconds) that falls between
     *                  00:00 UTC and 23:59 UTC of the desired day.
     * @return A list of {@link SimpleBlock} objects
     */
    public List<SimpleBlock> getBlocks (long timestamp) throws APIException, IOException {
        return getBlocks(String.valueOf(timestamp * 1000));
    }

    /**
     * Gets a list of recent blocks by a specific mining pool.
     *
     * @param poolName Name of the mining pool
     * @return A list of {@link SimpleBlock} objects
     * @throws APIException If the server returns an error
     */
    public List<SimpleBlock> getBlocks (String poolName) throws APIException, IOException {
        List<SimpleBlock> blocks = new ArrayList<SimpleBlock>();
        poolName = poolName == null ? "" : poolName;

        Map<String, String> params = new HashMap<String, String>();
        params.put("format", "json");
        if (apiCode != null) {
            params.put("api_code", apiCode);
        }

        String response = HttpClient.getInstance().get("blocks/" + poolName, params);
        JsonObject blockList = new JsonParser().parse(response).getAsJsonObject();

        for (JsonElement blockElem : blockList.get("blocks").getAsJsonArray()) {
            blocks.add(new SimpleBlock(blockElem.getAsJsonObject()));
        }

        return blocks;
    }

    /**
     * Gets inventory data for an object.
     *
     * @param hash Object hash
     * @return An instance of the {@link InventoryData} class
     * @throws APIException If the server returns an error
     */
    public InventoryData getInventoryData (String hash) throws APIException, IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("format", "json");
        if (apiCode != null) {
            params.put("api_code", apiCode);
        }

        String response = HttpClient.getInstance().get("inv/" + hash, params);
        JsonObject invObj = new JsonParser().parse(response).getAsJsonObject();
        return new InventoryData(invObj);
    }
}
