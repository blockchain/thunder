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
package network.thunder.core.communication.nio;

import com.google.gson.Gson;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.Node;
import network.thunder.core.communication.Type;
import network.thunder.core.communication.objects.subobjects.AuthenticationObject;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class AuthenticationHandler extends ChannelDuplexHandler {

	private Node node;
	private boolean isServer = false;

	private int authFailCounter = 0;
	private static final int AUTH_FAIL_MAX_RETRIES = 3;

	public AuthenticationHandler (boolean isServer) {
		this.isServer = isServer;
	}

	@Override
	public void channelActive (final ChannelHandlerContext ctx) {
		//The node receiving the incoming connection sends out his auth first
		if (isServer) {
			ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(future -> {
				node = new Node();
				sendAuthentication(ctx);
			});
		}
	}

	public void sendAuthentication (ChannelHandlerContext ctx) {
		ctx.writeAndFlush(node.getAuthenticationObject());
	}

	public void sendFailure (ChannelHandlerContext ctx) {
		ctx.writeAndFlush(new Message(null, Type.FAILURE, null));
	}

	public void sendAccept (ChannelHandlerContext ctx) {
		ctx.writeAndFlush(new Message(null, Type.AUTH_ACCEPT, null));
	}

	@Override
	public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
		if (node.isAuthFinished()) {
			//Authentication is complete, we don't touch any messages here anymore.
			ctx.fireChannelRead(msg);
		} else {
			//Check authentication first before doing anything else with the messages.
			Message message = (Message) msg;
			if (message.type > 1010 && message.type < 1099) {

				if (message.type == Type.AUTH_ACCEPT) {
					if (node.hasSentAuth() && node.isAuth()) {
						//Authentication complete. Send the node object to the next layer.
						node.finishAuth();
						ctx.fireChannelRead(node);
					} else {
						//AUTH_ACCEPT is only sent after we sent auth.
						sendFailure(ctx);
					}
				} else if (message.type == Type.AUTH_SEND) {
					AuthenticationObject authObject = new Gson().fromJson(message.data, AuthenticationObject.class);
					if (node.processAuthentication(authObject)) {
						if (node.hasSentAuth()) {
							sendAccept(ctx);
						} else {
							sendAuthentication(ctx);
						}
					}
				} else if (message.type == Type.AUTH_FAILED) {
					//For some reason authentication failed. We will retry it some times.
					if (authFailCounter < AUTH_FAIL_MAX_RETRIES) {
						sendAuthentication(ctx);
						authFailCounter++;
					} else {
						//TODO: Probably some more stuff here like blacklisting or anything?
						ctx.disconnect();
					}
				}

			} else {
				//Not authenticated. Will not look at messages with wrong types.
				sendFailure(ctx);
			}

		}

	}

	@Override
	public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		//Make sure to not send messages accidentally when auth is not finished yet.
		Message message = (Message) msg;
		if (!node.isAuthFinished() && (message.type < 1010 || message.type > 1099)) {
			throw new RuntimeException("This should not happen. Don't send messages when auth is not finished yet");
		}

		ctx.writeAndFlush(msg, promise);
	}

	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
