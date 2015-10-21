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
import network.thunder.core.communication.Node;
import network.thunder.core.communication.Type;
import network.thunder.core.etc.crypto.CryptoTools;
import network.thunder.core.etc.crypto.ECDH;
import network.thunder.core.etc.crypto.ECDHKeySet;
import org.bitcoinj.core.ECKey;

import java.security.SecureRandom;

//TODO: Add a nonce to prevent replay attacks
public class EncryptionHandler extends ChannelDuplexHandler {

	private ECKey keyServer;
	private ECKey keyClient;

	private boolean sentOurKey = false;
	private boolean keyReceived = false;

	private ECDHKeySet ecdhKeySet;
	private boolean isServer;

	long counterIn;
	long counterOut;

	Node node;

	public EncryptionHandler (boolean isServer, Node node) {
		//TODO: Probably not save yet...
		keyServer = new ECKey(new SecureRandom());
		this.isServer = isServer;
		this.node = node;
		node.setPubKeyTempServer(keyServer);
	}

	public void sendOurKey (ChannelHandlerContext ctx) {
		System.out.println("EncryptionHandler sendOurKey");
		sentOurKey = true;

		Object data = new Message(keyServer.getPubKey(), Type.KEY_ENC_SEND);
		ByteBuf buf = ctx.alloc().buffer();
		buf.writeBytes(keyServer.getPubKey());

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
		try {
			if (keyReceived) {
				System.out.println(msg);

				ByteBuf buf = (ByteBuf) msg;

				ByteBuf out = ctx.alloc().buffer();

				byte[] data = new byte[buf.readableBytes()];
				buf.readBytes(data);
				buf.release();

				data = CryptoTools.checkAndRemoveHMAC(data, ecdhKeySet.getHmacKey());

				byte[] enc = CryptoTools.decryptAES_CTR(data, ecdhKeySet.getEncryptionKey(), ecdhKeySet.getIvClient(), counterIn);

				out.writeBytes(enc);

				counterIn++;

				//TODO: Add Decryption
				ctx.fireChannelRead(out);
			} else {
				keyReceived = true;
				byte[] pubkey = new byte[33];

				ByteBuf buffer = (ByteBuf) msg;
				buffer.readBytes(pubkey);
				keyClient = ECKey.fromPublicOnly(pubkey);
				node.setPubKeyTempClient(keyClient);

				if (!sentOurKey) {
					sendOurKey(ctx);
				}

				try {
					this.ecdhKeySet = ECDH.getSharedSecret(keyServer, keyClient);
				} catch (Exception e) {
					e.printStackTrace();
				}
				ctx.fireChannelActive();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		try {
			ByteBuf buf = (ByteBuf) msg;

			ByteBuf out = ctx.alloc().buffer();

			byte[] data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			buf.release();

			byte[] enc = CryptoTools.encryptAES_CTR(data, ecdhKeySet.getEncryptionKey(), ecdhKeySet.getIvServer(), counterOut);

			enc = CryptoTools.addHMAC(enc, ecdhKeySet.getHmacKey());

			out.writeBytes(enc);

			counterOut++;

			System.out.println(msg);
			//TODO: Add Encryption
//		System.out.println("test");
			ctx.writeAndFlush(out, promise);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

	/* I don't like this design, as it wires the EncryptionHandler and the AuthenticationHandler together for eternity, but I guess that is per
	 * product design....
	 *
	 * TODO: Merge Encryption and Authentication handler, as authentication is no longer possible without encryption..
	 */
}
