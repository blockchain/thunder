package network.thunder.core.communication.nio.handler.low;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.interfaces.helper.MessageSerializer;

/**
 * Created by matsjerratsch on 13/10/2015.
 */
public class MessageObjectToByteHandler extends MessageToByteEncoder {

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
//        System.out.println("Outgoing: " + msg);

        try {

            Message message = (Message) msg;

//			System.out.println(new Gson().toJson(message));
//			byte[] data = Tools.stringToByte(new Gson().toJson(message));
//          byte[] data = new Gson().toJson(message).getBytes("UTF-8");

            byte[] data = serializater.serializeMessage(message);

            out.writeBytes(data);
            ctx.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
