package network.thunder.server.communications;

import network.thunder.server.communications.objects.WebSocketNewSecret;
import network.thunder.server.communications.objects.WebSocketUpdatePayment;
import network.thunder.server.database.objects.Payment;
import org.eclipse.jetty.websocket.api.Session;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by PC on 17.08.2015.
 */
public class WebSocketHandler {
	public static HashMap<Integer, Session> websocketList = new HashMap<>();
	public static DataSource dataSource;

	public static Class<?> getEventSocket () {
		return EventSocket.class;
	}

	public static void init (DataSource ds) {
		dataSource = ds;
	}

	public static void newSecret (int channelIdReceiver) {
		WebSocketNewSecret request = new WebSocketNewSecret();

		Session session = websocketList.get(channelIdReceiver);
		/**
		 * Check if there is a client listening for updates on this channel..
		 */
		if (session == null) {
			return;
		}
		Message message = new Message();
		message.type = Type.WEBSOCKET_NEW_SECRET;
		message.success = true;

		try {
			session.getRemote().sendString(message.getDataString());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void sendPayment (int channelIdReceiver, Payment payment) {
		WebSocketUpdatePayment request = new WebSocketUpdatePayment();
		request.amount = payment.getAmount();
		request.hash = payment.getSecretHash();

		Session session = websocketList.get(channelIdReceiver);
		/**
		 * Check if there is a client listening for updates on this channel..
		 */
		if (session == null) {
			return;
		}
		Message message = new Message();
		message.type = Type.WEBSOCKET_NEW_PAYMENT;
		message.success = true;

		try {
			session.getRemote().sendString(message.getDataString());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
