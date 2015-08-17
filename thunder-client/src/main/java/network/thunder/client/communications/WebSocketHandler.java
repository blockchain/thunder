package network.thunder.client.communications;

import com.google.gson.Gson;
import network.thunder.client.api.ThunderContext;
import network.thunder.client.communications.objects.WebSocketAddListener;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.Tools;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

/**
 * Created by PC on 17.08.2015.
 */
public class WebSocketHandler {

    static HashMap<Integer, Session> sessionHashMap = new HashMap<>();


    public static void connectToServer(Channel channel) {
        WebSocketClient client = new WebSocketClient();
        URI uri = URI.create("ws://"+ Constants.SERVER_URL +"/websocket");
        System.out.println(uri);
        try
        {
            client.start();
            EventSocket socket = new EventSocket();
            socket.channel = channel;
            Future<Session> fut = client.connect(socket, uri);
            Session session = fut.get();

            Message message = new Message(new WebSocketAddListener(), Type.WEBSOCKET_OPEN, channel.getClientKeyOnClient());

            session.getRemote().sendString(new Gson().toJson(message));
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
//                        System.out.println("Ping");
                        session.getRemote().sendPing(null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 30000);


            sessionHashMap.put(channel.getId(), session);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class EventSocket extends WebSocketAdapter {
        public Channel channel;

        @Override
        public void onWebSocketConnect(Session sess) {
            super.onWebSocketConnect(sess);
            System.out.println("Socket Connected: " + sess);
        }

        @Override
        public void onWebSocketText(String message) {
            /**
             * TODO: Do some more checking on the messag we actually received.
             */
            super.onWebSocketText(message);
            System.out.println("Received TEXT message: " + message);
            try {
                ThunderContext.updateChannel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            super.onWebSocketClose(statusCode, reason);
            System.out.println("Socket Closed: [" + statusCode + "] " + reason);
            connectToServer(channel);

        }

        @Override
        public void onWebSocketError(Throwable cause) {
            super.onWebSocketError(cause);
            cause.printStackTrace(System.err);
        }
    }
}
