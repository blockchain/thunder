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
package network.thunder.core.communication.nio.handler.mid;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.MessageExecutorImpl;
import network.thunder.core.communication.processor.interfaces.GossipProcessor;
import network.thunder.core.mesh.Node;

import java.sql.SQLException;

/* This layer is for coordinating gossip messages we received.
 * Messages are sent using the Node.class.
 */
public class GossipHandler extends ChannelInboundHandlerAdapter {

    private Node node;
    GossipProcessor processor;

    public GossipHandler (Node node, GossipProcessor processor) {
        this.node = node;
        this.processor = processor;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) throws SQLException {
        System.out.println("CHANNEL ACTIVE GOSSIP");

        try {
            processor.onLayerActive(new MessageExecutorImpl(ctx));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            checkIfMessage(msg);
            processor.onInboundMessage((Message) msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void checkIfMessage (Object msg) {
        if (msg instanceof Message) {
            return;
        } else {
            throw new RuntimeException("Received a wrong type? " + msg);
        }
    }

}
