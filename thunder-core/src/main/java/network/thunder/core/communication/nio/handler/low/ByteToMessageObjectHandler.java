package network.thunder.core.communication.nio.handler.low;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializater;

import java.util.List;

/**
 * Created by matsjerratsch on 13/10/2015.
 */
public class ByteToMessageObjectHandler extends ByteToMessageDecoder {
    MessageSerializater serializater;

    public ByteToMessageObjectHandler (MessageSerializater serializater) {
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
//                System.out.println("Incoming: " + message);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
