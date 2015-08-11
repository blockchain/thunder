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
package network.thunder.client.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Output;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.ClientTools;
import network.thunder.client.etc.Tools;
import network.thunder.client.wallet.TransactionStorage;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Wallet;


public class ThunderContext {
	
	public static Connection conn;
	
	private static ArrayList<Payment> paymentListIncluded = new ArrayList<Payment>();
	private static ArrayList<Payment> paymentListSettled = new ArrayList<Payment>();
	private static ArrayList<Payment> paymentListRefunded = new ArrayList<Payment>();
	private static ArrayList<Payment> paymentListOpen = new ArrayList<Payment>();
	
	public static ArrayList<Output> outputList = new ArrayList<Output>();
	
	public static ArrayList<Channel> channelList = new ArrayList<Channel>();
	
	public static Channel currentChannel;
	
	private static Wallet wallet;
	private static PeerGroup peerGroup;
	
	public static TransactionStorage transactionStorage;
	
    private static ArrayList<ChangeListener> listeners = new ArrayList<ChangeListener>();
    private static InitFinishListener initListener;
    private static ProgressUpdateListener updateListener;
    private static ErrorListener errorListener;

    private static boolean first = true;
    
    public static void init(Wallet w, PeerGroup p, int clientId) throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    	if(first) {
			System.out.println("Start init!");
			conn = MySQLConnection.getInstance(clientId);

            /**
             * Hack to check if the database has been created already..
             */
            try {
                channelList = MySQLConnection.getActiveChannels(conn);
            } catch(SQLException e) {
                MySQLConnection.buildDatabase(conn);
            }
			wallet = w;
			peerGroup = p;
			
			channelList = MySQLConnection.getActiveChannels(conn);
			if(channelList.size() > 0) {
				currentChannel = channelList.get(0);
				updatePaymentLists();
			}
			
			transactionStorage = TransactionStorage.initialize(conn);
			wallet.addEventListener(transactionStorage);
			System.out.println("Finished init! Active channels: "+channelList.size());
			
			TransactionStorage.updateOutputs(wallet);
			if(initListener != null)
				initListener.initFinished();
			first = false;
    	}
    }
	
	public static void init(Wallet w, PeerGroup p) throws SQLException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		init(w, p, 1);
	}
	
	private static void updatePaymentLists() throws SQLException {
		paymentListIncluded = MySQLConnection.getPaymentsIncludedInChannel(conn, currentChannel.getId());
		paymentListSettled = MySQLConnection.getPaymentsSettled(conn, currentChannel.getId());
		paymentListRefunded = MySQLConnection.getPaymentsRefunded(conn, currentChannel.getId());
		paymentListOpen = MySQLConnection.getPaymentsOpen(conn, currentChannel.getId());
	}
	

	
	
	
	public static ArrayList<Payment> getPaymentListIncluded() {
		return paymentListIncluded;
	}
	public static ArrayList<Payment> getPaymentListSettled() {
		return paymentListSettled;
	}
	public static ArrayList<Payment> getPaymentListRefunded() {
		return paymentListRefunded;
	}	
	public static ArrayList<Payment> getPaymentListOpen() {
		return paymentListOpen;
	}



	public static ArrayList<Channel> getChannelList() {
		return channelList;
	}
	
	public static Coin getAmountClient() {
		if(currentChannel == null)
			return Coin.ZERO;
		return Coin.valueOf(currentChannel.getAmountClient());
	}
	
	public static Coin getAmountClientAccessible() throws SQLException {
		if(currentChannel == null)
			return Coin.ZERO;
		if(currentChannel.getChannelTxClientID() == 0) {
			return Coin.valueOf(currentChannel.getAmountClient());
		} else {
			return currentChannel.getChannelTxClient().getOutput(0).getValue();
		}
	}
	
	public static boolean hasActiveChannel() { 
		return (channelList.size() != 0); 
	}
	
    public static void addListener(ChangeListener toAdd) {
		System.out.println("Listener added!");
        listeners.add(toAdd);
    }
    public static void setInitFinishedListener(InitFinishListener listener) {
    	initListener = listener;
    }
    
    public static void setProgressUpdateListener(ProgressUpdateListener listener) {
    	updateListener = listener;
    }
    public static void setErrorListener(ErrorListener listener) {
    	errorListener = listener;
    }
	
	public static PaymentRequest getPaymentReceiveRequest(long amount) throws Exception {
	
		Payment p = new Payment(0, currentChannel.getId(), amount);
		p.setReceiver(currentChannel.getPubKeyClient());
		p.paymentToServer = false;
		
		MySQLConnection.addPayment(conn, p);
		conn.commit();
		PaymentRequest request = new PaymentRequest(currentChannel, p);

		updatePaymentLists();
		for(ChangeListener listener : listeners)
			listener.channelListChanged();	
		
		return request;
		
	}
	
	public static void openChannel(final long clientAmount, final long serverAmount, final int timeInDays) throws Exception {
		new Thread(new Runnable() {
			@Override 
			public void run() {
			
				try {
					System.out.println("New Thread..");
					Channel channel = currentChannel;
					
					channel = ClientTools.createChannel(conn, wallet, peerGroup, clientAmount, serverAmount, timeInDays);
						
					channelList.add(channel);
					currentChannel = channel;
	
					
					for(ChangeListener listener : listeners)
						listener.channelListChanged();	
				} catch (Exception e) {
					throwError(Tools.stacktraceToString(e));
					e.printStackTrace();
				}
				
			}
		}).start();
			
		

		
	
		
	}
	
	public static void closeChannel() throws Exception {
		
		new Thread(new Runnable() {
			@Override 
			public void run() {
				try {
			
					ClientTools.closeChannel(conn, currentChannel, peerGroup);
					channelList.remove(currentChannel);
					currentChannel = null;
					
					for(ChangeListener listener : listeners)
						listener.channelListChanged();	
					
				} catch (Exception e) {
					throwError(Tools.stacktraceToString(e));
					e.printStackTrace();
				}
		
			}
		}).start();
		
	}
	
	public static void makePayment(final long amount, final String address) throws Exception {
		
		new Thread(new Runnable() {
			@Override 
			public void run() {
				try {
					
					PaymentRequest request = new PaymentRequest(currentChannel, amount, address);
			
					currentChannel = ClientTools.makePayment(conn, currentChannel, request.getPayment());
					updatePaymentLists();
					for(ChangeListener listener : listeners)
						listener.channelListChanged();	
					
				} catch (Exception e) {
					throwError(Tools.stacktraceToString(e));
					e.printStackTrace();
				} 
			}
		}).start();
	}
	
	public static void updateChannel() throws Exception {
		new Thread(new Runnable() {
			@Override 
			public void run() {
				try {
					/**
					 * TODO: change protocol, such that server sends amount of new payments
					 * 			with the first response, such that we know whether we should update
					 * 			at all.
					 */
					currentChannel = ClientTools.updateChannel(conn, currentChannel, true);
					
					updatePaymentLists();
					for(ChangeListener listener : listeners)
						listener.channelListChanged();
					
//					System.out.println("First finished!");
//					
//					currentChannel = ClientTools.updateChannel(conn, currentChannel, false);
//					
//					updatePaymentLists();
//					for(ChangeListener listener : listeners)
//						listener.channelListChanged();
					
					ThunderContext.progressUpdated(10, 10);
		
				} catch (Exception e) {
					throwError(Tools.stacktraceToString(e));
					e.printStackTrace();
				} 
			}
		}).start();
	}
	
	public static void progressUpdated(int progress, int max) {
		if(updateListener != null)
			updateListener.progressUpdated(progress, max);
	}
	
	public static void throwError(String error) {
		if(errorListener != null)
			errorListener.error(error);
	}
	
	public static Channel getCurrentChannel() {
		return currentChannel;
	}
	
	public interface ChangeListener {
		public void channelListChanged();
	}
	
	public interface InitFinishListener {
		public void initFinished();
	}	
	public interface ProgressUpdateListener {
		public void progressUpdated(int progress, int max);
	}
	public interface ErrorListener {
		public void error(String error);
	}
	
	
	public static void setChannel(Channel channel) {
		currentChannel = channel;
	}
	
	
	
	

}
