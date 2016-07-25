package network.thunder.core.communication.layer.low.serialisation;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.etc.Tools;
import org.slf4j.Logger;

import java.util.List;

public class ByteToMessageObjectHandler extends ByteToMessageDecoder {
    private static final Logger log = Tools.getLogger();
    MessageSerializer serializater;

    public ByteToMessageObjectHandler (MessageSerializer serializater) {
        this.serializater = serializater;
    }

    @Override
    protected void decode (ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        try {
            if (in.readableBytes() > 0) {
                byte[] data = new byte[in.readableBytes()];
                in.readBytes(data);

                Message message = serializater.deserializeMessage(data);

                out.add(message);
//                log.debug("Incoming: " + message);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
