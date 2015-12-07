package network.thunder.core.communication.objects.messages.impl;

import io.netty.channel.ChannelHandlerContext;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class MessageExecutorImpl implements MessageExecutor {
    ChannelHandlerContext context;

    public MessageExecutorImpl (ChannelHandlerContext context) {
        this.context = context;
    }

    public void setContext (ChannelHandlerContext context) {
        this.context = context;
    }

    @Override
    public void sendNextLayerActive () {
        context.fireChannelActive();
    }

    @Override
    public void sendMessageUpwards (Message message) {
        System.out.println("O: " + message);
        context.writeAndFlush(message);
    }

    @Override
    public void sendMessageDownwards (Message message) {
//        System.out.println("I: " + message);
        context.fireChannelRead(message);
    }

    @Override
    public void closeConnection () {
        context.close();
    }
}
