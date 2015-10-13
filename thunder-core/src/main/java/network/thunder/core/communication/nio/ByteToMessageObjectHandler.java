package network.thunder.core.communication.nio;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import network.thunder.core.communication.Message;
import network.thunder.core.etc.Tools;

import java.util.List;

/**
 * Created by matsjerratsch on 13/10/2015.
 */
public class ByteToMessageObjectHandler extends ByteToMessageDecoder {
	@Override
	protected void decode (ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		try {
//			int length = in.readInt();
//			in.discardReadBytes();
			byte[] data = new byte[in.readableBytes()];
			in.readBytes(data);
			Message message = new Gson().fromJson(Tools.byteToString(data), Message.class);
			out.add(message);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
