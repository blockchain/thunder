/*
 * ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 * Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package network.thunder.core.communication;

import com.google.gson.Gson;
import network.thunder.server.database.MySQLConnection;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.sql.Connection;

public class EventSocket extends WebSocketAdapter {
	private Session session;
	private int channelId;

	@Override
	public void onWebSocketClose (int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		WebSocketHandler.websocketList.remove(channelId);
		System.out.println("Socket Closed: [" + statusCode + "] " + reason);
	}

	@Override
	public void onWebSocketConnect (Session sess) {
		super.onWebSocketConnect(sess);
		session = sess;
		System.out.println("Socket Connected: " + sess);
	}

	@Override
	public void onWebSocketError (Throwable cause) {
		super.onWebSocketError(cause);
		cause.printStackTrace(System.err);
	}

	@Override
	public void onWebSocketText (String data) {
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
}
