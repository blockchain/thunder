package network.thunder.core.communication.nio;

import com.google.gson.Gson;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.Node;
import network.thunder.core.communication.Type;
import network.thunder.core.communication.objects.subobjects.AuthenticationObject;
import network.thunder.core.etc.Tools;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

public class EchoWorker implements Runnable {
	private final List<ServerDataEvent> queue = new LinkedList();

	public void processData (NioServer server, SocketChannel socket, byte[] data, int count) {
		byte[] dataCopy = new byte[count];
		System.arraycopy(data, 0, dataCopy, 0, count);
		synchronized (queue) {
			queue.add(new ServerDataEvent(server, socket, dataCopy));
			queue.notify();
		}
	}

	public void run () {
		ServerDataEvent dataEvent;

		while (true) {
			// Wait for data to become available
			synchronized (queue) {
				while (queue.isEmpty()) {
					try {
						queue.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				dataEvent = (ServerDataEvent) queue.remove(0);
			}

			//This is a new thread in which we will later process the data..
			//TODO: From here we should call to the database, relay data and do further requests

			//We get all the data as byte[] array
			//This will get seriaziled back to a JSON string, which in turn will then be
			//turned into the object. While this is probably very ineffective, it is great for
			//debugging and we can replace it anytime we want...
			Gson gson = new Gson();
			Message message = gson.fromJson(Tools.byteToString(dataEvent.data), Message.class);

			//The node that sent us this request
			Node node = dataEvent.node;

			//Data we want to send back
			Message returnMessage = new Message();

			if (message.type == Type.AUTH_SEND) {
				AuthenticationObject authenticationObject = gson.fromJson(message.data, AuthenticationObject.class);
				if (node.allowsAuth() && node.processAuthentication(authenticationObject)) {
					//Authentication successful
					if (!node.hasSentAuth()) {
						returnMessage = new Message(dataEvent.node.getAuthenticationObject(), Type.AUTH_SEND, null);

					} else {
						returnMessage = new Message(null, Type.AUTH_ACCEPT, null);
					}

				} else {
					//TODO: Authentication not successful..
					returnMessage = new Message(null, Type.AUTH_FAILED, null);
				}
			} else if (message.type == Type.AUTH_ACCEPT) {
				if (node.hasSentAuth() && node.isAuth()) {
					node.finishAuth();
					return;
				}
			}

			//From here on, all operations are only allowed for authed nodes..
			if (!message.hasData() && !node.isAuth()) {
				//TODO: All the lightning stuff here...
			} else {
				returnMessage = new Message(null, Type.FAILURE, null);
			}

			// Return to sender
			if (message.hasData()) {
				byte[] data = Tools.messageToBytes(message);
				dataEvent.server.send(dataEvent.socket, dataEvent.data);
			}
		}
	}
}