/*
 *  ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 *  Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package network.thunder.client.communications;

import com.google.gson.Gson;
import network.thunder.client.etc.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class HTTPS {

    public HttpURLConnection con;
    public HttpResponse httpResponse;
    HttpClient httpClient;
    //	HttpURLConnection urlConnection;
    HttpPost httpPost;
    HttpGet httpGet;
    List<NameValuePair> nvps;

    boolean error = false;

    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

    public static String postToApi (Object data) throws ClientProtocolException, IOException {
        HTTPS connectionOne = new HTTPS();
        connectionOne.connectPOST("http://" + Constants.SERVER_URL + "/api/");
        //    	connectionOne.connectPOST("http://localhost/api/");
        String d = new Gson().toJson(data);
        //    	System.out.println("Request Size: "+d.length());
        connectionOne.addPOSTParameter("data", d);
        connectionOne.submitPOST();
        return connectionOne.getContent();
    }

    public void addPOSTParameter (String parameter, String value) {
        nvps.add(new BasicNameValuePair(parameter, value));
    }

    public boolean connect (String URL) {
        try {

            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10 * 1000).build();
            httpClient = HttpClients.createDefault();

            httpGet = new HttpGet(URL);
            httpResponse = httpClient.execute(httpGet);

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean connectPOST (String URL) {
        try {

            RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10 * 1000).build();
            httpClient = HttpClients.createDefault();
            httpPost = new HttpPost(URL);
            //			httpPost.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            //			httpPost.setHeader("Accept-Encoding", "gzip, deflate");
            //		    httpPost.setHeader("Content-Type", "application/json");
            //		    httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");

            nvps = new ArrayList<NameValuePair>();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getContent () throws UnsupportedOperationException, IOException {
        if (httpResponse != null && !error) {

            HttpEntity entity = httpResponse.getEntity();

            BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));

            String input;
            String ausgabe = "";

            while ((input = br.readLine()) != null) {
                ausgabe += input + "\n";
            }
            br.close();

            return ausgabe;

        }
        return null;

    }

    private String getQuery (List<NameValuePair> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public void submitPOST () throws ClientProtocolException, IOException {
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        httpResponse = httpClient.execute(httpPost);
    }

}
