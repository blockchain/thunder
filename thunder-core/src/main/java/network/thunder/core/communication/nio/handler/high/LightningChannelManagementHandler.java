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
package network.thunder.core.communication.nio.handler.high;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.thunder.core.communication.Message;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.ECKey;

import java.util.ArrayList;

/**
 *
 */
public class LightningChannelManagementHandler extends ChannelInboundHandlerAdapter {

	private Node node;
	private boolean isServer = false;

	private ECKey key;

	public boolean initialized = false;

	ArrayList<Channel> channelList = new ArrayList<>();

	public LightningChannelManagementHandler (ECKey key, boolean isServer, Node node) {
		this.key = key;
		this.isServer = isServer;
		this.node = node;

	}

	public void initialize () {
		//TODO: Add all the database stuff here to get a complete list of all channels related with this node
		//Also, this is the place to ask for a new channel in case we don't have one yet (or want another one..)
	}

	@Override
	public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {

		if (!initialized) {
			initialize();
			ctx.fireChannelRead("1");
		} else {
			Message message = (Message) msg;
			if (message.type >= 110 && message.type < 199) {
				//TODO: Do all the channel opening stuff here..
			} else {
				ctx.fireChannelRead(msg);
			}
		}

	}

	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
