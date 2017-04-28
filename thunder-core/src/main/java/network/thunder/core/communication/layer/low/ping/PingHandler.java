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
package network.thunder.core.communication.layer.low.ping;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.security.SecureRandom;
import java.util.Random;

public class PingHandler extends ChannelDuplexHandler {

    static final long PING_INTERVAL = 10 * 1000;
    static final long TIMEOUT = 30 * 1000;

    long lastMessageSent = System.currentTimeMillis();
    long lastMessageReceived = System.currentTimeMillis();
    long lastPing = System.currentTimeMillis();
    long lastPong = System.currentTimeMillis();

    boolean connectionClosed = false;

    ChannelHandlerContext ctx;

    Random random = new SecureRandom();

    @Override
    public void channelActive (ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        ctx.fireChannelActive();

        new Thread(() -> {
            while (true) {
                if (connectionClosed) {
                    return;
                } else {
                    if (lastPing - lastPong > TIMEOUT) {
                        ctx.disconnect();
                    } else if (System.currentTimeMillis() - lastPing > PING_INTERVAL) {
                        sendPing();
                    }
                }

                try {
                    Thread.sleep(random.nextInt(1000));
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        lastMessageReceived = System.currentTimeMillis();

        if (msg instanceof Ping) {
            lastPing = System.currentTimeMillis();
            sendPong();
        } else if (msg instanceof Pong) {
            lastPong = System.currentTimeMillis();
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void sendPong () {
        lastPong = System.currentTimeMillis();
        ctx.writeAndFlush(new Pong());
    }

    private void sendPing () {
        lastPing = System.currentTimeMillis();
        ctx.writeAndFlush(new Ping());
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        lastMessageSent = System.currentTimeMillis();
        ctx.writeAndFlush(msg, promise);
    }

    @Override
    public void channelUnregistered (ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        connectionClosed = true;
    }
}
