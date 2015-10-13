package network.thunder.core.communication.nio;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import network.thunder.core.communication.Message;

/**
 * Created by matsjerratsch on 13/10/2015.
 */
public class MessageObjectToByteHandler extends MessageToByteEncoder {

	@Override
	protected void encode (ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {

		try {

			Message message = (Message) msg;
			System.out.println("Outgoing: " + message.type);

//			System.out.println(new Gson().toJson(message));
//			byte[] data = Tools.stringToByte(new Gson().toJson(message));
			byte[] data = new Gson().toJson(message).getBytes("UTF-8");
			out.writeBytes(data);
			ctx.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean acceptOutboundMessage (Object msg) {
		if (msg instanceof Message) {
			return true;
		}

		return false;
//		return true;
	}
}
