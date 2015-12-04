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
import io.netty.channel.ChannelPromise;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.MessageExecutorImpl;
import network.thunder.core.communication.objects.messages.interfaces.message.FailureMessage;
import network.thunder.core.communication.processor.interfaces.EncryptionProcessor;

//TODO: Add a nonce to prevent replay attacks
public class EncryptionHandler extends ChannelDuplexHandler {

    EncryptionProcessor encryptionProcessor;

    public EncryptionHandler (EncryptionProcessor encryptionProcessor) {
        this.encryptionProcessor = encryptionProcessor;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) {
        try {
            System.out.println("CHANNEL ACTIVE ENCRYPTION");
            encryptionProcessor.onLayerActive(new MessageExecutorImpl(ctx));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            checkIfMessage(msg);
            if (msg instanceof FailureMessage) {
                System.out.println("In Failure: " + ((FailureMessage) msg).getFailure());
            } else {
                System.out.println("I: " + msg);
                encryptionProcessor.onInboundMessageMessage((Message) msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        throw new RuntimeException(cause);
    }

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        promise.setSuccess();
        if (msg instanceof FailureMessage) {
            System.out.println("Out Failure: " + ((FailureMessage) msg).getFailure());
        }
        try {
            checkIfMessage(msg);
            encryptionProcessor.onOutboundMessage((Message) msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkIfMessage (Object msg) {
        if (msg instanceof Message) {
            return;
        } else {
            throw new RuntimeException("Received a wrong type? " + msg);
        }
    }

}
