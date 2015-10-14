package network.thunder.core.communication.nio.handler.low;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import network.thunder.core.communication.Message;

import java.util.List;

/**
 * Created by matsjerratsch on 13/10/2015.
 */
public class ByteToMessageObjectHandler extends ByteToMessageDecoder {
	@Override
	protected void decode (ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
			if (in.readableBytes() > 0) {
				byte[] data = new byte[in.readableBytes()];
				in.readBytes(data);
				Message message = new Gson().fromJson(new String(data), Message.class);

				out.add(message);
				System.out.println("Incoming: " + message.type);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
