/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package network.thunder.core.communication.nio.handler.low;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.Type;
import org.bitcoinj.core.ECKey;

import java.security.SecureRandom;

//TODO: Add a nonce to prevent replay attacks
public class EncryptionHandler extends ChannelDuplexHandler {

	private ECKey keyUs;
	private ECKey keyTheir;

	private boolean sentOurKey = false;
	private boolean keyReceived = false;

	private byte[] sharedSecret;

	private boolean isServer;

	public EncryptionHandler (boolean isServer) {
		//TODO: Probably not save yet...
		keyUs = new ECKey(new SecureRandom());
		this.isServer = isServer;
	}

	public void sendOurKey(ChannelHandlerContext ctx) {
		System.out.println("EncryptionHandler sendOurKey");
		sentOurKey = true;

		Object data = new Message(keyUs.getPubKey(), Type.KEY_ENC_SEND);
		ByteBuf buf = ctx.alloc().buffer();
		buf.writeBytes(keyUs.getPubKey());


		try {
			ctx.writeAndFlush(buf).sync().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete (ChannelFuture future) throws Exception {
					System.out.println(future);
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void channelActive (final ChannelHandlerContext ctx) {
		System.out.println("CHANNEL ACTIVE ENCRYPTION");
		//The node doing the incoming connection sends out his key first
		if (!isServer) {
			sendOurKey(ctx);
		}
	}

	@Override
	public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
		if(keyReceived) {
			//TODO: Add Decryption
			ctx.fireChannelRead(msg);
		} else {
			keyReceived = true;
			byte[] pubkey = new byte[33];

			ByteBuf buffer = (ByteBuf) msg;
			buffer.readBytes(pubkey);
			keyTheir = ECKey.fromPublicOnly(pubkey);

			if(!sentOurKey) {
				sendOurKey(ctx);
			}
			ctx.fireChannelActive();

		}

	}

	@Override
	public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		//TODO: Add Encryption
//		System.out.println("test");
		ctx.writeAndFlush(msg, promise);
	}

	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
