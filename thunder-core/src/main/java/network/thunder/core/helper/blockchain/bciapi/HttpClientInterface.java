package network.thunder.core.helper.blockchain.bciapi;

import java.io.IOException;
import java.util.Map;

/**
 * This is a utility class for performing API calls using GET and POST requests. It is
 * possible to override these calls with your own implementation by overriding the 'get'
 * and 'post' methods.
 */
public interface HttpClientInterface {

    String get (String resource, Map<String, String> params) throws APIException, IOException;

    String post (String resource, Map<String, String> params) throws APIException, IOException;

}
