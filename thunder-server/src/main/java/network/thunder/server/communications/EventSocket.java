package network.thunder.server.communications;

import com.google.gson.Gson;
import network.thunder.server.database.MySQLConnection;
import network.thunder.server.etc.Tools;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.sql.Connection;


public class EventSocket extends WebSocketAdapter
{
    private Session session;
    private int channelId;

    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        session = sess;
        System.out.println("Socket Connected: " + sess);
    }

    @Override
    public void onWebSocketText(String data)
    {
        /**
         * TODO: Do some more checking on the messag we actually received.
         */
        super.onWebSocketText(data);

        Message message;
        try {
            message = new Gson().fromJson(data, Message.class);
            message.prepare(null);
            Connection conn = WebSocketHandler.dataSource.getConnection();
            channelId = MySQLConnection.getChannel(conn, message.pubkey).getId();
            WebSocketHandler.websocketList.put(channelId, session);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Received TEXT message: " + data);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        WebSocketHandler.websocketList.remove(channelId);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}
