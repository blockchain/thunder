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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.ChannelInit;
import network.thunder.core.communication.layer.ContextFactory;

/**
 */
public final class P2PClient {

    ContextFactory contextFactory;

    public P2PClient (ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    //We will add a new handler for the different layers
    //Furthermore, we will add a new handler for the different message types,
    //as it will greatly improve readability and maintainability of the code.

    public void connectTo (ClientObject node) {
        new Thread(new Runnable() {
            @Override
            public void run () {

                while (!node.isConnected) {

                    //TODO Refactor
                    try {
                        connect(node);
                    } catch (Exception e) {
                        //Not able to connect?
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                }

            }
        }).start();

    }

    public void connectBlocking (ClientObject node) {
        connect(node);
    }

    private void connect (ClientObject node) {
        try {
            System.out.println("Connect to " + node.host + ":" + node.port + " - " + node.intent);

            EventLoopGroup group = new NioEventLoopGroup();
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInit(contextFactory, node));

            // Start the connection attempt.
            Channel ch = b.connect(node.host, node.port).sync().channel();
            node.isConnected = ch.isOpen();
            ch.closeFuture().sync();

            System.out.println("Connection to " + node.host + " closed..");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
