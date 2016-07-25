package wallettemplate;

import network.thunder.core.etc.Tools;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

public class PaymentProtocolServerSocket {
    private static final Logger log = Tools.getLogger();

    public static void init () {

        new Thread(new Runnable() {
            @Override
            public void run () {

                int portNumber = 15462;

                try {

                    ServerSocket serverSocket = null;

                    serverSocket = new ServerSocket(portNumber);

                    while (true) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                            String request = in.readLine();
                            log.info("Payment Request received: " + request);

                            request = request.substring(8);

                            List<NameValuePair> params = URLEncodedUtils.parse(request, Charset.defaultCharset());
                            HashMap<String, String> list = new HashMap<String, String>();
                            for (NameValuePair param : params) {
                                log.info(param.getName() + "  " + param.getValue());
                                list.put(param.getName(), param.getValue());
                            }

                            try {
//                                ThunderContext.instance.makePayment(Long.valueOf(list.get("amount")), list.get("address"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
