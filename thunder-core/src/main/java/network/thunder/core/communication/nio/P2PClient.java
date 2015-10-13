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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import network.thunder.core.communication.Node;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;

/**
 */
public final class P2PClient {

	static final int PORT = Integer.parseInt(System.getProperty("port", "8992"));

	//We will add a new handler for the different layers
	//Furthermore, we will add a new handler for the different message types,
	//as it will greatly improve readability and maintainability of the code.

	public static Channel connectTo(String address, int port) throws Exception {

		ECKey key = ECKey.fromPrivate(BigInteger.ONE.multiply(BigInteger.valueOf(100000)));


		SelfSignedCertificate ssc = new SelfSignedCertificate();
		SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel (SocketChannel ch) throws Exception {
					Node node = new Node();

//					ch.pipeline().addLast(new DumpHexHandler());
					ch.pipeline().addLast(new EncryptionHandler());
//					ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
					ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2147483647, 0, 4, 0, 4));
					ch.pipeline().addLast(new LengthFieldPrepender(4));

					ch.pipeline().addLast(new ByteToMessageObjectHandler());
					ch.pipeline().addLast(new MessageObjectToByteHandler());
					ch.pipeline().addLast(new AuthenticationHandler(key, false, node));
				}
			});

			// Start the connection attempt.
			Channel ch = b.connect(address, port).sync().channel();

			return ch;
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}

	public static void main(String[] args) throws Exception {
		com.sun.org.apache.xml.internal.security.Init.init();
		connectTo("127.0.0.1", 8992);
	}
}
