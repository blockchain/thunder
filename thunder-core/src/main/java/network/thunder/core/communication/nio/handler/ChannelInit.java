package network.thunder.core.communication.nio.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.nio.handler.low.ByteToMessageObjectHandler;
import network.thunder.core.communication.nio.handler.low.MessageObjectToByteHandler;
import network.thunder.core.communication.nio.handler.low.NodeConnectionHandler;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;
import network.thunder.core.communication.processor.interfaces.AuthenticationProcessor;
import network.thunder.core.communication.processor.interfaces.EncryptionProcessor;
import network.thunder.core.communication.processor.interfaces.PeerSeedProcessor;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class ChannelInit extends ChannelInitializer<SocketChannel> {
    ContextFactory contextFactory;
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
        node.isServer = isServer;

//        ch.pipeline().addLast(new DumpHexHandler());

//        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        ch.pipeline().addLast(new NodeConnectionHandler(context, node));

        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2147483647, 0, 4, 0, 4));
        ch.pipeline().addLast(new LengthFieldPrepender(4));

        MessageSerializer messageSerializer = contextFactory.getMessageSerializer();
        ch.pipeline().addLast(new ByteToMessageObjectHandler(messageSerializer));
        ch.pipeline().addLast(new MessageObjectToByteHandler(messageSerializer));

        EncryptionProcessor encryptionProcessor = contextFactory.getEncryptionProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(encryptionProcessor, "Encryption"));

        AuthenticationProcessor authenticationProcessor = contextFactory.getAuthenticationProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(authenticationProcessor, "Authentication"));

        PeerSeedProcessor peerSeedProcessor = contextFactory.getPeerSeedProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(peerSeedProcessor, "PeerSeed"));


//        ch.pipeline().addLast(new SyncHandler(isServer, node, context));

    }
}
