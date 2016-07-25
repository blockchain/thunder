package network.thunder.core.communication.layer;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import network.thunder.core.etc.Tools;
import org.slf4j.Logger;

public class ProcessorHandler extends ChannelDuplexHandler {
    private static final Logger log = Tools.getLogger();

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
        if (activated) {
            return;
        }
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
                log.error("In Failure: " + ((FailureMessage) msg).getFailure());
            } else {

                Message message = (Message) msg;
                message.verify();

                if (processor.consumesInboundMessage(message)) {
                    processor.onInboundMessage(message);
                } else {
                    messageExecutor.sendMessageDownwards(message);
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            ctx.close();
        }
    }

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        promise.setSuccess();
        if (msg instanceof FailureMessage) {
            log.error("Out Failure: " + ((FailureMessage) msg).getFailure());
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
            ctx.close();
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
