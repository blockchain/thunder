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

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */
public class DumpHexHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Incoming: " + msg);
        ctx.fireChannelRead(msg);

    }

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
//		System.out.println("test");
        System.out.println("Outgoing: " + msg);

        ctx.writeAndFlush(msg, promise);
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
