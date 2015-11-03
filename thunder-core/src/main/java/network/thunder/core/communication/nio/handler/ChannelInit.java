package network.thunder.core.communication.nio.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.nio.handler.low.ByteToMessageObjectHandler;
import network.thunder.core.communication.nio.handler.low.EncryptionHandler;
import network.thunder.core.communication.nio.handler.low.MessageObjectToByteHandler;
import network.thunder.core.communication.nio.handler.low.NodeConnectionHandler;
import network.thunder.core.communication.nio.handler.mid.AuthenticationHandler;
import network.thunder.core.communication.nio.handler.mid.SyncHandler;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class ChannelInit extends ChannelInitializer<SocketChannel> {
    boolean isServer;
    Node node;
    P2PContext context;

    public ChannelInit (P2PContext context, boolean isServer) {
        this.isServer = isServer;
        this.context = context;
    }

    public ChannelInit (P2PContext context, boolean isServer, Node node) {
        this.isServer = isServer;
        this.node = node;
        this.context = context;
    }

    @Override
    protected void initChannel (SocketChannel ch) throws Exception {
        if (isServer) {
            node = new Node();
        }

//		ch.pipeline().addLast(new DumpHexHandler());

//		ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        ch.pipeline().addLast(new NodeConnectionHandler(context, node));

        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2147483647, 0, 4, 0, 4));
        ch.pipeline().addLast(new LengthFieldPrepender(4));

        ch.pipeline().addLast("EncryptionHandler", new EncryptionHandler(isServer, node));

        ch.pipeline().addLast(new ByteToMessageObjectHandler());
        ch.pipeline().addLast(new MessageObjectToByteHandler());

        ch.pipeline().addLast("AuthenticationHandler", new AuthenticationHandler(context.nodeKey, isServer, node));

        ch.pipeline().addLast(new SyncHandler(isServer, node, context));

    }
}
