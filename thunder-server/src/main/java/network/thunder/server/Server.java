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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.sql.DataSource;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import network.thunder.server.communications.RequestHandler;
import network.thunder.server.database.MySQLConnection;
import network.thunder.server.wallet.KeyChain;
import network.thunder.server.wallet.TransactionStorage;

import org.bitcoinj.utils.BriefLogFormatter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

// TODO: Auto-generated Javadoc
/**
 * The Class Server.
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
	
	
	
	
    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
    	DataSource dataSource = MySQLConnection.getDataSource();
    	Connection conn = dataSource.getConnection();
        try {
            MySQLConnection.getActiveChannels(conn);
        } catch(SQLException e) {
            MySQLConnection.resetToBackup();
        }
    	MySQLConnection.cleanUpDatabase(conn);
    	KeyChain keyChain = new KeyChain(conn);       

    	TransactionStorage.initialize(conn, keyChain.peerGroup);
    	
    	keyChain.transactionStorage = TransactionStorage.instance;
    	keyChain.start();
    	
    	TransactionStorage.instance.peerGroup = keyChain.peerGroup;
        
        
//        Peer peer = keyChain.peerGroup.get;
        
        Thread.sleep(3000);

        
        
//        transactionStorage.rebroadcastOpeningTransactions(peer);
    	
    	RequestHandler establishChannelHandler = new RequestHandler();
    	establishChannelHandler.transactionStorage = TransactionStorage.instance;
    	establishChannelHandler.dataSource = dataSource;
    	establishChannelHandler.wallet = keyChain.kit.wallet();
    	establishChannelHandler.peerGroup = keyChain.peerGroup;
    	
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(80);
        
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(80);
        server.setConnectors(new Connector[] { connector });

        
        ContextHandler context0 = new ContextHandler("/api");
        context0.setContextPath("/api");
        context0.setHandler(establishChannelHandler);
        
        ContextHandler context1 = new ContextHandler("/");
        context1.setContextPath("/");
        ResourceHandler rh0 = new ResourceHandler();
        rh0.setWelcomeFiles(new String[]{ "index.html" });
//        rh0.setDirectoriesListed(true);
        rh0.setResourceBase("");       
        
        context1.setHandler(rh0);
        
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { context0, context1 });
        
        server.setHandler(handlers);
        
        
        server.start();
//        server.join();
        
    	   	
        BriefLogFormatter.init();
        


    }
}
