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
import network.thunder.core.communication.objects.messages.impl.MessageEncrypterImpl;
import network.thunder.core.communication.objects.messages.impl.MessageSerializerImpl;
import network.thunder.core.communication.objects.messages.impl.factories.AuthenticationMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.factories.EncryptionMessageFactoryImpl;
import network.thunder.core.communication.objects.messages.interfaces.factories.EncryptionMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageEncrypter;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;
import network.thunder.core.communication.processor.implementations.AuthenticationProcessorImpl;
import network.thunder.core.communication.processor.implementations.EncryptionProcessorImpl;
import network.thunder.core.communication.processor.interfaces.AuthenticationProcessor;
import network.thunder.core.communication.processor.interfaces.EncryptionProcessor;
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
        node.isServer = isServer;

//        ch.pipeline().addLast(new DumpHexHandler());

//        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        ch.pipeline().addLast(new NodeConnectionHandler(context, node));

        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2147483647, 0, 4, 0, 4));
        ch.pipeline().addLast(new LengthFieldPrepender(4));

        MessageSerializer messageSerializer = new MessageSerializerImpl();
        ch.pipeline().addLast(new ByteToMessageObjectHandler(messageSerializer));
        ch.pipeline().addLast(new MessageObjectToByteHandler(messageSerializer));

        MessageEncrypter messageEncrypter = new MessageEncrypterImpl(messageSerializer);
        EncryptionMessageFactory encryptionMessageFactory = new EncryptionMessageFactoryImpl();
        EncryptionProcessor encryptionProcessor = new EncryptionProcessorImpl(encryptionMessageFactory, messageEncrypter, node);
        ch.pipeline().addLast(new EncryptionHandler(encryptionProcessor));

        AuthenticationProcessor authenticationProcessor = new AuthenticationProcessorImpl(new AuthenticationMessageFactoryImpl(), node);
        ch.pipeline().addLast(new AuthenticationHandler(authenticationProcessor));

//        ch.pipeline().addLast(new SyncHandler(isServer, node, context));

    }
}
