package network.thunder.core.communication.layer;

import io.netty.channel.ChannelHandlerContext;

public class MessageExecutorImpl implements MessageExecutor {
    ChannelHandlerContext context;
    String layerName;

    public MessageExecutorImpl (ChannelHandlerContext context) {
        this(context, "");
    }

    public MessageExecutorImpl (ChannelHandlerContext context, String layerName) {
        this.context = context;
        this.layerName = layerName;
    }

    @Override
    public void sendNextLayerActive () {
        context.fireChannelActive();
    }

    @Override
    public void sendMessageUpwards (Message message) {
        context.writeAndFlush(message);
    }

    @Override
    public void sendMessageDownwards (Message message) {
        context.fireChannelRead(message);
    }

    @Override
    public void closeConnection () {
        context.close();
    }
}
