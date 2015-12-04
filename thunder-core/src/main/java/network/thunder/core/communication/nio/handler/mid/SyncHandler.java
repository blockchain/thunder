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
import network.thunder.core.communication.processor.interfaces.SyncProcessor;

/*
 * Handling the data transfer to new nodes.
 *
 * New nodes will first connect to a random node and just ask for more IP addresses.
 * As soon as they got new IP addresses, they will close the first connection and connect to
 * some of the new ones to ask them for fragments of the PubkeyChannelMap.
 */
public class SyncHandler extends ChannelInboundHandlerAdapter {

    SyncProcessor syncProcessor;

    public SyncHandler (SyncProcessor syncProcessor) {
        this.syncProcessor = syncProcessor;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) {
        System.out.println("CHANNEL ACTIVE SYNC");
        syncProcessor.onLayerActive(new MessageExecutorImpl(ctx));

    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            checkIfMessage(msg);
            syncProcessor.onInboundMessageMessage((Message) msg);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void channelUnregistered (ChannelHandlerContext ctx) {
        ctx.fireChannelUnregistered();
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
