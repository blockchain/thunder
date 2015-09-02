/*
 *  ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 *  Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package network.thunder.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import network.thunder.server.communications.EventSocket;
import network.thunder.server.communications.Message;
import network.thunder.server.communications.RequestHandler;
import network.thunder.server.communications.WebSocketHandler;
import network.thunder.server.database.MySQLConnection;
import network.thunder.server.etc.SideConstants;
import network.thunder.server.etc.Tools;
import network.thunder.server.wallet.KeyChain;
import network.thunder.server.wallet.TransactionStorage;

import org.bitcoinj.utils.BriefLogFormatter;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

// TODO: Auto-generated Javadoc
/**
 * The Class Server.
 */

/**
 * TODO: Implement a push-system for real time updates on the clients.
 *          We will use WebSockets for this, as it's a fairly distributed technology that can be
 *              used for all plattforms.
 *          Additionally, we can also use Jetty to do all the background stuff of WebSockets
 *              for us. For now, we will use these to notify a client of a receiving payment,
 *              such that he can add it to the channel and clear it again.
 *          This will lead to an interface MUCH MORE common to any bitcoin wallet currently.
 *          Furthermore, it will lead to much faster channel clearance.
 *          Example Code to implement here:     https://github.com/jetty-project/embedded-jetty-websocket-examples/tree/master/native-jetty-websocket-example/src/main/java/org/eclipse/jetty/demo
 */
public class Server {

	/**
	 * The keychain.
	 */
	KeyChain keychain;
	
	/**
	 * The server.
	 */
	org.eclipse.jetty.server.Server server;
	
	/**
	 * The establish channel handler.
	 */
	RequestHandler establishChannelHandler;


    HashMap<Integer, Session> websocketList = new HashMap<>();
    static DataSource dataSource;
	
	
	
	
    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        System.out.println("Starting ThunderNetwork Server..");
    	dataSource = MySQLConnection.getDataSource();
        WebSocketHandler.init(dataSource);
        Connection conn = dataSource.getConnection();
        try {
            MySQLConnection.getActiveChannels(conn);
        } catch(SQLException e) {
            MySQLConnection.resetToBackup();
        }
    	MySQLConnection.cleanUpDatabase(conn);
        System.out.println("Syncing our Wallet..");
    	KeyChain keyChain = new KeyChain(conn);
        keyChain.startUp();


        TransactionStorage.initialize(conn, keyChain.peerGroup);
        TransactionStorage.instance.peerGroup = keyChain.peerGroup;
        System.out.println("Checking old Transactions..");
        keyChain.run();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Closing down the wallet gracefully..");
                keyChain.shutdown();
                System.out.println("Closing down the wallet gracefully successful..");
            }
        });



//        Peer peer = keyChain.peerGroup.get;
        
        Thread.sleep(3000);

        
        
//        transactionStorage.rebroadcastOpeningTransactions(peer);



    	
    	RequestHandler establishChannelHandler = new RequestHandler();
    	establishChannelHandler.transactionStorage = TransactionStorage.instance;
    	establishChannelHandler.dataSource = dataSource;
    	establishChannelHandler.wallet = keyChain.kit.wallet();
    	establishChannelHandler.peerGroup = keyChain.peerGroup;

        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server();

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(443);


        ServerConnector httpConnector = new ServerConnector(server);
        httpConnector.addConnectionFactory(new HttpConnectionFactory(http_config));
        httpConnector.setPort(80);




        if(!SideConstants.DEBUG) {
            String keystorePath = System.getProperty(
                    "user.home");
            File keystoreFile = new File(keystorePath, "keystore");
            if (!keystoreFile.exists())
            {
                throw new FileNotFoundException(keystoreFile.getAbsolutePath());
            }

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
            sslContextFactory.setKeyStorePassword("000000"); //Set correct password here for deployed system..
            sslContextFactory.setKeyManagerPassword("000000"); //Set correct password here for deployed system..
            sslContextFactory.setCertAlias("jetty");
            HttpConfiguration https_config = new HttpConfiguration(http_config);
            https_config.addCustomizer(new SecureRequestCustomizer());
            ServerConnector https = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory(https_config));
            https.setPort(443);
            https.setIdleTimeout(500000);
            server.setConnectors(new Connector[]{ httpConnector, https});

        } else {
            server.setConnectors(new Connector[] { httpConnector });
        }




        ContextHandler context0 = new ContextHandler("/api");
        context0.setContextPath("/api");
        context0.setHandler(establishChannelHandler);

        ContextHandler context1 = new ContextHandler("/");
        context1.setContextPath("/");
        ResourceHandler rh0 = new ResourceHandler();
        rh0.setWelcomeFiles(new String[]{"index.html"});
//        rh0.setDirectoriesListed(true);
        rh0.setResourceBase("");

        context1.setHandler(rh0);

        ServletContextHandler context2 = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context2.setContextPath("/");
//        server.setHandler(context2);
        ServletHolder holderEvents = new ServletHolder("websocket", new WebSocketServlet() {
            @Override
            public void doGet(HttpServletRequest request,
                              HttpServletResponse response) throws ServletException, IOException {
                response.getWriter().println("HTTP GET method not implemented.");
            }

            @Override
            public void configure(WebSocketServletFactory webSocketServletFactory) {
//                webSocketServletFactory.getPolicy().setIdleTimeout(10000);
                webSocketServletFactory.register(EventSocket.class);
            }
        });
        context2.addServlet(holderEvents, "/websocket");



        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{context0, context1, context2});
        server.setHandler(handlers);


        System.out.println("Server ready!");
        server.start();
        server.join();



        BriefLogFormatter.init();
        


    }


}
