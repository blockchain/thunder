package network.thunder.core.communication.objects.messages.impl;

import io.netty.channel.ChannelHandlerContext;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
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
