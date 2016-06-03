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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import network.thunder.core.communication.layer.PipelineInitialiser;
import network.thunder.core.communication.layer.ContextFactory;

/**
 */
public final class P2PServer {

    ContextFactory contextFactory;

    public P2PServer (ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    //We will add a new handler for the different layers
    //Furthermore, we will add a new handler for the different message types,
    //as it will greatly improve readability and maintainability of the code.

    public void startServer (int port) {
        try {

            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO)).childHandler(new PipelineInitialiser(contextFactory));
            b.bind(port).sync().channel().closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
