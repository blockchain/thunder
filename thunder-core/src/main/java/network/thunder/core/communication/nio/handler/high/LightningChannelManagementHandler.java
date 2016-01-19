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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.impl.MessageExecutorImpl;
import network.thunder.core.communication.processor.interfaces.LNEstablishProcessor;
import network.thunder.core.mesh.Node;

/**
 * Handler for opening new channels.
 * <p>
 * Channel establishment consists of 2 requests and responses.
 * If the process gets interrupted at any point (eg network failure), it has to be started from scratch again (no recovery).
 * <p>
 * The complete channel will only get saved to the database once the four requests has been completed.
 */
public class LightningChannelManagementHandler extends ChannelInboundHandlerAdapter {

    private Node node;

    MessageExecutor messageExecutor;
    LNEstablishProcessor messageProcessor;

    @Override
    public void channelActive (final ChannelHandlerContext ctx) {
        System.out.println("CHANNEL ACTIVE LNESTABLISH");

        messageProcessor.onLayerActive(new MessageExecutorImpl(ctx));
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        checkIfMessage(msg);
        messageProcessor.onInboundMessage((Message) msg);
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
