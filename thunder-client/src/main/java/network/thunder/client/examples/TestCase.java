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
package network.thunder.client.examples;

import java.sql.Connection;

import javax.sql.DataSource;

import network.thunder.client.api.PaymentRequest;
import network.thunder.client.api.ThunderContext;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.ClientTools;
import network.thunder.client.etc.Tools;
import network.thunder.client.wallet.KeyChain;
import network.thunder.client.wallet.TransactionStorage;

import org.bitcoinj.utils.BriefLogFormatter;

public class TestCase {
	
	
	/**
	 * Create a new channel
	 */

	
	KeyChain keychain;
	
	public static String channel1 = Tools.byteToString(Tools.stringToByte58("oVddFMVCgFRyQq7ouzHy9DW82C3sRgZZr8J7rQdDbD9G"));
	public static String channel2 = Tools.byteToString(Tools.stringToByte58("kBze7cTJN8i65Xfah56DF2BhAFyTnQV7vq1Hs1N591YU"));
	
	public static boolean createNewChannels = false;
	
    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
        MySQLConnection.resetToBackup();

//        for(int i=0; i<20; i++) {
//        testPaymentsWithoutUpdates(2);     
//        testPaymentsWithoutUpdates2(2);     

//        	testPaymentsWithUpdates(5);
//        }
//        /**
//         * Test making Payments just from one channel, without any updates
//         */
//        MySQLConnection.resetToBackup();
//        testPaymentsWithoutUpdates(10);     
//        
//        
//        /**
//         * Test making Payments from channel1->channel2 and channel2->channel1 afterwards
//         */
//        MySQLConnection.resetToBackup();
//        testPaymentsWithoutUpdates2();        
//        
//        
//        /**
//         * Test making Payments from channel1->channel2 and channel2->channel1 afterwards
//         */
//        MySQLConnection.resetToBackup();
//        testPaymentsWithUpdates(2);
//        
//        
//        /**
//         * Test making Payments from channel1->channel2 and channel2->channel1 afterwards
//         */
//        MySQLConnection.resetToBackup();
//        testPaymentsWithRefunds();
//        
//        
////        MySQLConnection.resetToBackup();
//        testPaymentsWithoutUpdates(100);  
        
        
        testPaymentsWithoutUpdates2(100);        
//        testPaymentsWithUpdates(10);
//        testPaymentsWithRefunds();

    	

    }
    
//    /**
//     * Have channel 1 send money to channel 2
//     * 
//     * @param amount
//     * @throws Exception
//     */
//    static void testPaymentsWithoutUpdates(int amount) throws Exception {
//    	
//		long time = System.currentTimeMillis();
//    	
//		DataSource dataSource1 = MySQLConnection.getDataSource(1);
//		DataSource dataSource2 = MySQLConnection.getDataSource(2);
//		
//        Connection conn1 = dataSource1.getConnection();
//    	conn1.setAutoCommit(false);
//    	MySQLConnection.cleanUpDatabase(conn1);
//    	
//        Connection conn22 = dataSource2.getConnection();
//    	conn22.setAutoCommit(false);
//    	MySQLConnection.cleanUpDatabase(conn22);
//    	
//    	Connection conn11 = dataSource1.getConnection();
//    	conn11.setAutoCommit(false);
//    	
//    	TransactionStorage transactionStorage = TransactionStorage.initialize(conn1);
//    	KeyChain keyChain = new KeyChain();
//    	keyChain.transactionStorage = transactionStorage;
//        keyChain.conn = conn1;
//        keyChain.start();
//        
//        int i=0;
//        while(i<amount) {
//            Channel channel11 = MySQLConnection.getChannel(conn11, channel1);
//	        Payment newPayment = new Payment(channel1, channel2, 1000); 
//	        MySQLConnection.addPayment(conn22, newPayment);
//	        ClientTools.makePayment(conn11, channel11, newPayment);
//	        i++;
//        }
//        
//
//        
//        
//		long time2 = System.currentTimeMillis();
//		conn1.close();
//        conn11.close();  
//        conn22.close();
//
//		keyChain.stop();
//		
//		
//		System.out.println("testPayments success! Time: "+(time2-time)+"ms");
//
//    }
    
    /**
     * Have channel 1 send money to channel 2 and vice-versa
     * 
     * @param amount
     * @throws Exception
     */
    static void testPaymentsWithoutUpdates2(int amount) throws Exception {
    	
//    	KeyChain keyChain = new KeyChain();
////    	keyChain.transactionStorage = transactionStorage;
////        keyChain.conn = conn1;
//        keyChain.start();
//    	
//    	ThunderContext.init(keyChain.wallet, keyChain.peerGroup);
//    	
//    	ThunderContext.openChannel(100000, 100000, 100);
//    	Thread.sleep(4000);
//    	Channel channel1 = ThunderContext.currentChannel;
//    	
//    	ThunderContext.openChannel(100000, 100000, 100);
//    	Thread.sleep(4000);
//    	Channel channel2 = ThunderContext.currentChannel;
//
//    	
//		long time = System.currentTimeMillis();
//    	
//
//    	
//        int i=0;
//        while(i<amount) {
//        	
//        	ThunderContext.setChannel(channel1);
//        	PaymentRequest p1 = ThunderContext.getPaymentReceiveRequest(1000);
//        	
//        	ThunderContext.setChannel(channel2);        	
//        	ThunderContext.makePayment(1000, p1.getAddress());
//
//	        i++;
//	    	Thread.sleep(10000);
//
//        }
//        
//
//
//        
////        i=0;
////        while(i<amount) {
////	        Channel channel22 = MySQLConnection.getChannel(conn22, channel2);
////	        Payment newPayment = new Payment(channel2, channel1, 1000); 
////	        MySQLConnection.addPayment(conn11, newPayment);
////	        ClientTools.makePayment(channel22, newPayment);
////	        i++;
////        }
////        conn1.close();
////        conn2.close();
////        conn22.close(); 
////        conn11.close();    
////        keyChain.stop();
//        
//		long time2 = System.currentTimeMillis();
//		
//		System.out.println("testPayments2 success! Time: "+(time2-time)+"ms");

    }

//    /**
//     * Have payments from channel1 to channel2, vice-versa, and updates in between and at the end.
//     * 
//     * @param amount
//     * @throws Exception
//     */
//	static void testPaymentsWithUpdates(int amount) throws Exception {
//		
//		long time = System.currentTimeMillis();
//		
//		DataSource dataSource1 = MySQLConnection.getDataSource(1);
//		DataSource dataSource2 = MySQLConnection.getDataSource(2);
//		
//	    Connection conn1 = dataSource1.getConnection();
//		conn1.setAutoCommit(false);
//		MySQLConnection.cleanUpDatabase(conn1);
//		
//		
//    	TransactionStorage transactionStorage = TransactionStorage.initialize(conn1);
//    	KeyChain keyChain = new KeyChain();
//    	keyChain.transactionStorage = transactionStorage;
//        keyChain.conn = conn1;
//        keyChain.start();
//		
//		
//	    
//		Connection conn11 = dataSource1.getConnection();
//		conn11.setAutoCommit(false);
//		
//		Connection conn22 = dataSource2.getConnection();
//		conn22.setAutoCommit(false);
//		
//		Channel channel11 = MySQLConnection.getChannel(conn11, channel1);;
//	    Channel channel22 = MySQLConnection.getChannel(conn22, channel2);
//		Payment newPayment;
//		
//		for(int j=0; j<amount; j++) {
//		
//	    
//		    for(int i=0; i<amount; i++) {
//		        channel11 = MySQLConnection.getChannel(conn11, channel1);
//		        newPayment = new Payment(channel1, channel2, 1000); 
//		        MySQLConnection.addPayment(conn22, newPayment);
//		        ClientTools.makePayment(channel11, newPayment);
//		    }	    
//	
//		    for(int i=0; i<amount; i++) {
//		        channel22 = MySQLConnection.getChannel(conn22, channel2);
//		        newPayment = new Payment(channel2, channel1, 1000); 
//		        MySQLConnection.addPayment(conn11, newPayment);
//		        ClientTools.makePayment(channel22, newPayment);
//		    }
//		    
//		    channel11 = MySQLConnection.getChannel(conn11, channel1);
//		    ClientTools.updateChannel(channel11);
//		    channel22 = MySQLConnection.getChannel(conn22, channel2);
//		    ClientTools.updateChannel(channel22);
//		    
//		    for(int i=0; i<amount; i++) {
//		        channel11 = MySQLConnection.getChannel(conn11, channel1);
//		        newPayment = new Payment(channel1, channel2, 1000); 
//		        MySQLConnection.addPayment(conn22, newPayment);
//		        ClientTools.makePayment(channel11, newPayment);
//		    }	    
//	
//		    for(int i=0; i<amount; i++) {
//		        channel22 = MySQLConnection.getChannel(conn22, channel2);
//		        newPayment = new Payment(channel2, channel1, 1000); 
//		        MySQLConnection.addPayment(conn11, newPayment);
//		        ClientTools.makePayment(channel22, newPayment);
//		    }
//		    
//		    channel11 = MySQLConnection.getChannel(conn11, channel1);
//		    ClientTools.updateChannel(channel11);
//		    channel22 = MySQLConnection.getChannel(conn22, channel2);
//		    ClientTools.updateChannel(channel22);
//		    
//		    
//	    
//		}
//	    
//	    ClientTools.updateChannel(channel11);
//	    ClientTools.updateChannel(channel11);
//	    
//	    ClientTools.updateChannel(channel22);
//	    ClientTools.updateChannel(channel22);
//	    
//	    ClientTools.updateChannel(channel11);
//	    ClientTools.updateChannel(channel11);
//	    
//	    ClientTools.updateChannel(channel22);
//	    ClientTools.updateChannel(channel22);
//	
//	
//	
//	    conn22.commit();
//	    conn22.close(); 
//	    conn11.close();
//	    conn1.close();
//	    keyChain.stop();
//	    
//		long time2 = System.currentTimeMillis();
//		
//		System.out.println("testPayments with Updates success! Time: "+(time2-time)+"ms");
//	
//	}
//	
//	static void testPaymentsWithRefunds() throws Exception {
//		
//		long time = System.currentTimeMillis();
//		
//		DataSource dataSource1 = MySQLConnection.getDataSource(1);
//		DataSource dataSource2 = MySQLConnection.getDataSource(2);
//		
//	    Connection conn1 = dataSource1.getConnection();
//		conn1.setAutoCommit(false);
//		MySQLConnection.cleanUpDatabase(conn1);
//		
//	    Connection conn2 = dataSource2.getConnection();
//		conn2.setAutoCommit(false);
//		MySQLConnection.cleanUpDatabase(conn2);
//		
//    	TransactionStorage transactionStorage = TransactionStorage.initialize(conn1);
//    	KeyChain keyChain = new KeyChain();
//    	keyChain.transactionStorage = transactionStorage;
//        keyChain.conn = conn1;
//        keyChain.start();
//		
//		
//	    int i=0;
//	    
//		Connection conn22 = dataSource2.getConnection();
//		conn22.setAutoCommit(false);
//	    Channel channel22 = MySQLConnection.getChannel(conn22, channel2);
//	    
//		Connection conn11 = dataSource1.getConnection();
//		conn11.setAutoCommit(false);
//		Channel channel11 = MySQLConnection.getChannel(conn11, channel1);
//		Payment newPayment;
//	    
//		
//		
//		
//		
//	    while(i<15) {
//	        channel11 = MySQLConnection.getChannel(conn11, channel1);
//	        newPayment = new Payment(channel1, channel2, 1000); 
//	        ClientTools.makePayment(channel11, newPayment);
//	        i++;
//	    }	    
//	    i=0;
//	
//	
//
//	    
//	    newPayment = new Payment(channel2, channel1, 1000); 
//	    
//	    ClientTools.updateChannel(channel22);
//	    
//	    while(i<15) {
//	        channel22 = MySQLConnection.getChannel(conn22, channel2);
//	        newPayment = new Payment(channel2, channel1, 1000); 
//	        ClientTools.makePayment( channel22, newPayment);
//	        i++;
//	    }
//	    i=0;
//
////	    ClientTools.updateChannel(conn22, channel22);
//	    
//	    while(i<15) {
//	        channel22 = MySQLConnection.getChannel(conn22, channel2);
//	        newPayment = new Payment(channel2, channel1, 1000); 
//	        MySQLConnection.addPayment(conn11, newPayment);
//	        ClientTools.makePayment(channel22, newPayment);
//	        conn22.commit();
//	        i++;
//	    }
//	    
//	    while(i<15) {
//	        channel22 = MySQLConnection.getChannel(conn22, channel2);
//	        newPayment = new Payment(channel2, channel1, 1000); 
//	        ClientTools.makePayment(channel22, newPayment);
//	        i++;
//	    }
//	    i=0;
//	    ClientTools.updateChannel(channel22);
//	    ClientTools.updateChannel(channel22);
//	    
//	    while(i<15) {
//	        channel11 = MySQLConnection.getChannel(conn11, channel1);
//	        newPayment = new Payment(channel1, channel2, 1000); 
//	        ClientTools.makePayment(channel11, newPayment);
//	        i++;
//	    }	
//	    i=0;
//	    
//	    while(i<15) {
//	        channel11 = MySQLConnection.getChannel(conn11, channel1);
//	        newPayment = new Payment(channel1, channel2, 1000); 
//	        MySQLConnection.addPayment(conn22, newPayment);
//	        ClientTools.makePayment(channel11, newPayment);
//	        conn11.commit();
//	        i++;
//	    }
//	    
//	    ClientTools.updateChannel(channel11);
//
//	    
//	    while(i<15) {
//	        channel11 = MySQLConnection.getChannel(conn11, channel1);
//	        newPayment = new Payment(channel1, channel2, 1000); 
//	        ClientTools.makePayment(channel11, newPayment);
//	        i++;
//	    }	
//	    i=0;
//	    
//	    ClientTools.updateChannel(channel11);
//	    ClientTools.updateChannel(channel11);
//	    ClientTools.updateChannel(channel22);
//	    ClientTools.updateChannel(channel22);
//	    ClientTools.updateChannel(channel11);
//	    ClientTools.updateChannel(channel11);
//	    ClientTools.updateChannel(channel22);
//	    ClientTools.updateChannel(channel22);
//	
//	    conn22.commit();
//	    conn22.close(); 
//	    conn11.close();
//	    conn1.close();
//	    conn2.close();
//	    keyChain.stop();
//	    
//	    
//		long time2 = System.currentTimeMillis();
//		
//		System.out.println("testPayments with Refunds success! Time: "+(time2-time)+"ms");
//	
//	}
}
