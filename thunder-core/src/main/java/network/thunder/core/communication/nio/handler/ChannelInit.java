package network.thunder.core.communication.nio.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import network.thunder.core.communication.nio.handler.low.ByteToMessageObjectHandler;
import network.thunder.core.communication.nio.handler.low.MessageObjectToByteHandler;
import network.thunder.core.communication.nio.handler.low.PingHandler;
import network.thunder.core.communication.objects.messages.interfaces.factories.ContextFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;
import network.thunder.core.communication.processor.Processor;
import network.thunder.core.mesh.NodeClient;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class ChannelInit extends ChannelInitializer<SocketChannel> {
    ContextFactory contextFactory;
    NodeClient node;
    boolean serverMode = false;

    public ChannelInit (ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
        serverMode = true;
    }

    public ChannelInit (ContextFactory contextFactory, NodeClient node) {
        this.contextFactory = contextFactory;
        this.node = node;
    }

    @Override
    protected void initChannel (SocketChannel ch) throws Exception {

        if (serverMode) {
            node = new NodeClient();
            node.isServer = true;
        }
//        ch.pipeline().addLast(new DumpHexHandler());

//        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
//        ch.pipeline().addLast(new NodeConnectionHandler(context, node));

        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2147483647, 0, 4, 0, 4));
        ch.pipeline().addLast(new LengthFieldPrepender(4));

        MessageSerializer messageSerializer = contextFactory.getMessageSerializer();
        ch.pipeline().addLast(new ByteToMessageObjectHandler(messageSerializer));
        ch.pipeline().addLast(new MessageObjectToByteHandler(messageSerializer));

        ChannelHandler pingHandler = new PingHandler();
        ch.pipeline().addLast(pingHandler);

        Processor encryptionProcessor = contextFactory.getEncryptionProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(encryptionProcessor, "Encryption"));

        Processor authenticationProcessor = contextFactory.getAuthenticationProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(authenticationProcessor, "Authentication"));

        Processor gossipProcessor = contextFactory.getGossipProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(gossipProcessor, "Gossip"));

        Processor peerSeedProcessor = contextFactory.getPeerSeedProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(peerSeedProcessor, "PeerSeed"));

        Processor syncProcessor = contextFactory.getSyncProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(syncProcessor, "Sync"));

        Processor lnEstablishProcessor = contextFactory.getLNEstablishProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(lnEstablishProcessor, "LNEstablish"));

        Processor lnPaymentProcessor = contextFactory.getLNPaymentProcessor(node);
        ch.pipeline().addLast(new ProcessorHandler(lnPaymentProcessor, "LNPayment"));

    }
}
