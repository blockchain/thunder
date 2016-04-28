package network.thunder.core.helper.blockchain.bciapi;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is a utility class for performing API calls using GET and POST requests. It is
 * possible to override these calls by implementing HttpClientInterface. Add the new
 * implementation via setCustomHttpClient(...), such that it will get used globally.
 */
public class HttpClient implements HttpClientInterface {
    private static final String BASE_URL = "https://blockchain.info/";

    public volatile static int TIMEOUT_MS = 10000;

    private static HttpClientInterface instance;

    public static HttpClientInterface getInstance () {
        if (instance == null) {
            instance = new HttpClient();
        }
        return instance;
    }

    public static void setCustomHttpClient (HttpClientInterface httpClient) {
        instance = httpClient;
    }

    /**
     * Perform a GET request on a Blockchain.info API resource.
     *
     * @param resource Resource path after https://blockchain.info/api/
     * @param params   Map containing request parameters
     * @return String response
     * @throws APIException If the server returns an error
     */
    public String get (String resource, Map<String, String> params) throws APIException, IOException {
        return openURL(resource, params, "GET");
    }

    /**
     * Perform a POST request on a Blockchain.info API resource.
     *
     * @param resource Resource path after https://blockchain.info/api/
     * @param params   Map containing request parameters
     * @return String response
     * @throws APIException If the server returns an error
     */
    public String post (String resource, Map<String, String> params) throws APIException, IOException {
        return openURL(resource, params, "POST");
    }

    private static String openURL (String resource, Map<String, String> params, String requestMethod) throws APIException, IOException {
        String encodedParams = urlEncodeParams(params);
        URL url = null;
        APIException apiException = null;
        IOException ioException = null;

        String responseStr = null;

        if (requestMethod.equals("GET")) {
            if (encodedParams.isEmpty()) {
                url = new URL(BASE_URL + resource);
            } else {
                url = new URL(BASE_URL + resource + '?' + encodedParams);
            }
        } else if (requestMethod.equals("POST")) {
            url = new URL(BASE_URL + resource);
        }

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(requestMethod);
            conn.setConnectTimeout(TIMEOUT_MS);

            if (requestMethod.equals("POST")) {
                byte[] postBytes = encodedParams.getBytes("UTF-8");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Content-Length", String.valueOf(postBytes.length));
                conn.getOutputStream().write(postBytes);
                conn.getOutputStream().close();
            }

            if (conn.getResponseCode() != 200) {
                apiException = new APIException(inputStreamToString(conn.getErrorStream()));
            } else {
                responseStr = inputStreamToString(conn.getInputStream());
            }
        } catch (IOException e) {
            ioException = e;
        } finally {
            try {
                if (apiException != null) {
                    conn.getErrorStream().close();
                }
                conn.getInputStream().close();
            } catch (Exception ex) {
            }

            if (ioException != null) {
                throw ioException;
            }

            if (apiException != null) {
                throw apiException;
            }
        }

        return responseStr;
    }

    private static String urlEncodeParams (Map<String, String> params) {
        String result = "";

        if (params != null && params.size() > 0) {
            try {
                StringBuilder data = new StringBuilder();
                for (Entry<String, String> kvp : params.entrySet()) {
                    if (data.length() > 0) {
                        data.append('&');
                    }

                    data.append(URLEncoder.encode(kvp.getKey(), "UTF-8"));
                    data.append('=');
                    data.append(URLEncoder.encode(kvp.getValue(), "UTF-8"));
                }
                result = data.toString();
            } catch (UnsupportedEncodingException e) {
            }
        }

        return result;
    }

    private static String inputStreamToString (InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        StringBuilder responseStringBuilder = new StringBuilder();
        String line = "";

        while ((line = reader.readLine()) != null) {
            responseStringBuilder.append(line);
        }

        return responseStringBuilder.toString();
    }
}
