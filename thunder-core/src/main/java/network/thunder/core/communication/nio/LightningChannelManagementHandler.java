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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.thunder.core.communication.Node;
import org.bitcoinj.core.ECKey;

/**
 *
 */
public class LightningChannelManagementHandler extends ChannelInboundHandlerAdapter {

	private Node node;
	private boolean isServer = false;

	private ECKey key;

	public LightningChannelManagementHandler (ECKey key, boolean isServer, Node node) {
		this.key = key;
		this.isServer = isServer;
		this.node = node;

	}

	@Override
	public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
		
	}

	@Override
	public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
