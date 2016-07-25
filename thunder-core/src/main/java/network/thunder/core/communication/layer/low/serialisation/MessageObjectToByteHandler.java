package network.thunder.core.communication.layer.low.serialisation;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.etc.Tools;
import org.slf4j.Logger;

public class MessageObjectToByteHandler extends MessageToByteEncoder {
    private static final Logger log = Tools.getLogger();

    MessageSerializer serializater;

    public MessageObjectToByteHandler (MessageSerializer serializater) {
        this.serializater = serializater;
    }

    @Override
    public boolean acceptOutboundMessage (Object msg) {
        return msg instanceof Message;
    }

    @Override
    protected void encode (ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try {

            Message message = (Message) msg;

            byte[] data = serializater.serializeMessage(message);

            out.writeBytes(data);
            ctx.flush();
        } catch (Exception e) {
            log.warn("", e);
        }

    }
}
