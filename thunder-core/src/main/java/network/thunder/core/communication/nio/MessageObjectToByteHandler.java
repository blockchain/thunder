package network.thunder.core.communication.nio;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import network.thunder.core.communication.Message;
import network.thunder.core.etc.Tools;

/**
 * Created by matsjerratsch on 13/10/2015.
 */
public class MessageObjectToByteHandler extends MessageToByteEncoder {

	@Override
	protected void encode (ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
		Message message = (Message) msg;
		byte[] data = Tools.stringToByte(new Gson().toJson(message));
		out.writeBytes(data);
		ctx.writeAndFlush(out);

	}

	@Override
	public boolean acceptOutboundMessage (Object msg) {
		if (msg instanceof Message) {
			return true;
		}

		return false;
	}
}
