package network.thunder.core.communication.nio.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import network.thunder.core.communication.Node;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.nio.handler.low.ByteToMessageObjectHandler;
import network.thunder.core.communication.nio.handler.low.EncryptionHandler;
import network.thunder.core.communication.nio.handler.low.MessageObjectToByteHandler;
import network.thunder.core.communication.nio.handler.mid.AuthenticationHandler;
import network.thunder.core.communication.nio.handler.mid.GossipHandler;
import org.bitcoinj.core.ECKey;

import java.util.ArrayList;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class ChannelInit extends ChannelInitializer<SocketChannel> {
	ArrayList<Node> connectedNodes;
	boolean isServer;
	ECKey key;
	Node node;
	P2PContext context;

	public ChannelInit (P2PContext context, boolean isServer, ArrayList<Node> connectedNodes, ECKey key) {
		this.isServer = isServer;
		this.connectedNodes = connectedNodes;
		this.key = key;
		this.context = context;
	}

	public ChannelInit (P2PContext context, boolean isServer, Node node, ECKey key) {
		this.isServer = isServer;
		this.key = key;
		this.node = node;
		this.context = context;
	}

	@Override
	protected void initChannel (SocketChannel ch) throws Exception {
		if (isServer) {
			node = new Node();
			connectedNodes.add(node);
		}

//		ch.pipeline().addLast(new DumpHexHandler());

//		ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
		ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2147483647, 0, 4, 0, 4));
		ch.pipeline().addLast(new LengthFieldPrepender(4));

		ch.pipeline().addLast("EncryptionHandler", new EncryptionHandler(isServer, node));

		ch.pipeline().addLast(new ByteToMessageObjectHandler());
		ch.pipeline().addLast(new MessageObjectToByteHandler());

		ch.pipeline().addLast("AuthenticationHandler", new AuthenticationHandler(key, isServer, node));

		ch.pipeline().addLast(new GossipHandler(isServer, node, context));
	}
}
