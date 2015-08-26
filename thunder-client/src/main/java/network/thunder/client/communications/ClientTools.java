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
package network.thunder.client.communications;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import network.thunder.client.api.ThunderContext;
import network.thunder.client.communications.addkeys.AddKeysHandler;
import network.thunder.client.communications.closechannel.CloseChannelHandler;
import network.thunder.client.communications.establishchannel.EstablishChannelHandlerOne;
import network.thunder.client.communications.establishchannel.EstablishChannelHandlerThree;
import network.thunder.client.communications.establishchannel.EstablishChannelHandlerTwo;
import network.thunder.client.communications.objects.AddKeysRequest;
import network.thunder.client.communications.objects.AddKeysResponse;
import network.thunder.client.communications.objects.CloseChannelRequest;
import network.thunder.client.communications.objects.CloseChannelResponse;
import network.thunder.client.communications.objects.EstablishChannelRequestOne;
import network.thunder.client.communications.objects.EstablishChannelRequestThree;
import network.thunder.client.communications.objects.EstablishChannelRequestTwo;
import network.thunder.client.communications.objects.EstablishChannelResponseOne;
import network.thunder.client.communications.objects.EstablishChannelResponseThree;
import network.thunder.client.communications.objects.EstablishChannelResponseTwo;
import network.thunder.client.communications.objects.SendPaymentRequestFour;
import network.thunder.client.communications.objects.SendPaymentRequestOne;
import network.thunder.client.communications.objects.SendPaymentRequestThree;
import network.thunder.client.communications.objects.SendPaymentRequestTwo;
import network.thunder.client.communications.objects.SendPaymentResponseFour;
import network.thunder.client.communications.objects.SendPaymentResponseOne;
import network.thunder.client.communications.objects.SendPaymentResponseThree;
import network.thunder.client.communications.objects.SendPaymentResponseTwo;
import network.thunder.client.communications.objects.UpdateChannelRequestFive;
import network.thunder.client.communications.objects.UpdateChannelRequestFour;
import network.thunder.client.communications.objects.UpdateChannelRequestThree;
import network.thunder.client.communications.objects.UpdateChannelRequestTwo;
import network.thunder.client.communications.objects.UpdateChannelResponseFive;
import network.thunder.client.communications.objects.UpdateChannelResponseFour;
import network.thunder.client.communications.objects.UpdateChannelResponseThree;
import network.thunder.client.communications.objects.UpdateChannelResponseTwo;
import network.thunder.client.communications.sendpayment.SendPaymentHandlerFour;
import network.thunder.client.communications.sendpayment.SendPaymentHandlerOne;
import network.thunder.client.communications.sendpayment.SendPaymentHandlerThree;
import network.thunder.client.communications.sendpayment.SendPaymentHandlerTwo;
import network.thunder.client.communications.updatechannel.UpdateChannelHandlerFive;
import network.thunder.client.communications.updatechannel.UpdateChannelHandlerFour;
import network.thunder.client.communications.updatechannel.UpdateChannelHandlerThree;
import network.thunder.client.communications.updatechannel.UpdateChannelHandlerTwo;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Output;
import network.thunder.client.database.objects.Payment;

import network.thunder.client.etc.PerformanceLogger;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;

import com.google.gson.Gson;

public class ClientTools {
	
	
	public static Channel createChannel(Connection conn, Wallet wallet, PeerGroup peerGroup, ArrayList<Output> outputArrayListlist, long clientAmount, long serverAmount, int timeInDays) throws Exception {
		ThunderContext.instance.progressUpdated(1, 10);
		
//		DataSource dataSource1 = MySQLConnection.getDataSource(1);
//        Connection conn = dataSource1.getConnection();
//		Connection conn = MySQLConnection.getInstance();
    	conn.setAutoCommit(false);
    	MySQLConnection.cleanUpDatabase(conn);
		
		ThunderContext.instance.progressUpdated(2, 10);

		/**
		 * First request..
		 */
//		System.out.println("Establish Channel...");
        Channel channel = MySQLConnection.createNewChannel(conn, wallet.currentReceiveAddress().toString());
        
        channel.setInitialAmountClient(clientAmount);
        channel.setInitialAmountServer(serverAmount);
        channel.setAmountClient(channel.getInitialAmountClient());
        channel.setAmountServer(channel.getInitialAmountServer());
        
        EstablishChannelHandlerOne requestOne = new EstablishChannelHandlerOne();
        
		ThunderContext.instance.progressUpdated(3, 10);

        
        requestOne.channel = channel;
        requestOne.conn = conn;
        requestOne.timeInDays = timeInDays;
        
        EstablishChannelRequestOne request = requestOne.request();
        Message requestWrapper = new Message(request, Type.ESTABLISH_CHANNEL_ONE_REQUEST, channel.getClientKeyOnClient(), channel.getTimestampOpen());

		ThunderContext.instance.progressUpdated(4, 10);


		channel.setTimestampOpen( requestWrapper.timestamp );
		channel.setTimestampClose( requestWrapper.timestamp + timeInDays * 24 * 60 * 60);
        
    	
    	String response = HTTPS.postToApi(requestWrapper);
    	Message responseOne = new Message(response, conn);
    	EstablishChannelResponseOne responseMessage = new Gson().fromJson(responseOne.data, EstablishChannelResponseOne.class);
    	

		ThunderContext.instance.progressUpdated(5, 10);
    	
    	requestOne.evaluateResponse(responseMessage);
//    	System.out.println("First request successful..");
    	
		/**
		 * Second request..
		 */
        EstablishChannelHandlerTwo requestTwo = new EstablishChannelHandlerTwo();
        
        
        requestTwo.channel = channel;
        requestTwo.conn = conn;
        requestTwo.outputArrayListlist = outputArrayListlist;
        
        conn.commit();
        EstablishChannelRequestTwo request2 = requestTwo.request();
        requestWrapper = new Message(request2, Type.ESTABLISH_CHANNEL_TWO_REQUEST, channel.getClientKeyOnClient());
    	response = HTTPS.postToApi(requestWrapper);
    	Message responseTwo = new Message(response, conn);
        
		ThunderContext.instance.progressUpdated(6, 10);

    	
    	EstablishChannelResponseTwo responseMessage2 = new Gson().fromJson(responseTwo.data, EstablishChannelResponseTwo.class);
    	requestTwo.evaluateResponse(responseMessage2);
    	MySQLConnection.updateChannel(conn, channel);
//    	System.out.println("Second request successful..");
    	
		ThunderContext.instance.progressUpdated(7, 10);

    	
		/**
		 * Third request..
		 */
        EstablishChannelHandlerThree requestThree = new EstablishChannelHandlerThree();
        
        requestThree.channel = channel;
        requestThree.conn = conn;
        requestThree.transactionStorage = ThunderContext.instance.transactionStorage;
        
        EstablishChannelResponseThree request3 = requestThree.request();
        requestWrapper = new Message(request3, Type.ESTABLISH_CHANNEL_THREE_REQUEST, channel.getClientKeyOnClient());
    	response = HTTPS.postToApi(requestWrapper);
    	Message responseThree = new Message(response, conn);
        
		ThunderContext.instance.progressUpdated(8, 10);

    	
    	EstablishChannelRequestThree responseMessage3 = new Gson().fromJson(responseThree.data, EstablishChannelRequestThree.class);
    	requestThree.evaluateResponse(responseMessage3);
    	
		ThunderContext.instance.progressUpdated(9, 10);

    	
    	channel.setReady(true);
    	channel.setEstablishPhase(0);
    	
    	MySQLConnection.updateChannel(conn, channel);
    	
		ThunderContext.instance.progressUpdated(10, 10);

    	
    	return channel;
    	
//    	System.out.println("Channel requested successful..");
	}
	
	
	public static Channel makePayment(Connection conn, Channel channel, Payment payment) throws Exception {
        System.out.println(channel.getPubKeyClient() + "   Make Payment");


        ThunderContext.instance.progressUpdated(1, 20);

		
//		Connection conn = MySQLConnection.getInstance();
//    	conn.setAutoCommit(false);
		
		PerformanceLogger logger = new PerformanceLogger(2);
		
		payment.setSecret(null);
		payment.paymentToServer = true;
		int id = MySQLConnection.addPayment(conn, payment);
        payment.setId(id);

		PerformanceLogger performance = new PerformanceLogger();

		ThunderContext.instance.progressUpdated(2, 20);

		
		/**
		 * Make sure both sides have sufficient amount of keys before starting a payment..
		 */
		int currentPayments = MySQLConnection.getCurrentPaymentsAmount(conn, channel);
//		ClientTools.requestKeys(conn, channel, (currentPayments + 1) * Constants.KEYS_PER_PAYMENT_CLIENTSIDE +1, (currentPayments + 1) * Constants.KEYS_PER_PAYMENT_SERVERSIDE +1);
		ClientTools.requestKeys(conn, channel, 1, 1);
		
		performance.measure("requestKeys");		
		
		ThunderContext.instance.progressUpdated(3, 20);

		
		
		/**
		 * First request..
		 */  
		ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
		
		/**
		 * Order our payments in the order we have it in the current channel
		 */
		if(channel.getChannelTxClientID() != 0) {
			ArrayList<Payment> tempList = new ArrayList<Payment>();
			List<TransactionOutput> outputs = channel.getChannelTxClient().getOutputs();
			for(int i=2; i<outputs.size(); ++i) {
				TransactionOutput output = outputs.get(i);
				String secret = ScriptTools.getRofPaymentScript(output);
				tempList.add(Tools.getPaymentOutOfList(paymentList, secret));
			}
			paymentList = tempList;
		}
		
		ThunderContext.instance.progressUpdated(4, 20);

		
		paymentList.add(payment);
		
        SendPaymentHandlerOne requestOne = new SendPaymentHandlerOne();
        
        requestOne.channel = channel;
        requestOne.conn = conn;
        requestOne.newPayment = payment;
        requestOne.paymentList = paymentList;

        conn.commit();
        
		ThunderContext.instance.progressUpdated(5, 20);

        
        SendPaymentRequestOne request = requestOne.request();
        Message requestWrapper = new Message(request, Type.SEND_PAYMENT_ONE_REQUEST, channel.getClientKeyOnClient());

		ThunderContext.instance.progressUpdated(6, 20);


		performance.measure("First Request Request");		

    	
    	String response = HTTPS.postToApi(requestWrapper);
    	
		ThunderContext.instance.progressUpdated(7, 20);

    	
    	
		performance.measure("First Request Server");		

    	
    	Message responseOne = new Message(response, conn);
    	SendPaymentResponseOne responseMessage = new Gson().fromJson(responseOne.data, SendPaymentResponseOne.class);
    	requestOne.evaluateResponse(responseMessage);
    	
		ThunderContext.instance.progressUpdated(8, 20);

    	
		performance.measure("First Request Evaluate");		

		/**
		 * Second request..
		 */
    	SendPaymentHandlerTwo requestTwo = new SendPaymentHandlerTwo();
        
        requestTwo.channel = channel;
        requestTwo.conn = conn;
        requestTwo.newPayment = payment;
        requestTwo.channelHash = responseMessage.channelHash;
        requestTwo.paymentList = paymentList;

        
        SendPaymentRequestTwo request2 = requestTwo.request();
        requestWrapper = new Message(request2, Type.SEND_PAYMENT_TWO_REQUEST, channel.getClientKeyOnClient());
        
		ThunderContext.instance.progressUpdated(9, 20);

        
		performance.measure("Second Request Request");		

        
    	response = HTTPS.postToApi(requestWrapper);
    	

		performance.measure("Second Request Server");		

    	Message responseTwo = new Message(response, conn);
    	
    	conn.commit();
        
		ThunderContext.instance.progressUpdated(10, 20);

    	
    	SendPaymentResponseTwo responseMessage2 = new Gson().fromJson(responseTwo.data, SendPaymentResponseTwo.class);
    	requestTwo.evaluateResponse(responseMessage2);
    	

		performance.measure("Second Request Evaluate");		

		/**
		 * Third request..
		 */
    	SendPaymentHandlerThree requestThree = new SendPaymentHandlerThree();
        
        requestThree.channel = channel;
        requestThree.conn = conn;

        
        SendPaymentRequestThree request3 = requestThree.request();
        
		ThunderContext.instance.progressUpdated(11, 20);

        
		performance.measure("Third Request Request");		

        requestWrapper = new Message(request3, Type.SEND_PAYMENT_THREE_REQUEST, channel.getClientKeyOnClient());
        

		performance.measure("Third Request Serializing");		

        
    	response = HTTPS.postToApi(requestWrapper);
    	
		ThunderContext.instance.progressUpdated(12, 20);


		performance.measure("Third Request Server");		

    	
    	Message responseThree;
    	responseThree = new Message(response, conn);
    	conn.commit();

    	

		performance.measure("Third Request Commit");		

    	
    	SendPaymentResponseThree responseMessage3 = new Gson().fromJson(responseThree.data, SendPaymentResponseThree.class);
    	requestThree.evaluateResponse(responseMessage3);
    	conn.commit();

		performance.measure("Third Request Evaluate");		

		ThunderContext.instance.progressUpdated(13, 20);

    	
		/**
		 * Fourth request..
		 */
    	SendPaymentHandlerFour requestFour = new SendPaymentHandlerFour();

        
        SendPaymentRequestFour request4 = requestFour.request();
        requestWrapper = new Message(request4, Type.SEND_PAYMENT_FOUR_REQUEST, channel.getClientKeyOnClient());
        
        conn.commit();
        
		ThunderContext.instance.progressUpdated(14, 20);

    	/**
    	 * Payment is final.
    	 */
		
		/**
		 * TODO: The swapping of the transaction ids is probably not very efficient, as there are 
		 * 		MySQL solutions to do it directly. 
		 * http://stackoverflow.com/questions/824936/mysql-performance-delete-or-update
		 */
		for(Payment p : paymentList) {
			p.replaceCurrentTransactionsWithTemporary();
		}
		conn.commit();
		MySQLConnection.updatePayment(conn, paymentList);
		MySQLConnection.getKeysOfUsToBeExposed(conn, channel, true);
		
		ThunderContext.instance.progressUpdated(15, 20);

		
    	MySQLConnection.updateChannel(conn, channel);
    	channel.replaceCurrentTransactionsWithTemporary();
    	MySQLConnection.updateChannel(conn, channel);
    	
    	payment.setIncludeInSenderChannel(true);
    	payment.setPhase(1);
    	MySQLConnection.updatePayment(conn, payment);
    	
		MySQLConnection.deleteUnusedAndExposedKeysFromUs(conn, channel);
		MySQLConnection.deleteUnusedKeyFromOtherSide(conn, channel);
		
		ThunderContext.instance.progressUpdated(16, 20);

        
        
		performance.measure("Fourth Request Request");		

        
        
        try {
	    	response = HTTPS.postToApi(requestWrapper);
	    	
			ThunderContext.instance.progressUpdated(17, 20);

	    	

			performance.measure("Fourth Request Server");		

	    	
	    	Message responseFour = new Message(response, conn);
	    	SendPaymentResponseFour responseMessage4 = new Gson().fromJson(responseFour.data, SendPaymentResponseFour.class);
	    	requestFour.evaluateResponse(responseMessage4);
    	} catch(Exception e) {
    		conn.rollback();
    		throw e;
    	}
    	
    	conn.commit();
    	
		ThunderContext.instance.progressUpdated(18, 20);

    	

    	channel = MySQLConnection.getChannel(conn, channel.getPubKeyClient());
    	channel.conn = conn;
    	
		ThunderContext.instance.progressUpdated(19, 20);


//    	System.out.println("Current channeltx:"+channel.getChannelTxClient());
//    	System.out.println("Temp channeltx:"+channel.getChannelTxClientTemp());
    	
    	
//    	logger.measure("Amount: "+paymentList.size()+" Total Payment");
    	
		ThunderContext.instance.progressUpdated(20, 20);


    	return channel;
	}
	
	public static void requestKeys(Connection conn, Channel channel, int amountClient, int amountServer) throws Exception {
        System.out.println(channel.getPubKeyClient() + "   Request Keys");

        /**
         * First request..
         */
        AddKeysHandler requestOne = new AddKeysHandler();
        
        requestOne.channel = channel;
        requestOne.conn = conn;
        requestOne.amountClient = amountClient;
        requestOne.amountServer = amountServer;



        conn.commit();
        AddKeysRequest request = requestOne.request();

		
        Message requestWrapper = new Message(request, Type.ADD_KEYS_REQUEST, channel.getClientKeyOnClient());
        
        /**
         * TODO: This takes 200ms for some random reason...
         */
    	String response = HTTPS.postToApi(requestWrapper);    	
    	Message responseOne = new Message(response, conn);
    	
    	AddKeysResponse responseMessage = new Gson().fromJson(responseOne.data, AddKeysResponse.class);
    	
    	requestOne.evaluateResponse(responseMessage);
	}
	
	public static void closeChannel(Connection conn, Channel channel, PeerGroup peerGroup) throws Exception {
        System.out.println(channel.getPubKeyClient() + "   Close Channel");

        /**
         * First request..
         */
        CloseChannelHandler requestOne = new CloseChannelHandler();
        
        requestOne.channel = channel;
        requestOne.conn = conn;


        conn.commit();
        CloseChannelRequest request = requestOne.request();

		
        Message requestWrapper = new Message(request, Type.CLOSE_CHANNEL_REQUEST, channel.getClientKeyOnClient());
        
    	String response = HTTPS.postToApi(requestWrapper);    	
    	Message responseOne = new Message(response, conn);
    	
    	CloseChannelResponse responseMessage = new Gson().fromJson(responseOne.data, CloseChannelResponse.class);
    	
    	requestOne.evaluateResponse(responseMessage);
    	
    	peerGroup.broadcastTransaction(requestOne.receivedTransaction);

        channel.setReady(false);
        MySQLConnection.updateChannel(conn, channel);
	}
	
	
	public static Channel updateChannel(Connection conn, Channel channel, boolean force) throws Exception {
        System.out.println(channel.getPubKeyClient() + "   Update Channel");
//		Connection conn = MySQLConnection.getInstance();
//    	conn.setAutoCommit(false);
		
//		UpdateChannelHandlerOne requestOne = new UpdateChannelHandlerOne();
//		requestOne.channel = channel;
//		requestOne.conn = conn;
//		
//		
//        UpdateChannelRequestOne request = requestOne.request();
//        Message requestWrapper = new Message(request, Type.UPDATE_CHANNEL_ONE_REQUEST, channel.getClientKeyOnClient());
//
//    	String response = HTTPS.postToApi(requestWrapper);
//    	Message responseOne = new Message(response, conn);
//    	UpdateChannelResponseOne responseMessage = new Gson().fromJson(responseOne.data, UpdateChannelResponseOne.class);
//    	requestOne.evaluate(responseMessage);

		ThunderContext.instance.progressUpdated(1, 10);

    	
		UpdateChannelHandlerTwo requestTwo = new UpdateChannelHandlerTwo();
		requestTwo.channel = channel;
		requestTwo.conn = conn;

		
        UpdateChannelRequestTwo request2 = requestTwo.request();
        Message requestWrapper = new Message(request2, Type.UPDATE_CHANNEL_TWO_REQUEST, channel.getClientKeyOnClient());
        
    	if(!force && requestTwo.amountNewPayments == 0)
    		return channel;

    	String response = HTTPS.postToApi(requestWrapper);
    	Message responseOne = new Message(response, conn);
    	UpdateChannelResponseTwo responseMessage2 = new Gson().fromJson(responseOne.data, UpdateChannelResponseTwo.class);
    	requestTwo.evaluate(responseMessage2);


    	
		ThunderContext.instance.progressUpdated(2, 10);


		UpdateChannelHandlerThree requestThree = new UpdateChannelHandlerThree();
		requestThree.channel = channel;
		requestThree.conn = conn;
		requestThree.newPaymentsTotal = requestTwo.newPaymentsTotal;
		
		
		
        UpdateChannelRequestThree request3 = requestThree.request();
        requestWrapper = new Message(request3, Type.UPDATE_CHANNEL_THREE_REQUEST, channel.getClientKeyOnClient());

    	response = HTTPS.postToApi(requestWrapper);
    	responseOne = new Message(response, conn);
    	UpdateChannelResponseThree responseMessage3 = new Gson().fromJson(responseOne.data, UpdateChannelResponseThree.class);
    	requestThree.evaluate(responseMessage3);
    	
		ThunderContext.instance.progressUpdated(3, 10);


    	
		UpdateChannelHandlerFour requestFour = new UpdateChannelHandlerFour();
		requestFour.channel = channel;
		requestFour.conn = conn;
		requestFour.serverHash = requestThree.serverHash;
		requestFour.newPaymentsTotal = requestThree.newPaymentsTotal;
		
		
        UpdateChannelRequestFour request4 = requestFour.request();
        requestWrapper = new Message(request4, Type.UPDATE_CHANNEL_FOUR_REQUEST, channel.getClientKeyOnClient());
        
		ThunderContext.instance.progressUpdated(4, 10);


    	response = HTTPS.postToApi(requestWrapper);
    	responseOne = new Message(response, conn);
    	UpdateChannelResponseFour responseMessage4 = new Gson().fromJson(responseOne.data, UpdateChannelResponseFour.class);
    	requestFour.evaluate(responseMessage4);
    	
    	
		UpdateChannelHandlerFive requestFive = new UpdateChannelHandlerFive();
		requestFive.channel = channel;
		requestFive.conn = conn;
		
		ThunderContext.instance.progressUpdated(5, 10);

		
		
        UpdateChannelRequestFive request5 = requestFive.request();
        requestWrapper = new Message(request5, Type.UPDATE_CHANNEL_FIVE_REQUEST, channel.getClientKeyOnClient());

    	response = HTTPS.postToApi(requestWrapper);
    	responseOne = new Message(response, conn);
    	UpdateChannelResponseFive responseMessage5 = new Gson().fromJson(responseOne.data, UpdateChannelResponseFive.class);
    	requestFive.evaluate(responseMessage5);
    	
		ThunderContext.instance.progressUpdated(6, 10);


    	
		/**
		 * Update complete.
		 * We should update our database accordingly:
		 * 		- Change the amount values in the database
		 * 		- Swap the TXIDs of all payments and of the channel
		 */
		ArrayList<Payment> paymentList = requestFour.newPaymentsTotal;
		ArrayList<Payment> oldPayments = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
		ArrayList<Payment> oldPaymentsToUpdate = new ArrayList<Payment>();


		
		long addAmountToServer = 0;
		long addAmountToClient = 0;
		/**
		 * Find out which payments got removed from the channel with this update and 
		 * 		add these amounts.
		 */
		for(Payment p1 : oldPayments) {
			boolean found = false;
			for(Payment p2 : paymentList) {
				if(p1.getSecretHash().equals(p2.getSecretHash())) {
					found = true;
					break;
				}
			}
			if(!found) {
				if(p1.paymentToServer) {
					p1.setIncludeInSenderChannel(false);
					if(p1.getPhase() != 5) {
						addAmountToServer+=p1.getAmount();
						addAmountToClient-=p1.getAmount();
					} else {
						p1.setPhase(6);
					}
				} else {
					p1.setIncludeInReceiverChannel(false);
					if(p1.getPhase() != 5) {
						addAmountToClient+=p1.getAmount() - Tools.calculateServerFee(p1.getAmount());
						addAmountToServer-=p1.getAmount() - Tools.calculateServerFee(p1.getAmount());
					} else {
						p1.setPhase(6);
					}
				}
				oldPaymentsToUpdate.add(p1);
			}
		}
		
		ThunderContext.instance.progressUpdated(7, 10);

		
//		System.out.println("Update complete! New channel transaction on client: ");
//		System.out.println(channel.getChannelTxClientTemp().toString());
		
		channel.setAmountClient(channel.getAmountClient() + addAmountToClient );
		channel.setAmountServer(channel.getAmountServer() + addAmountToServer );
		
		for(Payment p : paymentList) {
			if(!p.paymentToServer)
				if(p.getTimestampAddedToReceiver() == 0) 
					p.setTimestampAddedToReceiver(Tools.currentTime());
			if(p.paymentToServer) {
				p.setIncludeInSenderChannel(true);
			} else {
				p.setPhase(2);
				p.setIncludeInReceiverChannel(true);
			}
			p.replaceCurrentTransactionsWithTemporary();
		}
		MySQLConnection.updateChannel(conn, channel);
		channel.replaceCurrentTransactionsWithTemporary();
		
		ThunderContext.instance.progressUpdated(8, 10);

		
		MySQLConnection.updatePayment(conn, paymentList);
		MySQLConnection.updatePayment(conn, oldPaymentsToUpdate);
		MySQLConnection.updatePaymentRefunds(conn, channel);
        conn.commit();

		

		
		channel.setPaymentPhase(0);
		MySQLConnection.updateChannel(conn, channel);
		channel = MySQLConnection.getChannel(conn, channel.getPubKeyClient());
    	
		ThunderContext.instance.progressUpdated(9, 10);
    	
    	return channel;
    	
    	
    	
    	
    	
		
	}

}
