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
package network.thunder.server.communications;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import network.thunder.server.communications.objects.AddKeysRequest;
import network.thunder.server.communications.objects.CloseChannelRequest;
import network.thunder.server.communications.objects.CloseChannelResponse;
import network.thunder.server.communications.objects.EstablishChannelRequestOne;
import network.thunder.server.communications.objects.EstablishChannelRequestThree;
import network.thunder.server.communications.objects.EstablishChannelRequestTwo;
import network.thunder.server.communications.objects.EstablishChannelResponseOne;
import network.thunder.server.communications.objects.EstablishChannelResponseThree;
import network.thunder.server.communications.objects.EstablishChannelResponseTwo;
import network.thunder.server.communications.objects.SendPaymentRequestFour;
import network.thunder.server.communications.objects.SendPaymentRequestOne;
import network.thunder.server.communications.objects.SendPaymentRequestThree;
import network.thunder.server.communications.objects.SendPaymentRequestTwo;
import network.thunder.server.communications.objects.SendPaymentResponseOne;
import network.thunder.server.communications.objects.SendPaymentResponseThree;
import network.thunder.server.communications.objects.SendPaymentResponseTwo;
import network.thunder.server.communications.objects.UpdateChannelRequestFive;
import network.thunder.server.communications.objects.UpdateChannelRequestFour;
import network.thunder.server.communications.objects.UpdateChannelRequestThree;
import network.thunder.server.communications.objects.UpdateChannelRequestTwo;
import network.thunder.server.communications.objects.UpdateChannelResponseFive;
import network.thunder.server.communications.objects.UpdateChannelResponseFour;
import network.thunder.server.communications.objects.UpdateChannelResponseOne;
import network.thunder.server.communications.objects.UpdateChannelResponseThree;
import network.thunder.server.communications.objects.UpdateChannelResponseTwo;
import network.thunder.server.database.MySQLConnection;
import network.thunder.server.database.objects.Channel;
import network.thunder.server.database.objects.Key;
import network.thunder.server.database.objects.KeyWrapper;
import network.thunder.server.database.objects.Output;
import network.thunder.server.database.objects.Payment;
import network.thunder.server.database.objects.Secret;
import network.thunder.server.database.objects.TransactionWrapper;
import network.thunder.server.etc.Constants;
import network.thunder.server.etc.PerformanceLogger;
import network.thunder.server.etc.ScriptTools;
import network.thunder.server.etc.Tools;
import network.thunder.server.wallet.TransactionStorage;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Transaction.SigHash;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.wallet.DefaultRiskAnalysis;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.google.gson.Gson;

// TODO: Auto-generated Javadoc
/**
 * HttpHandler for handling the HTTP Requests of establishing a Channel.
 */
public class RequestHandler extends AbstractHandler {

	/**
	 * The data source.
	 */
	public DataSource dataSource;
	
	/**
	 * The peer group.
	 */
	public PeerGroup peerGroup;
	
	/**
	 * The wallet.
	 */
	public Wallet wallet;
	
	/**
	 * The transaction storage.
	 */
	public TransactionStorage transactionStorage;

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.server.Handler#handle(java.lang.String, org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void handle(String target, Request baseRequest, HttpServletRequest jettyRequest, HttpServletResponse jettyResponse) throws IOException {
		
		System.out.println("Request.. "+target);
		if(baseRequest.getMethod() == "GET") {
//			jettyResponse.setContentType("text/plain;charset=utf-8");
			jettyResponse.setStatus(HttpServletResponse.SC_OK);
	        baseRequest.setHandled(true);
	        jettyResponse.getWriter().println("Hello there");
	        return;
		}
		
		
		Message responseContainer = new Message();
		Message message = null;
		Connection conn = null;
		Channel channel = null;
		Payment payment = null;
		Transaction receivedTransaction = null;

		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			message = Tools.getMessage(jettyRequest);
			message.prepare(conn);
						

			if (message.type == Type.ESTABLISH_CHANNEL_ONE_REQUEST) {
				/**
				 * First Request for establishing the channel.
				 * 
				 * Request: 	- PubKey of the client, that will be part of the 2-of-2 multisig channel
				 * 				- Maximum timeframe in days, needed for setting up the refund transactions
				 * 				- TotalAmount of the channel
				 * 				- ClientAmount of the channel
				 * 				- changeAddressClient for all channel payouts in the future
				 * 
				 * Response: 	- PubKey of the server, that will be part of the 2-of-2 multisig channel
				 * 				- TXunsigned, a transaction with the outputs needed for the channel and the server-change
				 * 				- changeAddressClient for all channel payouts in the future
				 * 				
				 */
				EstablishChannelRequestOne m = new Gson().fromJson(message.data, EstablishChannelRequestOne.class);				
				/**
				 * Test if there is already a channel open with this pubkey..
				 */
				if(MySQLConnection.testIfChannelExists(conn, message.pubkey))
					throw new Exception("Pubkey is already in use for a channel..");
				
								
				if(!m.pubKey.equals(message.pubkey)) throw new Exception("Signature Key does not match Channel Key..");
				if(m.timeInDays < Constants.MIN_CHANNEL_DURATION_IN_DAYS || m.timeInDays > Constants.MAX_CHANNEL_DURATION_IN_DAYS) throw new Exception("Channel duration exceeds limits..");
				
				/**
				 * Rules for the channel: 
				 * 	(1) Total value < MAX_CHANNEL_VALUE
				 * 	(2) serverShare < MAX_SERVER_SHARE
				 * 	(3) Total value > MIN_CHANNEL_VALUE
				 * 	(4) clientShare > serverShare
				 */
				if(m.totalAmount > Constants.MAX_CHANNEL_VALUE)
					throw new Exception("Channel value is too high. Currently the maximum is "+Constants.MAX_CHANNEL_VALUE+"satoshi.");
				
				if(m.totalAmount - m.clientAmount > Constants.MAX_SERVER_SHARE) 
					throw new Exception("Server share too high. Currently the maximum is "+Constants.MAX_SERVER_SHARE+"satoshi.");
				
				if(m.totalAmount - m.clientAmount > m.clientAmount) 
					throw new Exception("Server share higher than client share..");
					
				if(m.totalAmount < Constants.MIN_CHANNEL_VALUE)
					throw new Exception("Channel value is too low. Currently the minimum is "+Constants.MIN_CHANNEL_VALUE+"satoshi.");
							
				
				/**
				 * Create the channel and set all the values accordingly..
				 */
				channel = MySQLConnection.createNewChannel(conn, m, message.timestamp, wallet.getChangeAddress().toString(), m.changeAddress);
				channel.setAmountServer(m.totalAmount - m.clientAmount);
				channel.setAmountClient(m.clientAmount);
				channel.setInitialAmountServer(channel.getAmountServer());
				channel.setInitialAmountClient(channel.getAmountClient());
				channel.setTimestampOpen(message.timestamp);
				channel.setTimestampClose(message.timestamp + 24 * 60 * 60 * m.timeInDays);
				channel.setKeyChainDepth(m.timeInDays * 24 * 60 * 60 / Constants.TIMEFRAME_PER_KEY_DEPTH );
				channel.setKeyChainChild(1);
				channel.setPubKeyServer(Tools.byteToString(channel.getServerKeyOnServer().getPubKey()));
				
				
				/**
				 * Create the unsigned transaction for the client
				 */
				Transaction channelTransaction = new Transaction(Constants.getNetwork());
				Script correctScript = ScriptTools.getMultisigOutputScript(channel);
				channelTransaction.addOutput(Coin.valueOf(m.totalAmount), correctScript);
				channelTransaction = MySQLConnection.getOutAndInputsForChannel(conn, channel.getId(), m.totalAmount - m.clientAmount, channelTransaction, wallet.getChangeAddress(), false, false);
				
				
				/**
				 * Update our Database
				 */
				channel.setOpeningTx(channelTransaction);
				channel.setEstablishPhase(2);
				MySQLConnection.updateChannel(conn, channel);
				
				
				/**
				 * Fill the request object for the client.
				 */
				EstablishChannelResponseOne response = new EstablishChannelResponseOne();
				response.pubKey = channel.getPubKeyServer();
				response.changeAddress = channel.getChangeAddressServer();
				response.txUnsigned = Tools.byteToString(channelTransaction.bitcoinSerialize());
				responseContainer.success = true;
				responseContainer.type = Type.ESTABLISH_CHANNEL_ONE_RESPONSE;
				responseContainer.fill(response);
				
				

				
				


			} else if (message.type == Type.ESTABLISH_CHANNEL_TWO_REQUEST) {
				/**
				 * Second Request for establishing the channel.
				 * 
				 * Request: 	Transaction to 2-of-2 multisig, with additional inputs needed signed
				 * 
				 * Response: 	Refund transaction signed by the Server, with the initial channel transaction as an input
				 * 					The client must use this hash to send us a refund prior to us broadcasting the channel.
				 * 
				 * 				
				 */
				channel = MySQLConnection.getChannel(conn, message.pubkey);

				EstablishChannelRequestTwo m = new Gson().fromJson(message.data, EstablishChannelRequestTwo.class);
				
				Transaction transactionFromClient = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.transaction));
				Transaction transactionFromServer = channel.getOpeningTx();
				
				/**
				 * Test if the channel is indeed not yet created..
				 */
				if(channel.isReady()) 
					throw new Exception("Channel is ready already");
				if(channel.getEstablishPhase() != 2) 
					throw new Exception("Something went wrong somewhere, start again with a new pubkey..");
				
				
				/**
				 * Compare the clientTransaction with the saved serverTransaction
				 * 	and check if only his in- and outputs were added and none of 
				 * 	ours has been changed.
				 * 
				 * Also check if the values are accordingly..
				 */
				List<TransactionInput> clientInputs = transactionFromClient.getInputs();
				List<TransactionOutput> clientOutputs = transactionFromClient.getOutputs();
				
				List<TransactionInput> clientOnlyInputs = new ArrayList<TransactionInput>();
				List<TransactionOutput> clientOnlyOutputs = new ArrayList<TransactionOutput>();
				
				List<TransactionInput> serverInputs = transactionFromServer.getInputs();
				List<TransactionOutput> serverOutputs = transactionFromServer.getOutputs();
				
				for(int i=0; i<clientOutputs.size(); ++i) {
					if(i>serverOutputs.size()-1) {
						clientOnlyOutputs.add(clientOutputs.get(i));
					} else {
						TransactionOutput client = clientOutputs.get(i);
						TransactionOutput server = serverOutputs.get(i);					
						
						if(!Arrays.equals(client.bitcoinSerialize(), server.bitcoinSerialize()))
							throw new Exception("TransactionOutput has been changed..");
					}
				}
				

				
				for(int i=0; i<clientInputs.size(); ++i) {
					if(i>serverInputs.size()-1) {
						clientOnlyInputs.add(clientInputs.get(i));
					} else {
						TransactionInput client = clientInputs.get(i);
						TransactionInput server = clientInputs.get(i);
						
						if(!client.equals(server))
							throw new Exception("TransactionInput has been changed..");
					}
				}

				
				/**
				 * Add all our signatures to make the transaction valid
				 */
				for(int i=0; i<serverInputs.size(); ++i) {
					TransactionInput input = clientInputs.get(i);
					Output output = MySQLConnection.getOutput(conn, input.getOutpoint().getHash().toString(), input.getOutpoint().getIndex());
					ECDSASignature signature = Tools.getSignature(transactionFromClient, i, output.getTransactionOutput(), output.getECKey());
					TransactionSignature transactionSignature = new TransactionSignature(signature, SigHash.ALL, false);
					Script inputScript = ScriptBuilder.createInputScript(transactionSignature, output.getECKey());
					transactionFromClient.getInput(i).setScriptSig(inputScript);
					
				}
				
				/**
				 * Calculate the in- and outputs and check if the client provided enough inputs
				 * 		to match the fee requirements.
				 */
				
				long sumInputs = 0;
				long sumOutputs = 0;
				long difference = 0;
						

//				sumOutputs = Tools.getCoinValueFromOutput(transactionFromClient.getOutputs());
//				sumInputs = Tools.getCoinValueFromInput(peerGroup.getConnectedPeers().get(0), transactionFromClient.getInputs());
//
//				difference = sumInputs - sumOutputs;
//				
//				long fees = Tools.getTransactionFees(transactionFromClient.getInputs().size(), transactionFromClient.getOutputs().size());
//					
//				/**
//				 * TODO: This is currently pretty difficult.. 
//				 * For debug purposes, I leave this commented now, but we should find a way, if the client send us proper inputs
//				 */
//				if(difference < fees)
//					throw new Exception("Inputs not sufficient for the channel.. Should be: "+sumOutputs+" Is: "+sumInputs+" Difference: "+difference+" Fees: "+fees);
				
				/**
				 * Make sure the transaction is actually spendable..
				 */			
				transactionFromClient.verify();
								
				/**
				 * Save the transaction into our channel.
				 * This will be the final opening transaction..
				 */
				channel.setOpeningTx(transactionFromClient);
				channel.setOpeningTxHash(transactionFromClient.getHashAsString());
				
				
				/**
				 * Build the refund transaction
				 */
				TransactionWrapper transactionWrapper = Tools.getChannelRefundTransaction(channel);
				Transaction refundTransaction = transactionWrapper.getTransaction();
				ECDSASignature signature = transactionWrapper.getSignature();	
				
				
				/**
				 * Update our database with the channel
				 */
				channel.setEstablishPhase(3);
				channel.setRefundTxClient(refundTransaction);
				channel.setRefundTxServer(refundTransaction);
				MySQLConnection.updateChannel(conn, channel);
				
				
				/**
				 * Fill the respone to the client
				 */
				EstablishChannelResponseTwo response = new EstablishChannelResponseTwo();
				
				response.refundServerSig = Tools.byteToString(signature.encodeToDER());
				response.refundTransaction = Tools.byteToString(refundTransaction.bitcoinSerialize());
				
				responseContainer.success = true;
				responseContainer.type = Type.ESTABLISH_CHANNEL_TWO_RESPONSE;
				responseContainer.fill(response);

			} else if (message.type == Type.ESTABLISH_CHANNEL_THREE_REQUEST) {
				/**
				 * Third Request for establishing the channel.
				 * 
				 * Request: 	Refund transaction signed by the client, with the opening transaction as input
				 * 					Check if this allocates the funds correctly
				 * 				List of minimum MIN_KEYS_ON_CHANNEL_CREATION to use for revokes
				 * 
				 * Response: 	List of minimum MIN_KEYS_ON_CHANNEL_CREATION to use for revokes
				 * 
				 * After verifying, the server will broadcast the opening transaction and the channel will be established.
				 */
				EstablishChannelRequestThree m = new Gson().fromJson(message.data, EstablishChannelRequestThree.class);
				
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
				/**
				 * Test if the channel is indeed not yet created..
				 */
				if(channel.isReady()) 
					throw new Exception("Channel is ready already");
				if(channel.getEstablishPhase() != 3) 
					throw new Exception("Something went wrong somewhere, start again with a new pubkey..");
				

				
				Transaction openingTransaction = channel.getOpeningTx();

				TransactionWrapper transactionWrapper = Tools.getChannelRefundTransaction(channel);
				Transaction refundTransaction = transactionWrapper.getTransaction();
				
				/**
				 * Add the signature we got from the client into the refund transaction,
				 * 		to check that it is correct..
				 */
				ECDSASignature serverSignature = transactionWrapper.getSignature();	
				ECDSASignature clientSignature = ECDSASignature.decodeFromDER(Tools.stringToByte(m.refundTransactionSignature));

				Script inputScript = Tools.getMultisigInputScript(clientSignature, serverSignature);
				refundTransaction.getInput(0).setScriptSig(inputScript);
				inputScript.correctlySpends(refundTransaction, 0, openingTransaction.getOutput(0).getScriptPubKey());

				
				
				DefaultRiskAnalysis.isStandard(openingTransaction);
				DefaultRiskAnalysis.isStandard(refundTransaction);

				/**
				 * Everything is okay, broadcast the opening transaction
				 * 
				 * Also, add it to the TransactionStorage and wait for enough confirmations
				 * 		to have the channel ready.
				 */
				System.out.println(openingTransaction);
				peerGroup.broadcastTransaction(openingTransaction);
				MySQLConnection.deleteOutputByChannel(conn, channel);
				
				
				/**
				 * Update our database with the channel
				 */
				transactionStorage.addOpenedChannel(channel);

				channel.setEstablishPhase(4);
				channel.setRefundTxServer(refundTransaction);
				channel.setRefundTxClient(refundTransaction);
				channel.setTimestampForceClose(channel.getTimestampRefunds());
				
				/**
				 * Debug
				 */
				channel.setEstablishPhase(0);
				channel.setReady(true);
				
				MySQLConnection.updateChannel(conn, channel);
				

				
				/**
				 * Fill the respone to the client
				 */
				EstablishChannelResponseThree response = new EstablishChannelResponseThree();
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.ESTABLISH_CHANNEL_THREE_REQUEST;

			} else if (message.type == Type.SEND_PAYMENT_ONE_REQUEST) {
				/**
				 * First Request making a payment.
				 * 
				 * Request: 	- A new serverside channel tx according to the new payment
				 * 				- The receipient for the payment 
				 * 				- The amount to be sent
				 * 
				 * Response: 	- Updated clientside channel tx
				 * 				- The hash of the serverside channel
				 * 				
				 */
				SendPaymentRequestOne m = new Gson().fromJson(message.data, SendPaymentRequestOne.class);				

				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
				/**
				 * Check if the payment comply with our general rules.
				 * TODO: Add further checks here to check if a maximum amount of payments is reached already.
				 * 
				 * TODO: Check if there are enough funds left on the receiving channel
				 * 			to cover this payment..
				 */
				if(!channel.isReady()) 
					throw new Exception("Channel is not ready for payments..");
				
				if(m.amount < Constants.MIN_PAYMENT) 
					throw new Exception("Payment is below threshold");
				
				if(m.amount > channel.getAmountClient()) 
					throw new Exception("Payment is too high for the channel");
				
				if(channel.getChannelTxClientID() != 0) 
					if(m.amount + 2 * Tools.getTransactionFees(Constants.SIZE_OF_SETTLEMENT_TX) > channel.getChannelTxClient().getOutput(0).getValue().value)
						throw new Exception("Too many uncleared payments in the channel. Current Client change not sufficient.");
				
				int receiverChannelId = MySQLConnection.checkChannelForReceiving(conn, m.receipient);
				if(receiverChannelId == 0)
					throw new Exception("Receiver does not exist or is not ready for receiving payments..");
				

				
				/**
				 * Just allow one input for now, the one of the multisig
				 */
				receivedTransaction = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.channelTransaction));

				if(receivedTransaction.getInputs().size() != 1) 
					throw new Exception("Transaction input count is not 1");
				
				TransactionInput input = receivedTransaction.getInput(0);
				TransactionOutPoint outpoint = input.getOutpoint();
				if(!outpoint.getHash().toString().equals(channel.getOpeningTx().getHash().toString()) || outpoint.getIndex() != 0)
					throw new Exception("Transaction input does not point to the opening transaction");
				
				
				/**
				 * Check the outputs of the transaction that was sent to us
				 * 
				 *  (1) Total amount of outputs must be one higher than the one we have on file
				 *  (2) It should pay the client back the correct amount
				 *  (3) It should pay us the correct amount back
				 *  (4) The next N outputs must match the outputs of the channel tx we have on file
				 *  		the value must be the same
				 *  		the keys used must be unused keys from me				 *  		
				 *  (5) The last output must point to the new payment
				 *  
				 *  If there is no channel transaction on file, this means this is the first payment.
				 *  In this case, (1) and (4) are true automatically.
				 */
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
				KeyWrapper keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, true);
				
				if(channel.getChannelTxServerID() != 0) {
					Transaction channelTransaction = channel.getChannelTxServer();
//					System.out.println("Current Transaction: \n"+channelTransaction + "\n\nNew Transaction: "+receivedTransaction);
					if(receivedTransaction.getOutputs().size() - channelTransaction.getOutputs().size() != 1 )
						throw new Exception("Transaction Output count is not correct. Is: "+receivedTransaction.getOutputs().size()+" Should be: "+(channelTransaction.getOutputs().size()+1));
					
					List<TransactionOutput> clientList = receivedTransaction.getOutputs();
					List<TransactionOutput> serverList = channelTransaction.getOutputs();
					
					
					for(int i=0; i<paymentList.size(); i++) {
						TransactionOutput client = clientList.get(i+2);
						TransactionOutput server = serverList.get(i+2);
						payment = paymentList.get(i);
						if(client.getValue().value != server.getValue().value) {
							System.out.println(channelTransaction.toString());
							System.out.println(receivedTransaction.toString());
							throw new Exception("Value of an old payment does not match..");

						}

						if(!ScriptTools.checkPaymentScript(client, channel, keyWrapper, payment.getSecretHash(), Constants.SERVERSIDE, payment.paymentToServer ))
							throw new Exception("Payment Script is not correct..");
					}
				}
				/**
				 * As in the channel creation, the 
				 * 		1. output is the client change
				 * 		2. output is the server change
				 */
				if(receivedTransaction.getOutput(0).getValue().value > ( channel.getAmountClient() - m.amount) )
					throw new Exception("Client change is too high");
				if(receivedTransaction.getOutput(1).getValue().value != ( channel.getAmountServer() + Tools.getTransactionFees(Constants.SIZE_REVOKE_TX) ) ) 
					throw new Exception("Server change is not correct");
				
				/**
				 * On Serverside, the change(revoke) is
				 * 		1. client goes directly to his change address
				 * 		2. server goes to another multisig, between clientPubKey and serverPubKeyTemp
				 */
				if(!receivedTransaction.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
					throw new Exception("Revoke Script to Client does not pay to correct address.. Is: "+receivedTransaction.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString()+" Should Be: "+channel.getChangeAddressClient());
				if(!ScriptTools.checkRevokeScript(receivedTransaction.getOutput(1), channel, keyWrapper, Constants.SERVERSIDE))
					throw new Exception("Revoke Script to Server is not correct..");

				
				/**
				 * Check the added payment..
				 *   (1) is the amount correct?
				 *  		amount of the payment has to be the amount to be transferred 
				 *  			plus the fees for the depending transactions
				 *  		the fees must be sufficient, such that the largest of the depending
				 *  			transactions will confirm within a few blocks
				 *  		the largest transaction is the settlement transaction,
				 *  			as the hashed value R will be included, together with 
				 *  			two signatures, and it will pay out to one output
				 *  
				 *  TODO: find out the (probably constant) size of that output..
				 *  
				 *  
				 *   (2) the outputscript must be correct, paying either to
				 *   		multisig of clientMain + serverTemp 
				 *   			and a hashed value R
				 *   		
				 *   		or 
				 *   		
				 *   		multisig of clientMain + serverTemp(different one)
				 *   		
				 */
				
				TransactionOutput paymentOutput = receivedTransaction.getOutputs().get(receivedTransaction.getOutputs().size()-1);
				String secretHash = ScriptTools.getRofPaymentScript(paymentOutput);
				if(MySQLConnection.getPayment(conn, secretHash, channel.getId()) != null )
					throw new Exception("Secret Hash must be unique..");
				
				long amountShouldBe = m.amount + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX );
				if(paymentOutput.getValue().value < amountShouldBe)
					throw new Exception("New payment output value is not sufficient");
				
				if(!ScriptTools.checkPaymentScript(paymentOutput, channel, keyWrapper, null, Constants.SERVERSIDE, true))
					throw new Exception("Script of the added payment is not correct..");
				
				/**
				 * Set those keys from the list as used, that have been around in any of the Scripts.
				 */
				MySQLConnection.setKeysUsed(conn, keyWrapper);
				

				/**
				 * Sign it, such that we can give the hash to the client
				 * 	and make sure it spends our opening tx
				 */			
				ECDSASignature serverSig = Tools.getSignature(receivedTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getServerKeyOnServer());
				ECDSASignature clientSig = ScriptTools.getSignatureOufOfMultisigInput(receivedTransaction.getInput(0));
				Script inputScript = Tools.getMultisigInputScript(clientSig, serverSig);
				receivedTransaction.getInput(0).setScriptSig(inputScript);
				
				inputScript.correctlySpends(receivedTransaction, 0, channel.getOpeningTx().getOutput(0).getScriptPubKey());
				
				/**
				 * Check that the transaction fees are sufficient for the size of the channel
				 */
				int size = receivedTransaction.getMessageSize() + 71;

				if(!Tools.checkTransactionFees(size, receivedTransaction, channel.getOpeningTx().getOutput(0)))
					throw new Exception("Transaction fees for channel transaction is not correct");

				/**
				 * Validation complete..
				 * Create the channel transaction for the channel with the new payment included
				 */
				
				keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, false);
				
				Payment newPayment = new Payment(channel.getId(), receiverChannelId, m.amount, secretHash);
				newPayment.paymentToServer = true;
				paymentList.add(newPayment);
				
				/**
				 * Add the Change Revoke Payments
				 * Use '0' for the client change here and change it later when we have the otherwise complete tx.
				 */
				Transaction channelTransaction = new Transaction(Constants.getNetwork());
				channelTransaction.addOutput(Coin.valueOf(0), ScriptTools.getRevokeScript(keyWrapper, channel, Constants.CLIENTSIDE));
				channelTransaction.addOutput(Coin.valueOf(channel.getAmountServer()), channel.getChangeAddressServerAsAddress());
				

				
				for(Payment p : paymentList) {
					Script outputScript = ScriptTools.getPaymentScript(channel, keyWrapper, p.getSecretHash(), Constants.CLIENTSIDE, p.paymentToServer);
					long amount;
					if(p.paymentToServer) {
						amount = p.getAmount() + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX );
					} else {
						amount = p.getAmount() + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX ) - Tools.calculateServerFee(p.getAmount());
					}
					channelTransaction.addOutput(Coin.valueOf(amount), outputScript);
				}
				/**
				 * Set those keys from the list as used, that have been around in any of the Scripts.
				 */
				MySQLConnection.setKeysUsed(conn, keyWrapper);
				
				channelTransaction.addInput(channel.getOpeningTx().getOutput(0));
				
				/**
				 * Calculate the fees and set the client change accordingly.
				 */
				size = channelTransaction.getMessageSize() + 72 * 2;
				long sumOutputs = Tools.getCoinValueFromOutput(channelTransaction.getOutputs());
				long sumInputs = channel.getOpeningTx().getOutput(0).getValue().value;
				channelTransaction.getOutput(0).setValue(Coin.valueOf(sumInputs - sumOutputs - Tools.getTransactionFees(size)));
				
				/**
				 * Add our signature.
				 */
				serverSig = Tools.getSignature(channelTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getServerKeyOnServer());
				channelTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));
								
		
				/**
				 * Update our database with the new data.
				 */
				channel.setChannelTxClientTemp(channelTransaction);
				channel.setChannelTxServerTemp(receivedTransaction);
				channel.setPaymentPhase(2);
				
				MySQLConnection.updateChannel(conn, channel);
				MySQLConnection.addPayment(conn, newPayment);
				
				/**
				 * Fill the response to the client.
				 */
				SendPaymentResponseOne response = new SendPaymentResponseOne();
				response.channelHash = Tools.byteToString(receivedTransaction.getHash().getBytes());
				response.channelTransaction = Tools.byteToString(channelTransaction.bitcoinSerialize());
				
				responseContainer.success = true;
				responseContainer.type = Type.SEND_PAYMENT_ONE_RESPONSE;
				responseContainer.fill(response);
				
			} else if (message.type == Type.SEND_PAYMENT_TWO_REQUEST) {
				PerformanceLogger performance = new PerformanceLogger();

				/**
				 * Second Request for making a payment.
				 * 
				 * Request: 	Hash of the clientside channel tx
				 * 				Revoke tx of the server change in the serverside channel
				 * 				Array with settlements 	for each payment for the serverside channel
				 * 				Array with refunds 		for each payment for the serverside channel
				 * 
				 * Response: 	Revoke tx of the client change in the clientside channel
				 * 				Array with settlements 	for each payment for the clientside channel
				 * 				Array with refunds 		for each payment for the clientside channel
				 * 				Array with additionals	for each payment for the clientside channel
				 * 
				 * 				
				 */
				SendPaymentRequestTwo m = new Gson().fromJson(message.data, SendPaymentRequestTwo.class);
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				



				if(!channel.isReady()) 
					throw new Exception("Channel is not ready for payments..");
				if(channel.getPaymentPhase() != 2)
					throw new Exception("Something went wrong somewhere, start again at the beginning..");
				
				/**
				 * We got a 2 lists back with refunds and settlements tx for each payment.
				 * Make sure all of those are correct and spends the corresponding output of our channel.
				 * 	
				 */
				ArrayList<Transaction> refundList = new ArrayList<Transaction>();
				ArrayList<Transaction> settlementList = new ArrayList<Transaction>();

				for(String s : m.paymentRefunds) {
					refundList.add(new Transaction(Constants.getNetwork(), Tools.stringToByte(s)));
				}
				for(String s : m.paymentSettlements) {
					settlementList.add(new Transaction(Constants.getNetwork(), Tools.stringToByte(s)));
				}
				
				receivedTransaction = channel.getChannelTxClientTemp();
				Sha256Hash channelHash = channel.getChannelTxServerTemp().getHash();
				
				performance.measure("SEND_PAYMENT_TWO_REQUEST 1");
				
				/**
				 * Get all payments that are stored in the channel, with the most recent one aswell.
				 */
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannelWithMostRecent(conn, channel.getId());

				performance.measure("SEND_PAYMENT_TWO_REQUEST 2");

				
				for(int i=0; i<paymentList.size(); ++i) {
					payment = paymentList.get(i);
					Transaction refund = refundList.get(i);
					Transaction settlement = settlementList.get(i);
					
					if(refund.getInputs().size() != 1)
						throw new Exception("Refund Input Size is not 1");
					if(settlement.getInputs().size() != 1)
						throw new Exception("Settlement Input Size is not 1");

					
					if(refund.getOutputs().size() != 1)
						throw new Exception("Refund Output Size is not 1");
					if(settlement.getOutputs().size() != 1)
						throw new Exception("Settlement Output Size is not 1");

					
					if(!Tools.compareHash(refund.getInput(0).getOutpoint().getHash(), channelHash))
						throw new Exception("Refund Input Hash is not correct");
					if(!Tools.compareHash(settlement.getInput(0).getOutpoint().getHash(), channelHash))
						throw new Exception("Settlement Input Hash is not correct");

					
					if(refund.getInput(0).getOutpoint().getIndex() != (i+2))
						throw new Exception("Refund Input Index is not correct");			
					if(settlement.getInput(0).getOutpoint().getIndex() != (i+2))
						throw new Exception("Settlement Input Index is not correct");			


					if(refund.getOutput(0).getValue().value != payment.getAmount() )
						throw new Exception("Refund Output value not correct");
					if(settlement.getOutput(0).getValue().value != payment.getAmount() )
						throw new Exception("Settlement Output value not correct");
					

					if(payment.paymentToServer) {
						if(!refund.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
							throw new Exception("Refund Output Address is not correct");
						if(!settlement.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressServer()))
							throw new Exception("Settlement Output Address is not correct");
						if(!Tools.checkTransactionLockTime(refund, channel.getTimestampRefunds()))
							throw new Exception("Refund LockTime is not correct.");
					} else {
						if(!refund.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressServer()))
							throw new Exception("Refund Output Address is not correct");
						if(!settlement.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
							throw new Exception("Settlement Output Address is not correct");
						if(!Tools.checkTransactionLockTime(refund, payment.getTimestampCreated() + Constants.TIME_TO_REVEAL_SECRET))
							throw new Exception("Refund LockTime is not correct.");
					}
					
					if(Tools.checkSignature(refund, 0, receivedTransaction.getOutput(i+2), channel.getClientKeyOnServer(), refund.getInput(0).getScriptSig().getChunks().get(1).data))
						throw new Exception("Refund Client Signature is not correct");
					if(Tools.checkSignature(settlement, 0, receivedTransaction.getOutput(i+2), channel.getClientKeyOnServer(), refund.getInput(0).getScriptSig().getChunks().get(1).data))
						throw new Exception("Settlement Client Signature is not correct");
					
					if(payment.paymentToServer) {
						payment.setSettlementTxSenderTemp(settlement);
						payment.setRefundTxSenderTemp(refund);
					} else {
						payment.setSettlementTxReceiverTemp(settlement);
						payment.setRefundTxReceiverTemp(refund);
					}


					
				}
				performance.measure("SEND_PAYMENT_TWO_REQUEST 3");

				/**
				 * Check the revoke payment we got from the client.
				 * This payment shall be timelocked up until the channel ends.
				 * 
				 * TODO: Do an correctlySpends instead of the signature validation.
				 */
				Transaction revoke = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.revokeTransaction));
				
				if(revoke.getInputs().size() != 1)
					throw new Exception("Revoke Input Size is not 1");
				if(revoke.getOutputs().size() != 1)
					throw new Exception("Revoke Output Size is not 1");
				if(!Tools.compareHash(revoke.getInput(0).getOutpoint().getHash(), channelHash))
					throw new Exception("Revoke Input Hash is not correct");
				if(revoke.getInput(0).getOutpoint().getIndex() != 1)
					throw new Exception("Revoke Input Index is not correct");	
				if(  revoke.getOutput(0).getValue().value != channel.getAmountServer() )
					throw new Exception("Revoke Output value not correct");
				if(!Tools.checkTransactionLockTime(revoke, channel.getTimestampRefunds()))
					throw new Exception("Revoke TimeLock is not correct.");
					
//				if(Tools.checkSignature(revoke, 0, channelTransaction.getOutput(0), channel.getClientKeyOnServer(), revoke.getInput(0).getScriptSig().getChunks().get(1).data))
//					throw new Exception("Revoke Client Signature is not correct");

				
				/**
				 * Validation complete, all transactions we got from the client are correct..
				 * 
				 * Create the response to the client
				 */				
				
				Sha256Hash hash = new Sha256Hash(Tools.stringToByte(m.channelHash));
				
				Transaction revokeTransaction = new Transaction(Constants.getNetwork());
				revokeTransaction.addOutput(channel.getChannelTxClientTemp().getOutput(0).getValue().subtract(Coin.valueOf(Tools.getTransactionFees(Constants.SIZE_REVOKE_TX) )), channel.getChangeAddressClientAsAddress());
				revokeTransaction.addInput(hash, 0, Tools.getDummyScript());
				revokeTransaction = Tools.setTransactionLockTime(revokeTransaction, channel.getTimestampRefunds());
				
				/**
				 * Sign the revoke Transaction for the client
				 */
				ECDSASignature serverSig = Tools.getSignature(revokeTransaction, 0, channel.getChannelTxServerTemp().getOutput(1), channel.getServerKeyOnServer());
				revokeTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));
				
				
				
				/**
				 * Create the refunds and settlement transactions for each payment, 
				 * 	based on the hash we got from the server
				 */
				ArrayList<String> refundListToClient = new ArrayList<String>();
				ArrayList<String> settlementListToClient = new ArrayList<String>();
				ArrayList<String> addListToClient = new ArrayList<String>();
				
				performance.measure("SEND_PAYMENT_TWO_REQUEST 4");


				
				for(int i=0; i<paymentList.size(); ++i) {
					
					TransactionOutput output = channel.getChannelTxServerTemp().getOutput(i+2);
					payment = paymentList.get(i);
					
					Transaction refund = new Transaction(Constants.getNetwork());
					refund.addInput(hash, i+2, Tools.getDummyScript());
					refund.getInput(0).setSequenceNumber(0);
					
					if(payment.paymentToServer) {
						refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressClientAsAddress());
						refund.setLockTime(channel.getTimestampRefunds());
					} else {
						refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressServerAsAddress());
						refund.setLockTime(payment.getTimestampCreated() + Constants.TIME_TO_REVEAL_SECRET);
					}
					
					
					
					Transaction settlement = new Transaction(Constants.getNetwork());
					settlement.addInput(hash, i+2, Tools.getDummyScript());
					
					if(payment.paymentToServer) {
						settlement.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressServerAsAddress());
					} else {
						settlement.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressClientAsAddress());
					}
					
					/**
					 * The additional transactions needed to be cosigned with a temporary key of ours
					 */
					Transaction additional;
					ECKey key;
					if(payment.paymentToServer) {
						String pubKey = ScriptTools.getPubKeyOfPaymentScript(output, Constants.SERVERSIDE, true);
						key = MySQLConnection.getKey(conn, channel, pubKey);
						additional = payment.getRefundTxSenderTemp();
					} else {
						String pubKey = ScriptTools.getPubKeyOfPaymentScript(output, Constants.SERVERSIDE, false);
						key = MySQLConnection.getKey(conn, channel, pubKey);
						additional = payment.getSettlementTxReceiverTemp();
					}
					ECDSASignature signatureClient = ScriptTools.getSignatureOufOfMultisigInput(additional.getInput(0));
					ECDSASignature signature = Tools.getSignature(additional, 0, output, key);
					Script scriptSig = Tools.getMultisigInputScript(signatureClient, signature);
					additional.getInput(0).setScriptSig(scriptSig);
					
					
					
					
					serverSig = Tools.getSignature(refund, 0, output, channel.getServerKeyOnServer());
					refund.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));
					
					serverSig = Tools.getSignature(settlement, 0, output, channel.getServerKeyOnServer());
					settlement.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));

					
					refundListToClient.add(Tools.byteToString(refund.bitcoinSerialize()));
					settlementListToClient.add(Tools.byteToString(settlement.bitcoinSerialize()));
					addListToClient.add(Tools.byteToString(additional.bitcoinSerialize()));
					
					
				}
				
				performance.measure("SEND_PAYMENT_TWO_REQUEST 5");

				
				/**
				 * Update our database with the channel.
				 */
				
				channel.setChannelTxClientTemp(receivedTransaction);
				channel.setChannelTxRevokeClientTemp(revokeTransaction);
				channel.setChannelTxRevokeServerTemp(revoke);
				conn.setAutoCommit(false);
				MySQLConnection.updatePayment(conn, paymentList);
				
				performance.measure("SEND_PAYMENT_TWO_REQUEST 51");

				
				channel.setPaymentPhase(3);
				MySQLConnection.updateChannel(conn, channel);
				
				performance.measure("SEND_PAYMENT_TWO_REQUEST 52");

				
				/**
				 * Fill response for the client.
				 */
				SendPaymentResponseTwo response = new SendPaymentResponseTwo();
				
				response.paymentAdditionals = addListToClient;
				response.paymentRefunds = refundListToClient;
				response.paymentSettlements = settlementListToClient;
				response.revokeTransaction = Tools.byteToString(revokeTransaction.bitcoinSerialize());
				
				responseContainer.success = true;
				responseContainer.type = Type.SEND_PAYMENT_TWO_RESPONSE;
				
				performance.measure("SEND_PAYMENT_TWO_REQUEST 53");

				
				responseContainer.fill(response);
				
				performance.measure("SEND_PAYMENT_TWO_REQUEST 6");


				
			} else if (message.type == Type.SEND_PAYMENT_THREE_REQUEST) {
				/**
				 * Third Request for establishing the channel.
				 * 
				 * Request: 	Array with keys, that were used for the old channel before
				 * 				Array with new keys, to be used with the next payment
				 * 
				 * Response: 	Array with keys, that were used for the old channel before
				 * 				Array with new keys, to be used with the next payment
				 * 
				 * After verifying, the server will disclose his keys to the client
				 */

				PerformanceLogger performance = new PerformanceLogger();
				
				SendPaymentResponseThree m = new Gson().fromJson(message.data, SendPaymentResponseThree.class);
				
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
				
				if(!channel.isReady()) 
					throw new Exception("Channel is not ready for payments..");
				if(channel.getPaymentPhase() != 3)
					throw new Exception("Something went wrong somewhere, start again at the beginning..");
				
				performance.measure("Payment Request Three 1");
				
				/**
				 * Check if the keys are provided and save them..
				 */	
				if(m.keyList == null)
					throw new Exception("keyList is null");
				if(m.keyList.size() == 0)
					throw new Exception("keyList is empty");
				
				/**
				 * Helper Class to check all the different parameters from the new keys.
				 */
				MySQLConnection.checkKeysFromOtherSide(conn, channel, m.keyList);
				
				performance.measure("Payment Request Three 2");


				
				/**
				 * At this point, the payment is complete for us, wait for REQUEST_FOUR, such that the client is okay aswell.
				 */
				channel.setPaymentPhase(4);
				MySQLConnection.updateChannel(conn, channel);
				conn.commit();

				
				/**
				 * Fill response for the client.
				 */
				SendPaymentRequestThree response = new SendPaymentRequestThree();
				response.keyList = MySQLConnection.getKeysOfUsToBeExposed(conn, channel, false);
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.SEND_PAYMENT_THREE_RESPONSE;
				
				performance.measure("Payment Request Three 3");

				
			} else if (message.type == Type.SEND_PAYMENT_FOUR_REQUEST) {
				/**
				 * Fourth Request for establishing the channel.
				 * 
				 * This means the client is okay with the data we sent, the payment is final now.
				 */
				
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
				
				if(!channel.isReady()) 
					throw new Exception("Channel is not ready for payments..");
				if(channel.getPaymentPhase() != 4)
					throw new Exception("Something went wrong somewhere, start again at the beginning..");

				
				/**
				 * At this point, the payment is complete for us, so we should swap out the transactions
				 * 		with the temporary ones..
				 */
				MySQLConnection.getKeysOfUsToBeExposed(conn, channel, true);

//				MySQLConnection.deleteUnusedAndExposedKeysFromUs(conn, channel);
//				MySQLConnection.deleteUnusedKeyFromOtherSide(conn, channel);
				
				channel.replaceCurrentTransactionsWithTemporary();
				channel.setPaymentPhase(0);
				MySQLConnection.updateChannel(conn, channel);
				
				
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
				Payment newPayment = MySQLConnection.getPaymentMostRecentSent(conn, channel.getId());
				
				newPayment.setIncludeInSenderChannel(true);
				newPayment.setPhase(1);
				
				paymentList.add(newPayment);

				for(Payment p : paymentList) {
					p.replaceCurrentTransactionsWithTemporary();
				}
				MySQLConnection.updatePayment(conn, paymentList);
				
				int lowestTimestamp = channel.getTimestampRefunds();
				for(Payment p : paymentList) {
					if(!p.paymentToServer) {
						if(p.getTimestampAddedToReceiver() + Constants.TIME_TO_REVEAL_SECRET < lowestTimestamp)
							lowestTimestamp = p.getTimestampAddedToReceiver() + Constants.TIME_TO_REVEAL_SECRET;
					}
				}
				channel.setTimestampForceClose(lowestTimestamp);
				TransactionStorage.instance.onChannelChanged(channel);
				
//				System.out.println("Payment "+newPayment.getSecretHash()+" is final!");

				/**
				 * Fill the (empty) response to the client.
				 */
				SendPaymentRequestFour response = new SendPaymentRequestFour();
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.SEND_PAYMENT_FOUR_RESPONSE;

                /**
                 * For the real-time updates, we push this payment towards the receiving channel
                 *  if he is online currently..
                 */
				WebSocketHandler.sendPayment(newPayment.getChannelIdReceiver(), newPayment);
				
			} else if (message.type == Type.ADD_KEYS_REQUEST) {
				long time = System.currentTimeMillis();
				long time1 = System.currentTimeMillis();

				/**
				 * Request for new keys.
				 * 
				 * Request: 	Array with temp-keys, that should be added 
				 * 
				 * Response: 	Array with temp-keys, that should be added 
				 * 
				 */
				AddKeysRequest m = new Gson().fromJson(message.data, AddKeysRequest.class);
								
				/**
				 * Check if the keys are provided and save them..
				 */	
				if(m.keyList == null)
					throw new Exception("keyList is null");
				if(m.keyList.size() == 0)
					throw new Exception("keyList is empty");

				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
//				time1 = System.currentTimeMillis();
//				System.out.println("Add Key Request 1: "+(time1-time) );
//				time = System.currentTimeMillis();
				
				/**
				 * Clean our keylist first, such that only these new keys are used now.
				 */
//				MySQLConnection.deleteUnusedAndExposedKeysFromUs(conn, channel);
//				MySQLConnection.deleteUnusedKeyFromOtherSide(conn, channel);
				
				
				MySQLConnection.addKey(conn, m.keyList, channel.getId(), false);

//				time1 = System.currentTimeMillis();
//				System.out.println("Add Key Request 2: "+(time1-time) );
//				time = System.currentTimeMillis();

				SendPaymentRequestThree response = new SendPaymentRequestThree();
				response.keyList = MySQLConnection.createKeys(conn, channel, m.amount);
				
//				time1 = System.currentTimeMillis();
//				System.out.println("Add Key Request 3: "+(time1-time) );
//				time = System.currentTimeMillis();
				/**
				 * We definitely don't want to send the private keys.
				 */
				for(Key key : response.keyList) {
					key.privateKey = null;
				}


				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.ADD_KEYS_RESPONSE;
				
//				time1 = System.currentTimeMillis();
//				System.out.println("Add Key Request 4: "+(time1-time) );
//				time = System.currentTimeMillis();
				
				
			} else if (message.type == Type.UPDATE_CHANNEL_ONE_REQUEST) {
				/**
				 * First Request for updating the channel.
				 * 
				 * Request: 	Empty.
				 * 
				 * Response: 	Amount of current payments
				 * 				Amount of new payments that will be added to the channel
				 * 				Amount of payments that timed out and will be removed (TODO: not used currently..)
				 * 
				 * The client must start here and send us unused keys afterwards.
				 */
				channel = MySQLConnection.getChannel(conn, message.pubkey);

				UpdateChannelResponseOne response = new UpdateChannelResponseOne();
				response.amountCurrentPayments = MySQLConnection.getCurrentPaymentsAmount(conn, channel);
				response.amountNewPayments = MySQLConnection.getPaymentsAmount(conn, channel, 1);
				
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.UPDATE_CHANNEL_ONE_RESPONSE;
				
				channel.setPaymentPhase(12);
				MySQLConnection.updateChannel(conn, channel);
				
			} else if (message.type == Type.UPDATE_CHANNEL_TWO_REQUEST) {
				/**
				 * Second Request for updating the channel.
				 * 
				 * Request: 	Array with new keys to be included in the clientside channel transaction.
				 * 				Array with secretHashes of payments, that will be removed.
				 * 				Array with secrets of payments that are currently included in the channel
				 * 					for payments towards the client.
				 * 					These should get settled with this update.
				 * 
				 * Response: 	Array with new keys to be included in the serverside channel transaction.
				 * 				Array with secretHashes of payments, that will be removed.
				 * 				Array with secrets of payments that are currently included in the channel
				 * 					for payments towards the server, that has been confirmed by the receiving side.
				 * 					These should get settled with this update.
				 * 				A new channel transactions.
				 * 
				 */
				UpdateChannelRequestTwo m = new Gson().fromJson(message.data, UpdateChannelRequestTwo.class);
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
//				if(channel.getPaymentPhase() != 12)
//					throw new Exception("Something went wrong somewhere, start again at the beginning..");
				
				/**
				 * Clean our keylist first, such that only these new keys are used now.
				 */
//				MySQLConnection.deleteUnusedAndExposedKeysFromUs(conn, channel);
//				MySQLConnection.deleteUnusedKeyFromOtherSide(conn, channel);
				
				MySQLConnection.addKey(conn, m.keyList, channel.getId(), false);
				
				ArrayList<String> deletedPayments = new ArrayList<String>();
				
				
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
				ArrayList<Secret> secretsForSentPayments = new ArrayList<Secret>();
				
				long addAmountToServer = 0;
				ArrayList<Payment> paymentsToUpdate = new ArrayList<Payment>();
				ArrayList<Payment> paymentsForUpdatedChannel = new ArrayList<Payment>();
				ArrayList<Payment> paymentsForUpdatedChannelTemp = new ArrayList<Payment>();

				/**
				 * Payments that are in the channel already and that has been sent by this client.
				 * If we have the secret, it means this payment should be settled, and we have to 
				 * 		add up the amount to our balance.
				 */
				for(Payment p : paymentList) {
					if(p.paymentToServer) {
						if(p.getSecret() != null) {
							secretsForSentPayments.add(new Secret(p.getSecretHash(), p.getSecret()));
							addAmountToServer += p.getAmount();
						}
					}
				}
				
				/**
				 * Payments that should not be included in the channel anymore, because the receiver canceled it.
				 * 
				 * Payments that were released by the current client are included in removedPayments list.
				 * As removedPayments is temporary only, we will set the phase to 5, such that we can check it later.
				 * 
				 * Before finishing the updateChannel, this can be undone by starting a new updateChannel session 
				 * 		and not include the payment in removedPayments.
				 */
				for(Payment p : paymentList) {
					if(p.paymentToServer) {
						if(Tools.currentTime() - p.getTimestampCreated() < Constants.TIME_TOTAL_PAYMENT && p.getPhase() != 6 ) {
							paymentsForUpdatedChannelTemp.add(p);
						} else {
							deletedPayments.add(p.getSecretHash());
						}
					} else {
						boolean brk = false;
						for(String hash : m.removedPayments) {
							if(hash.equals(p.getSecretHash())) {
								p.setPhase(5);
								MySQLConnection.updatePayment(conn, p);
								brk=true;
								break;
							}
						}
						if(brk) continue;
						if(p.getPhase() == 5) {
							p.setPhase(2);
						}
						paymentsForUpdatedChannelTemp.add(p);

					}
				}
				paymentList = paymentsForUpdatedChannelTemp;
				
				/**
				 * Payments that are in the channel already, that the client should receive.
				 * If he has produced the correct secret, the amount of the payment should be
				 * 		deducted from our balance.
				 */
                Set<Integer> receiverSet = new LinkedHashSet<>();
				for(Secret secret : m.secretList) {
					if(!secret.verify())
						throw new Exception("Secret does not hash to correct value");
					
					for(Payment p : paymentList) {
						if(!p.paymentToServer) {
							if(p.getSecretHash().equals(secret.secretHash)) {
								/**
								 * A secret has been exposed for a payment towards this client
								 */
								addAmountToServer -= p.getAmount();
								addAmountToServer += Tools.calculateServerFee(p.getAmount());
								p.setSecret(secret.secret);
								paymentsToUpdate.add(p);
                                receiverSet.add(p.getChannelIdSender());
								break;
							}
						}
					}
				}	
				
				MySQLConnection.updatePayment(conn, paymentList);

                for(Integer sendingChannelId : receiverSet)
                    WebSocketHandler.newSecret(sendingChannelId);
				
				/**
				 * Add all payments to the channel, that have no secret attached
				 */
				for(Payment p : paymentList) {
					if(p.getSecret() == null) {
						paymentsForUpdatedChannel.add(p);
					}
				}
				
				/**
				 * Validation complete..
				 * Create the channel transaction for the channel with the new payment included
				 */
				
				KeyWrapper keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, false);
				
				/**
				 * Calculate amount of total payments we can include with the amount of keys we have
				 */
				int amountOfTotalPayments = (keyWrapper.getKeyList().size() - 1) / Constants.KEYS_PER_PAYMENT_CLIENTSIDE;
				int amountOfAdditionalPayments = amountOfTotalPayments - ( paymentsForUpdatedChannel.size() );
				
				/**
				 * Grab additional payments out of the database, that aren't yet included
				 */
				ArrayList<Payment> newPayments = MySQLConnection.getPaymentsIncludedInChannelWithPaymentsNotAddedYet(conn, channel.getId(), amountOfAdditionalPayments);
				for(Payment p : newPayments) {
					paymentsForUpdatedChannel.add(p);
				}
				
				/**
				 * For later reference, order the payments in order of their id in our database.
				 */
				paymentsForUpdatedChannel.sort(new Comparator<Payment>() {
					public int compare(Payment arg0, Payment arg1) {
						return arg0.getId()-arg1.getId();
					}
				});
				
				/**
				 * Calculate the change for the server.
				 * We have to substract the amounts for every payment to the client.
				 */
				for(Payment p : paymentsForUpdatedChannel) {
					if(!p.paymentToServer) {
						addAmountToServer -= p.getAmount() - Tools.calculateServerFee(p.getAmount());
					}
				}
				
				/**
				 * Create the channel transaction for the client
				 * Add the Change Revoke Payments
				 * Use '0' for the client change here and change it later when we have the otherwise complete tx.
				 */
				Transaction channelTransaction = new Transaction(Constants.getNetwork());
				channelTransaction.addOutput(Coin.valueOf(0), ScriptTools.getRevokeScript(keyWrapper, channel, Constants.CLIENTSIDE));
				channelTransaction.addOutput(Coin.valueOf( channel.getAmountServer() + addAmountToServer), channel.getChangeAddressServerAsAddress());
				channelTransaction.addInput(channel.getOpeningTx().getOutput(0));

				/**
				 * Add all payments
				 */
				for(Payment p : paymentsForUpdatedChannel) {
					Script outputScript = ScriptTools.getPaymentScript(channel, keyWrapper, p.getSecretHash(), Constants.CLIENTSIDE, p.paymentToServer);
					long amount;
					if(p.paymentToServer) {
						amount = p.getAmount() + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX );
					} else {
						amount = p.getAmount() + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX ) - Tools.calculateServerFee(p.getAmount());
					}
					channelTransaction.addOutput(Coin.valueOf(amount), outputScript);
				}
				/**
				 * Set those keys from the list as used, that have been around in any of the Scripts.
				 */
				MySQLConnection.setKeysUsed(conn, keyWrapper);
				
				/**
				 * Calculate the fees and set the client change accordingly.
				 */
				int size = channelTransaction.getMessageSize() + 72 * 2;
				long sumOutputs = Tools.getCoinValueFromOutput(channelTransaction.getOutputs());
				long sumInputs = channel.getOpeningTx().getOutput(0).getValue().value;
				channelTransaction.getOutput(0).setValue(Coin.valueOf(sumInputs - sumOutputs - Tools.getTransactionFees(size)));
				
				/**
				 * Add our signature.
				 */
				ECDSASignature serverSig = Tools.getSignature(channelTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getServerKeyOnServer());
				channelTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));

				
				/**
				 * Fill the response to the client
				 */
				UpdateChannelResponseTwo response = new UpdateChannelResponseTwo();
				response.channelTransaction = Tools.byteToString(channelTransaction.bitcoinSerialize());
				response.secretList = secretsForSentPayments;
//				response.keyList = MySQLConnection.createKeys(conn, channel, (paymentsForUpdatedChannel.size() * Constants.KEYS_PER_PAYMENT_SERVERSIDE) + 1);
				response.keyList = MySQLConnection.createKeys(conn, channel, 1);
				response.removedPayments = deletedPayments;
								
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.UPDATE_CHANNEL_TWO_RESPONSE;
				
				channel.setPaymentPhase(13);
				channel.setChannelTxClientTemp(channelTransaction);
				MySQLConnection.updatePayment(conn, paymentList);
				MySQLConnection.updateChannel(conn, channel);
			} else if (message.type == Type.UPDATE_CHANNEL_THREE_REQUEST) {
				/**
				 * Third Request for updating the channel.
				 * 
				 * Request: 	A new channel transaction
				 * 
				 * Response: 	Hash of the now received transaction
				 * 				
				 */
				UpdateChannelRequestThree m = new Gson().fromJson(message.data, UpdateChannelRequestThree.class);
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				if(channel.getPaymentPhase() != 13)
					throw new Exception("Something went wrong somewhere, start again at the beginning..");
				
				/**
				 * Just allow one input for now, the one of the multisig
				 */
				receivedTransaction = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.channelTransaction));
				Transaction channelTransaction = channel.getChannelTxClientTemp();

				if(receivedTransaction.getInputs().size() != 1) 
					throw new Exception("Transaction input count is not 1");
				
				TransactionInput input = receivedTransaction.getInput(0);
				TransactionOutPoint outpoint = input.getOutpoint();
				if(!Tools.compareHash(outpoint.getHash(), channel.getOpeningTx().getHash()) || outpoint.getIndex() != 0)
					throw new Exception("Transaction input does not point to the opening transaction");
				
				
				/**
				 * Check the outputs of the transaction that was sent to us
				 * 
				 *  (1) It should pay the client back the correct amount
				 *  (2) It should pay us the correct amount back
				 *  (3) The next N outputs must match the outputs of the channel tx we have on file
				 *  		the value must be the same
				 *  		the keys used must be unused keys from me				 *  		
				 *  (4) The last output must point to the new payment
				 *  

				 *  
				 *  Compare the received transaction with the tx we have on file,
				 *  	the payments must be in the same order.
				 */
				KeyWrapper keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, true);
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsForUpdatingChannelOrdered(conn, channel.getId(), channelTransaction.getOutputs().size()-2);


				
				if(receivedTransaction.getOutputs().size() != channelTransaction.getOutputs().size() )
					throw new Exception("Transaction Output count is not correct.");
				
				List<TransactionOutput> clientList = receivedTransaction.getOutputs();
				List<TransactionOutput> serverList = channelTransaction.getOutputs();
				
				
				for(int i=2; i<serverList.size(); ++i) {
					TransactionOutput client = clientList.get(i);
					TransactionOutput server = serverList.get(i);
					payment = paymentList.get(i-2);
					
					
					if(client.getValue().value != server.getValue().value)
						throw new Exception("Value of an old payment does not match..");

					if(!ScriptTools.checkPaymentScript(client, channel, keyWrapper, payment.getSecretHash(), Constants.SERVERSIDE, payment.paymentToServer ))
						throw new Exception("Payment Script is not correct..");
				}

				/**
				 * As in the channel creation, the 
				 * 		1. output is the client change
				 * 		2. output is the server change
				 */
				if(receivedTransaction.getOutput(0).getValue().value > channelTransaction.getOutput(0).getValue().value )
					throw new Exception("Client change is too high");
				if(receivedTransaction.getOutput(1).getValue().value < ( channelTransaction.getOutput(1).getValue().value + Tools.getTransactionFees(Constants.SIZE_REVOKE_TX) )  )
					throw new Exception("Server change is too low");
				
				/**
				 * On Serverside, the change(revoke) is
				 * 		1. client goes directly to his change address
				 * 		2. server goes to another multisig, between clientPubKey and serverPubKeyTemp
				 */
				if(!receivedTransaction.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
					throw new Exception("Revoke Script to Client does not pay to correct address.. Is: "+receivedTransaction.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString()+" Should Be: "+channel.getChangeAddressClient());
				if(!ScriptTools.checkRevokeScript(receivedTransaction.getOutput(1), channel, keyWrapper, Constants.SERVERSIDE))
					throw new Exception("Revoke Script to Server is not correct..");
				
				/**
				 * Sign it, such that we can give the hash to the client
				 * 	and make sure it spends our opening tx
				 */			
				ECDSASignature serverSig = Tools.getSignature(receivedTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getServerKeyOnServer());
				ECDSASignature clientSig = ScriptTools.getSignatureOufOfMultisigInput(receivedTransaction.getInput(0));
				Script inputScript = Tools.getMultisigInputScript(clientSig, serverSig);
				receivedTransaction.getInput(0).setScriptSig(inputScript);
				
				inputScript.correctlySpends(receivedTransaction, 0, channel.getOpeningTx().getOutput(0).getScriptPubKey());
				
				
				
				/**
				 * Fill the response to the client
				 */
				UpdateChannelResponseThree response = new UpdateChannelResponseThree();
				response.transactionHash = Tools.byteToString(receivedTransaction.getHash().getBytes());
			
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.UPDATE_CHANNEL_THREE_RESPONSE;
				
				/**
				 * Update our database
				 */
				channel.setPaymentPhase(14);
				channel.setChannelTxServerTemp(receivedTransaction);
				MySQLConnection.updateChannel(conn, channel);
				MySQLConnection.setKeysUsed(conn, keyWrapper);
				
			} else if (message.type == Type.UPDATE_CHANNEL_FOUR_REQUEST) {
				/**
				 * Third Request for updating the channel.
				 * 
				 * Request: 	Revoke tx of the server change in the serverside channel
				 * 				Array with settlements 	for each payment for the serverside channel
				 * 				Array with refunds 		for each payment for the serverside channel
				 * 				Hash of the clientside channel
				 * 
				 * Response: 	Revoke tx of the client change in the clientside channel
				 * 				Array with settlements 	for each payment for the clientside channel
				 * 				Array with refunds 		for each payment for the clientside channel
				 * 				Array with additionals	for each payment for the clientside channel
				 * 				
				 */
				UpdateChannelRequestFour m = new Gson().fromJson(message.data, UpdateChannelRequestFour.class);
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
				if(channel.getPaymentPhase() != 14)
					throw new Exception("Something went wrong somewhere, start again at the beginning..");


				
				/**
				 * We got a 2 lists back with refunds and settlements tx for each payment.
				 * Make sure all of those are correct and spends the corresponding output of our channel.
				 * 	
				 */
				ArrayList<Transaction> refundList = new ArrayList<Transaction>();
				ArrayList<Transaction> settlementList = new ArrayList<Transaction>();

				for(String s : m.paymentRefunds) {
					refundList.add(new Transaction(Constants.getNetwork(), Tools.stringToByte(s)));
				}
				for(String s : m.paymentSettlements) {
					settlementList.add(new Transaction(Constants.getNetwork(), Tools.stringToByte(s)));
				}
				
				receivedTransaction = channel.getChannelTxClientTemp();
				Sha256Hash channelHash = channel.getChannelTxServerTemp().getHash();
				
				
				/**
				 * Get all payments that should be in this channel.
				 */
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsForUpdatingChannelOrdered(conn, channel.getId(), receivedTransaction.getOutputs().size()-2);

				
				for(int i=0; i<paymentList.size(); ++i) {
					payment = paymentList.get(i);
					Transaction refund = refundList.get(i);
					Transaction settlement = settlementList.get(i);
					
					if(payment.paymentToServer) {
						ScriptTools.checkTransaction(refund, (i+2), channelHash, payment.getAmount(), channel.getChangeAddressClient(), channel.getTimestampRefunds());
						ScriptTools.checkTransaction(settlement, (i+2), channelHash, payment.getAmount(), channel.getChangeAddressServer(), 0);

					} else {
						int time = payment.getTimestampAddedToReceiver();
						if(time == 0) time = Tools.currentTime() + Constants.TIME_TO_REVEAL_SECRET;
						ScriptTools.checkTransaction(refund, (i+2), channelHash, payment.getAmount(), channel.getChangeAddressServer(), time);
						ScriptTools.checkTransaction(settlement, (i+2), channelHash, payment.getAmount(), channel.getChangeAddressClient(), 0);
						
					}
					
					if(Tools.checkSignature(refund, 0, receivedTransaction.getOutput(i+2), channel.getClientKeyOnServer(), refund.getInput(0).getScriptSig().getChunks().get(1).data))
						throw new Exception("Refund Client Signature is not correct");
					if(Tools.checkSignature(settlement, 0, receivedTransaction.getOutput(i+2), channel.getClientKeyOnServer(), refund.getInput(0).getScriptSig().getChunks().get(1).data))
						throw new Exception("Settlement Client Signature is not correct");
					
					if(payment.paymentToServer) {
						payment.setSettlementTxSenderTemp(settlement);
						payment.setRefundTxSenderTemp(refund);
					} else {
						payment.setSettlementTxReceiverTemp(settlement);
						payment.setRefundTxReceiverTemp(refund);
					}
					
				}
				/**
				 * Check the revoke payment we got from the client.
				 * This payment shall be timelocked up until the channel ends.
				 * 
				 * TODO: Do an correctlySpends instead of the signature validation.
				 */
				Transaction revoke = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.revokeTransaction));
				ScriptTools.checkTransaction(revoke, 1, channelHash, channel.getChannelTxClientTemp().getOutput(1).getValue().value, channel.getChangeAddressServer(), channel.getTimestampRefunds());

//				if(Tools.checkSignature(revoke, 0, channelTransaction.getOutput(0), channel.getClientKeyOnServer(), revoke.getInput(0).getScriptSig().getChunks().get(1).data))
//					throw new Exception("Revoke Client Signature is not correct");
				
				
				
				
				

				

				/**
				 * Verification complete, produce the arrays of settlements/refunds/additionals and the revoke tx
				 *
				 * Create the refunds and settlement transactions for each payment, 
				 * 	based on the hash we got from the client
				 */
				Sha256Hash hash = new Sha256Hash(Tools.stringToByte(m.transactionHash));
				ArrayList<String> refundListToClient = new ArrayList<String>();
				ArrayList<String> settlementListToClient = new ArrayList<String>();
				ArrayList<String> addListToClient = new ArrayList<String>();
				

				
				ECDSASignature serverSig;
				for(int i=0; i<paymentList.size(); ++i) {
					
					TransactionOutput output = channel.getChannelTxServerTemp().getOutput(i+2);
					payment = paymentList.get(i);
					
					Transaction settlement = new Transaction(Constants.getNetwork());
					settlement.addInput(hash, i+2, Tools.getDummyScript());

					Transaction refund = new Transaction(Constants.getNetwork());
					refund.addInput(hash, i+2, Tools.getDummyScript());
					refund.getInput(0).setSequenceNumber(0);
					

					Transaction additional;
					ECKey key;
					
					if(payment.paymentToServer) {
						refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressClientAsAddress());
						refund.setLockTime(channel.getTimestampRefunds());
						settlement.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressServerAsAddress());
						
						String pubKey = ScriptTools.getPubKeyOfPaymentScript(output, Constants.SERVERSIDE, true);
						key = MySQLConnection.getKey(conn, channel, pubKey);
						additional = payment.getRefundTxSenderTemp();
					} else {
						refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressServerAsAddress());
						refund.setLockTime(channel.getTimestampRefunds());
						settlement.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressClientAsAddress());
						
						String pubKey = ScriptTools.getPubKeyOfPaymentScript(output, Constants.SERVERSIDE, false);
						key = MySQLConnection.getKey(conn, channel, pubKey);
						additional = payment.getSettlementTxReceiverTemp();
					}

					/**
					 * The additional transactions needed to be cosigned with a temporary key of ours
					 */
					ECDSASignature signatureClient = ScriptTools.getSignatureOufOfMultisigInput(additional.getInput(0));
					ECDSASignature signatureServer = Tools.getSignature(additional, 0, output, key);
					Script scriptSig = Tools.getMultisigInputScript(signatureClient, signatureServer);
					additional.getInput(0).setScriptSig(scriptSig);					
					
					/**
					 * Sign the refund and settlement transactions with our main key
					 */
					serverSig = Tools.getSignature(refund, 0, output, channel.getServerKeyOnServer());
					refund.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));
					
					serverSig = Tools.getSignature(settlement, 0, output, channel.getServerKeyOnServer());
					settlement.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));

					/**
					 * Add these transactions to the lists that we will fill into the response.
					 */
					refundListToClient.add(Tools.byteToString(refund.bitcoinSerialize()));
					settlementListToClient.add(Tools.byteToString(settlement.bitcoinSerialize()));
					addListToClient.add(Tools.byteToString(additional.bitcoinSerialize()));
					
					
				}
				
				/**
				 * Create the new revoke Transaction				
				 */
				Transaction revokeTransaction = new Transaction(Constants.getNetwork());
				revokeTransaction.addOutput(channel.getChannelTxClientTemp().getOutput(0).getValue().subtract(Coin.valueOf(Tools.getTransactionFees(Constants.SIZE_REVOKE_TX) )), channel.getChangeAddressClientAsAddress());
				revokeTransaction.addInput(hash, 0, Tools.getDummyScript());
				revokeTransaction = Tools.setTransactionLockTime(revokeTransaction, channel.getTimestampRefunds());
				
				/**
				 * Sign the revoke Transaction for the client
				 */
				serverSig = Tools.getSignature(revokeTransaction, 0, channel.getChannelTxServerTemp().getOutput(1), channel.getServerKeyOnServer());
				revokeTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(serverSig));
				
				
				
				/**
				 * Fill the response to the client
				 */
				UpdateChannelResponseFour response = new UpdateChannelResponseFour();
				response.paymentAdditionals = addListToClient;
				response.paymentRefunds = refundListToClient;
				response.paymentSettlements = settlementListToClient;
				response.revokeTransaction = Tools.byteToString(revokeTransaction.bitcoinSerialize());
			
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.UPDATE_CHANNEL_FOUR_RESPONSE;
				
				/**
				 * Update our database
				 */
				channel.setPaymentPhase(15);
				channel.setChannelTxRevokeClientTemp(revokeTransaction);
				channel.setChannelTxRevokeServerTemp(revoke);
				MySQLConnection.updateChannel(conn, channel);
				
			} else if (message.type == Type.UPDATE_CHANNEL_FIVE_REQUEST) {
				/**
				 * Fourth Request for updating the channel.
				 * 
				 * Request: 	Array with keys of the old channel
				 * 
				 * Response: 	Array with keys of the old channel
				 * 				
				 */

				UpdateChannelRequestFive m = new Gson().fromJson(message.data, UpdateChannelRequestFive.class);
				channel = MySQLConnection.getChannel(conn, message.pubkey);
				
				if(channel.getPaymentPhase() != 15)
					throw new Exception("Something went wrong somewhere, start again at the beginning..");
				


				
				
				/**
				 * Helper Class to check the new keys.
				 */
				if(channel.getChannelTxClient() != null) {
					MySQLConnection.checkKeysFromOtherSide(conn, channel, m.keyList);
				}
				/**
				 * Fill response to client
				 */
				UpdateChannelResponseFive response = new UpdateChannelResponseFive();
				response.keyList = MySQLConnection.getKeysOfUsToBeExposed(conn, channel, true);				
			
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.UPDATE_CHANNEL_FOUR_RESPONSE;
				
				/**
				 * Update complete.
				 * We should update our database accordingly:
				 * 		- Change the amount values in the database
				 * 		- Swap the TXIDs of all payments and of the channel
				 * 		- Change phase=5 into phase=6
				 */
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsForUpdatingChannelOrdered(conn, channel.getId(), channel.getChannelTxServerTemp().getOutputs().size() - 2);
				ArrayList<Payment> oldPayments = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
				ArrayList<Payment> oldPaymentsToUpdate = new ArrayList<Payment>();

				
				long addAmountToServer = 0;
				long addAmountToClient = 0;
				/**
				 * Find out which payments got removed from the channel with this update and 
				 * 		add these amounts.
				 * 
				 * Payments with phase 6 has been refunded by the receiver.
				 * Payments with phase 5 has been marked as refunded at the beginning of this updateChannel.
				 * 
				 * Both should be removed from this channel without changing the actual amounts.
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
							if(p1.getPhase() != 6) { 
								addAmountToServer+=p1.getAmount();
								addAmountToClient-=p1.getAmount();
							}
							p1.setIncludeInSenderChannel(false);

						} else {
							if(p1.getPhase() != 5 ) {
								addAmountToClient+=p1.getAmount() - Tools.calculateServerFee(p1.getAmount());;
								addAmountToServer-=p1.getAmount() - Tools.calculateServerFee(p1.getAmount());
							} else {
								p1.setPhase(6);
							}
							p1.setIncludeInReceiverChannel(false);
						}
						oldPaymentsToUpdate.add(p1);
					}
				}
				
//				System.out.println("Update complete! New channel transaction on server: ");
//				System.out.println(channel.getChannelTxServerTemp().toString());
				
				channel.setAmountClient(channel.getAmountClient() + addAmountToClient);
				channel.setAmountServer(channel.getAmountServer() + addAmountToServer);
				
				for(Payment p : paymentList) {
					if(p.paymentToServer) {
						p.setIncludeInSenderChannel(true);
					} else {
						if(p.getTimestampAddedToReceiver()==0) p.setTimestampAddedToReceiver(Tools.currentTime());
						p.setPhase(2);
						p.setIncludeInReceiverChannel(true);
					}
					p.replaceCurrentTransactionsWithTemporary();
				}
				channel.replaceCurrentTransactionsWithTemporary();
				
				int lowestTimestamp = channel.getTimestampRefunds();
				for(Payment p : paymentList) {
					if(!p.paymentToServer) {
						if(p.getTimestampAddedToReceiver() + Constants.TIME_TO_REVEAL_SECRET < lowestTimestamp)
							lowestTimestamp = p.getTimestampAddedToReceiver() + Constants.TIME_TO_REVEAL_SECRET;
					}
				}
				channel.setTimestampForceClose(lowestTimestamp);
				
				TransactionStorage.instance.onChannelChanged(channel);
				
				
				MySQLConnection.updatePayment(conn, paymentList);
				MySQLConnection.updatePayment(conn, oldPaymentsToUpdate);
				MySQLConnection.updatePaymentRefunds(conn, channel);

				
				channel.setPaymentPhase(0);
				MySQLConnection.updateChannel(conn, channel);
				
				MySQLConnection.deleteUnusedAndExposedKeysFromUs(conn, channel);
				MySQLConnection.deleteUnusedKeyFromOtherSide(conn, channel);
				
				
			}	 else if (message.type == Type.CLOSE_CHANNEL_REQUEST) {
				/**
				 * Request for closing the channel.
				 * 
				 * Request: 	The channel transaction
				 * 
				 * Response: 	
				 * 				
				 */
				CloseChannelRequest m = new Gson().fromJson(message.data, CloseChannelRequest.class);
				channel = MySQLConnection.getChannel(conn, message.pubkey);

				
				
				
				/**
				 * Just allow one input for now, the one of the multisig
				 */
				receivedTransaction = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.channelTransaction));
				

				if(receivedTransaction.getInputs().size() != 1) 
					throw new Exception("Transaction input count is not 1");
				
				TransactionInput input = receivedTransaction.getInput(0);
				TransactionOutPoint outpoint = input.getOutpoint();
				if(!Tools.compareHash(outpoint.getHash(), channel.getOpeningTx().getHash()) || outpoint.getIndex() != 0)
					throw new Exception("Transaction input does not point to the opening transaction");
				
				ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
				
				long serverAmount = channel.getAmountServer();
				
				for(Payment p : paymentList) {
					if(p.paymentToServer) {
						serverAmount += p.getAmount();
					}
				}
				
				if(receivedTransaction.getOutput(1).getValue().value != serverAmount)
					throw new Exception("Server Change is not correct.");
				if(!receivedTransaction.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient())) 
					throw new Exception("Client Change Address is not correct");
				if(!receivedTransaction.getOutput(1).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressServer())) 
					throw new Exception("Server Change Address is not correct");
				if(!Tools.checkTransactionLockTime(receivedTransaction, 0))
					throw new Exception("Lock Time is not correct");
				
				/**
				 * Sign it, such that we can give the hash to the client
				 * 	and make sure it spends our opening tx
				 */			
				ECDSASignature serverSig = Tools.getSignature(receivedTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getServerKeyOnServer());
				ECDSASignature clientSig = ScriptTools.getSignatureOufOfMultisigInput(receivedTransaction.getInput(0));
				Script inputScript = Tools.getMultisigInputScript(clientSig, serverSig);
				receivedTransaction.getInput(0).setScriptSig(inputScript);
				
				inputScript.correctlySpends(receivedTransaction, 0, channel.getOpeningTx().getOutput(0).getScriptPubKey());
				
				if(!Tools.checkTransactionFees(receivedTransaction.getMessageSize(), receivedTransaction, channel.getOpeningTx().getOutput(0)))
					throw new Exception("Transaction fees for channel transaction is not correct");
				
				
				/**
				 * Okay, we got a correct transaction from the client to close this channel.
				 * As this is not revocable, the channel cannot be used any more, and we will broadcast the transaction.
				 */
				
				peerGroup.broadcastTransaction(receivedTransaction);
				
				

				/**
				 * Fill the response to the client
				 */
				CloseChannelResponse response = new CloseChannelResponse();
				
				response.channelTransaction = Tools.byteToString(receivedTransaction.bitcoinSerialize());
			
				responseContainer.fill(response);
				responseContainer.success = true;
				responseContainer.type = Type.CLOSE_CHANNEL_RESPONSE;
				
				/**
				 * Update our database
				 */
				channel.setChannelTxServer(receivedTransaction);
				channel.setChannelTxServerTemp(receivedTransaction);
				channel.setReady(false);

				MySQLConnection.updateChannel(conn, channel);
				
				System.out.println(receivedTransaction);
			} else {
			
				throw new Exception("Type not supported..");
			}
		} catch (Exception e) {
//			try {
//				conn.setAutoCommit(true);
//			} catch (SQLException e1) {
//				e1.printStackTrace();
//			}
			responseContainer.success = false;
			responseContainer.type = Type.FAILURE;
			responseContainer.data = e.getMessage();
//			responseContainer.data = Tools.stacktraceToString(e);
			Tools.emailException(e, message, channel, payment, receivedTransaction, null);
			e.printStackTrace();
		} finally {
			try {
				if(conn != null)  {
					conn.commit();
					conn.setAutoCommit(true);
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		jettyResponse.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        
		responseContainer.sendMessage(jettyResponse);


	}

}
