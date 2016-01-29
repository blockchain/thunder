package network.thunder.core.communication.nio.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.MessageExecutorImpl;
import network.thunder.core.communication.objects.messages.interfaces.message.FailureMessage;
import network.thunder.core.communication.processor.Processor;

/**
 * Created by matsjerratsch on 07/12/2015.
 */
public class ProcessorHandler extends ChannelDuplexHandler {

    Processor processor;
    String layerName = "";

    public ProcessorHandler (Processor processor, String layerName) {
        this.processor = processor;
        this.layerName = layerName;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) {
        try {
            System.out.println("CHANNEL ACTIVE " + layerName);
            processor.onLayerActive(new MessageExecutorImpl(ctx, layerName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            checkIfMessage(msg);
            if (msg instanceof FailureMessage) {
                System.out.println("In Failure: " + ((FailureMessage) msg).getFailure());
            } else {
                System.out.println(layerName + " I: " + msg);
                Message message = (Message) msg;
                message.verify();
                processor.onInboundMessage(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        promise.setSuccess();
        if (msg instanceof FailureMessage) {
            System.out.println("Out Failure: " + ((FailureMessage) msg).getFailure());
        }
        try {
            checkIfMessage(msg);
            Message message = (Message) msg;
            message.verify();
            processor.onOutboundMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelUnregistered (ChannelHandlerContext ctx) throws Exception {
        processor.onLayerClose();
        super.channelUnregistered(ctx);
    }

    public void checkIfMessage (Object msg) {
        if (msg instanceof Message) {
            return;
        } else {
            throw new RuntimeException("Received a wrong type? " + msg);
        }
    }
}
