package network.thunder.core.helper.blockchain.bciapi;

import com.squareup.okhttp.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the {@link HttpClientInterface} using OkHttp.
 * OkHttp handles Gzip, connection pooling, caching and retries out of the box.
 * The library also provides a set of fluent builders for creating requests and responses.
 * For more information about OkHttp visit http://square.github.io/okhttp/
 */
public class OkClient implements HttpClientInterface {

    public static final String URL_SCHEME = "https";
    public static final String URL_HOST = "blockchain.info";
    public static final int TIMEOUT_MS = 10000;

    private static OkHttpClient okHttpClientInstance = null;
    private static OkClient okClientInstance = null;

    /**
     * Private constructor to prevent instantiation.
     */
    private OkClient () {
        //prevent instantiation through reflection.
        if (okClientInstance != null) {
            throw new IllegalStateException("Already initialized.");
        }
    }

    /**
     * OkClient singleton
     */
    public synchronized static OkClient getOkClientInstance () {
        if (okClientInstance == null) {
            okClientInstance = new OkClient();
        }
        return okClientInstance;
    }

    /**
     * OkHttpClient singleton
     */
    private synchronized static OkHttpClient getOkHttpClientInstance () {
        if (okHttpClientInstance == null) {
            okHttpClientInstance = new OkHttpClient();
            okHttpClientInstance.setConnectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        }
        return okHttpClientInstance;
    }

    /**
     * This method makes a get request to a specific API resource.
     * Request is built using the OkHttp {@link HttpUrl} builder.
     *
     * @param resource The API resource being requested.
     * @param params   A set of params sent to the resource
     * @return Returns a response as a {@link String}
     * @throws IOException Thrown if the request was unsuccessful.
     */
    @Override
    public String get (String resource, Map<String, String> params) throws APIException, IOException {

        HttpUrl.Builder url = getHttpUrlBuilder(resource);

        for (String paramName : params.keySet()) {
            url.addEncodedQueryParameter(paramName, params.get(paramName));
        }

        Request request = new Request.Builder()
                .url(url.build())
                .build();

        Response response = getOkHttpClientInstance().newCall(request).execute();
        if (isNotSuccessfulResponse(response)) {
            throw new IOException(String.format("Unsuccessful call to %s Response: %s",
                    request.urlString(),
                    response));
        }
        return response.body().string();
    }

    /**
     * This method makes a post request to a specific API resource.
     * Request is built using the OkHttp {@link HttpUrl} builder.
     * Request body is built using the OkHttp {@link FormEncodingBuilder} builder .
     *
     * @param resource The API resource being requested.
     * @param params   A set of params sent to the resource
     * @return Returns a response as a {@link String}
     * @throws IOException Thrown if the request was unsuccessful.
     */
    @Override
    public String post (String resource, Map<String, String> params) throws APIException, IOException {

        HttpUrl.Builder url = getHttpUrlBuilder(resource);

        FormEncodingBuilder formEncodingBuilder = new FormEncodingBuilder();
        for (String paramName : params.keySet()) {
            formEncodingBuilder.addEncoded(paramName, params.get(paramName));
        }

        Request request = new Request.Builder()
                .url(url.build())
                .post(formEncodingBuilder.build())
                .build();

        Response response = getOkHttpClientInstance().newCall(request).execute();
        if (isNotSuccessfulResponse(response)) {
            throw new IOException(String.format("Unsuccessful call to %s Response: %s",
                    request.urlString(),
                    response));
        }

        return response.body().string();

    }

    private HttpUrl.Builder getHttpUrlBuilder (String resource) {
        HttpUrl.Builder url = new HttpUrl.Builder();
        url.scheme(URL_SCHEME)
                .host(URL_HOST)
                .addPathSegment(resource);
        return url;
    }

    private boolean isNotSuccessfulResponse (Response response) {
        return !response.isSuccessful();
    }
}
