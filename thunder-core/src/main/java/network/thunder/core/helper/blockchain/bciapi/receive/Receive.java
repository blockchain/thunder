package network.thunder.core.helper.blockchain.bciapi.receive;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import network.thunder.core.helper.blockchain.bciapi.APIException;
import network.thunder.core.helper.blockchain.bciapi.HttpClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class reflects the functionality documented at
 * https://blockchain.info/api/api_receive. It allows merchants to create forwarding
 * addresses and be notified upon payment.
 */
public class Receive {
    /**
     * Calls the 'api/receive' endpoint and creates a forwarding address.
     *
     * @param receivingAddress Destination address where the payment should be sent
     * @param callbackUrl      Callback URI that will be called upon payment
     * @return An instance of the ReceiveResponse class
     * @throws APIException If the server returns an error
     */
    public static ReceiveResponse receive (String receivingAddress, String callbackUrl) throws APIException, IOException {
        return receive(receivingAddress, callbackUrl, null);
    }

    /**
     * Calls the 'api/receive' endpoint and creates a forwarding address.
     *
     * @param receivingAddress Destination address where the payment should be sent
     * @param callbackUrl      Callback URI that will be called upon payment
     * @param apiCode          Blockchain.info API code (optional, nullable)
     * @return An instance of the ReceiveResponse class
     * @throws APIException If the server returns an error
     */
    public static ReceiveResponse receive (String receivingAddress, String callbackUrl, String apiCode) throws APIException, IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("address", receivingAddress);
        params.put("callback", callbackUrl);
        params.put("method", "create");

        if (apiCode != null) {
            params.put("api_code", apiCode);
        }

        String response = HttpClient.getInstance().post("api/receive", params);
        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(response).getAsJsonObject();

        return new ReceiveResponse(obj.get("fee_percent").getAsInt(), obj.get("destination").getAsString(), obj.get("input_address").getAsString(), obj.get
                ("callback_url").getAsString());
    }
}
