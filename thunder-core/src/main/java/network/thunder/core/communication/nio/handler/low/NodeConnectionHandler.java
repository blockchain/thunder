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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.mesh.Node;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class NodeConnectionHandler extends ChannelDuplexHandler {

    private P2PContext context;
    private Node node;

    public NodeConnectionHandler (P2PContext context, Node node) {
        this.context = context;
        this.node = node;
    }

    @Override
    public void channelActive (ChannelHandlerContext ctx) {
        System.out.println("CHANNEL ACTIVE NODE_CONNECTION_HANDLER");
        context.activeNodes.add(node);
        node.setNettyContext(ctx);
        ctx.fireChannelActive();
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);

    }

    @Override
    public void channelUnregistered (ChannelHandlerContext ctx) {
        context.activeNodes.remove(node);
        System.out.println("CHANNEL UNREGISTERED..");
        node.closeConnection();
        ctx.fireChannelUnregistered();
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        context.activeNodes.remove(node);
        ctx.close();
    }

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ctx.writeAndFlush(msg, promise);
    }
}
