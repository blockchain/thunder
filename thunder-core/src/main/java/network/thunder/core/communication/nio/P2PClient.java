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
import network.thunder.core.communication.Node;
import network.thunder.core.communication.nio.handler.ChannelInit;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;

/**
 */
public final class P2PClient {

    private P2PContext context;

    public P2PClient (P2PContext context) {
        this.context = context;
    }

    static final int PORT = Integer.parseInt(System.getProperty("port", "8992"));

    //We will add a new handler for the different layers
    //Furthermore, we will add a new handler for the different message types,
    //as it will greatly improve readability and maintainability of the code.

    public void connectTo (Node node) throws Exception {
        System.out.println("Connect to " + node.getHost());
        new Thread(new Runnable() {
            @Override
            public void run () {

                while (!node.isConnected()) {

                    ECKey key = ECKey.fromPrivate(BigInteger.ONE.multiply(BigInteger.valueOf(100000)));

                    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                    EventLoopGroup workerGroup = new NioEventLoopGroup();

                    EventLoopGroup group = new NioEventLoopGroup();
                    try {
                        Bootstrap b = new Bootstrap();
                        b.group(group).channel(NioSocketChannel.class).handler(new ChannelInit(context, false, node, key));

                        // Start the connection attempt.
                        Channel ch = b.connect(node.getHost(), node.getPort()).sync().channel();
                        node.setConnected(ch.isOpen());

                    } catch (Exception e) {
                        //Not able to connect?
                        try {
                            Thread.sleep(60 * 1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        e.printStackTrace();
                    } finally {
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                    }
                }

            }
        }).start();

    }
}
