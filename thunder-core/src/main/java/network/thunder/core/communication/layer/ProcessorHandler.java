package network.thunder.core.communication.layer;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import network.thunder.core.communication.processor.exceptions.LNException;

/**
 * Created by matsjerratsch on 07/12/2015.
 */
public class ProcessorHandler extends ChannelDuplexHandler {

    Processor processor;
    String layerName = "";
    MessageExecutor messageExecutor;
    boolean activated = false;

    public ProcessorHandler (Processor processor, String layerName) {
        this.processor = processor;
        this.layerName = layerName;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) {
        activated = true;
        try {
            messageExecutor = new MessageExecutorImpl(ctx, layerName);
            processor.onLayerActive(messageExecutor);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            checkIfMessage(msg);
            if (msg instanceof FailureMessage) {
                System.out.println("In Failure: " + ((FailureMessage) msg).getFailure());
            } else {

                Message message = (Message) msg;
                message.verify();

                if (processor.consumesInboundMessage(message)) {
                    processor.onInboundMessage(message);
                } else {
                    messageExecutor.sendMessageDownwards(message);
                }
            }
        } catch (LNException e1) {
            if (e1.shouldDisconnect()) {
                ctx.close();
            }
            throw e1;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
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

            if (processor.consumesOutboundMessage(msg)) {
                processor.onOutboundMessage(message);
            } else {
                messageExecutor.sendMessageUpwards(message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void channelUnregistered (ChannelHandlerContext ctx) throws Exception {
        if (activated) {
            processor.onLayerClose();
        }
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
