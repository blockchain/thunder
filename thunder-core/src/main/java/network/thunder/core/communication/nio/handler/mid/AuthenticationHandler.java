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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.messages.impl.MessageExecutorImpl;
import network.thunder.core.communication.processor.interfaces.AuthenticationProcessor;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/*
 * FLOW:
 * client send auth
 * server send auth
 */
public class AuthenticationHandler extends ChannelDuplexHandler {

    AuthenticationProcessor authenticationProcessor;

    public AuthenticationHandler (AuthenticationProcessor authenticationProcessor) {
        this.authenticationProcessor = authenticationProcessor;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) throws NoSuchProviderException, NoSuchAlgorithmException {
        System.out.println("CHANNEL ACTIVE AUTHENTICATION");

        authenticationProcessor.onLayerActive(new MessageExecutorImpl(ctx));

    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {

        checkIfMessage(msg);
        authenticationProcessor.onInboundMessageMessage((Message) msg);
    }

    @Override
    public void write (ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        promise.setSuccess();
        try {
            checkIfMessage(msg);
            authenticationProcessor.onOutboundMessage((Message) msg);
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
