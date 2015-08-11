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
package network.thunder.client.communications.sendpayment;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import network.thunder.client.communications.objects.SendPaymentRequestOne;
import network.thunder.client.communications.objects.SendPaymentResponseOne;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.KeyWrapper;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
/**
 * First Request for making a payment.
 * 
 * Request: 	- Updated channel transaction with the new payment added
 * 				- Amount to be sent
 * 				- Receiver of the payment
 * 
 * Response: 	- Updated channel transaction with the new payment added
 * 				- Hash of the channel transaction from the server
 * @author PC
 *
 */
public class SendPaymentHandlerOne {
	
	public Connection conn;
	public Channel channel;
	public Payment newPayment;
	
	public String channelHash;
	
	public ArrayList<Payment> paymentList;
	
	public SendPaymentRequestOne request() throws Exception {
		/**
		 * Add the Change Revoke Payments
		 * Use '0' for the client change here and change it later when we have the otherwise complete tx.
		 */
		KeyWrapper keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, false);

		
		Transaction channelTransaction = new Transaction(Constants.getNetwork());
		channelTransaction.addOutput(Coin.valueOf(0), channel.getChangeAddressClientAsAddress());
		channelTransaction.addOutput(Coin.valueOf(channel.getAmountServer()+Tools.getTransactionFees(Constants.SIZE_REVOKE_TX)), ScriptTools.getRevokeScript(keyWrapper, channel, Constants.SERVERSIDE));	

		

		for(Payment payment : paymentList) {
			Script outputScript = ScriptTools.getPaymentScript(channel, keyWrapper, payment.getSecretHash(), Constants.SERVERSIDE, payment.paymentToServer);
			long amount;
			if(payment.paymentToServer) {
				amount = payment.getAmount() + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX );
			} else {
				amount = payment.getAmount() - Tools.calculateServerFee(payment.getAmount()) + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX );
			}
			channelTransaction.addOutput(Coin.valueOf(amount), outputScript);
		}
		
		Transaction oldChannel = channel.getChannelTxServer();
		/**
		 * Set those keys from the list as used, that have been around in any of the Scripts.
		 */	
		MySQLConnection.setKeysUsed(conn, keyWrapper);


		/**
		 * Calculate the fees and set the client change accordingly.
		 */
		channelTransaction.addInput(new Sha256Hash(channel.getOpeningTxHash()), 0, Tools.getDummyScript());
		ECDSASignature clientSig = Tools.getSignature(channelTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getClientKeyOnClient());	
		channelTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(clientSig));
		
		long sumInputs = channel.getOpeningTx().getOutput(0).getValue().value;
		long sumOutputs = Tools.getCoinValueFromOutput(channelTransaction.getOutputs());
		
		int size = channelTransaction.getMessageSize() + 72;
		long fees = Tools.getTransactionFees( size );
		channelTransaction.getOutput(0).setValue(Coin.valueOf( sumInputs - sumOutputs - fees ));
		
		/**
		 * Add our signature.
		 */
		clientSig = Tools.getSignature(channelTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getClientKeyOnClient());	
		channelTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(clientSig));


		
		/**
		 * Update our database with the new data.
		 */
		channel.setChannelTxServerTemp(channelTransaction);
		channel.setPaymentPhase(1);
		MySQLConnection.updateChannel(conn, channel);


		/**
		 * Fill the request to the server.
		 */
		SendPaymentRequestOne request = new SendPaymentRequestOne();
		
		request.channelTransaction = Tools.byteToString(channelTransaction.bitcoinSerialize());

		
		request.receipient = newPayment.getReceiver();
		request.amount = newPayment.getAmount();

		
		
		return request;
		

					
	}
	
	public void evaluateResponse(SendPaymentResponseOne m) throws Exception {
		
		/**
		 * Check the updated transaction we received from the server
		 * 
		 * Just allow one input for now, the one of the multisig
		 */
		Transaction receivedTransaction = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.channelTransaction));
		

		
		if(receivedTransaction.getInputs().size() != 1) 
			throw new Exception("Transaction input count is not 1");
		
		TransactionInput input = receivedTransaction.getInput(0);
		TransactionOutPoint outpoint = input.getOutpoint();
		if(!outpoint.getHash().toString().equals(channel.getOpeningTxHash()) || outpoint.getIndex() != 0)
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
		KeyWrapper keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, true);
		
		if(channel.getChannelTxServerID() != 0) {
			Transaction channelTransaction = channel.getChannelTxClient();
			if(receivedTransaction.getOutputs().size() - channelTransaction.getOutputs().size() != + 1 )
				throw new Exception("Transaction Output count is not correct");
			
			List<TransactionOutput> clientList = channelTransaction.getOutputs();
			List<TransactionOutput> serverList = receivedTransaction.getOutputs();
			
			
			for(int i=0; i<paymentList.size()-1; i++) {
				TransactionOutput client = clientList.get(i+2);
				TransactionOutput server = serverList.get(i+2);
				Payment payment = paymentList.get(i);
				if(client.getValue().value != server.getValue().value) {
					System.out.println(channelTransaction);
					System.out.println(receivedTransaction);
					throw new Exception("Value of an old payment does not match..");
				}

				if(!ScriptTools.checkPaymentScript(server, channel, keyWrapper, payment.getSecretHash(), Constants.CLIENTSIDE, payment.paymentToServer ))
					throw new Exception("Payment Script is not correct..");
			}
		}
		
		

		
		/**
		 * As in the channel creation, the 
		 * 		1. output is the client change
		 * 		2. output is the server change
		 */
		if(receivedTransaction.getOutput(0).getValue().value > ( channel.getAmountClient() - newPayment.getAmount()) )
			throw new Exception("Client change is too high");
		if(receivedTransaction.getOutput(1).getValue().value != ( channel.getAmountServer() ) ) 
			throw new Exception("Server change is not correct");
		
		/**
		 * On Clientside, the change(revoke) is
		 * 		1. client goes to another multisig, between clientPubKeyTemp and serverPubKey
		 * 		2. server goes directly to his change address
		 */
		if(!ScriptTools.checkRevokeScript(receivedTransaction.getOutput(0), channel, keyWrapper, Constants.CLIENTSIDE))
			throw new Exception("Revoke Script to Client is not correct..");
		if(!receivedTransaction.getOutput(1).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressServer()))
			throw new Exception("Revoke Script to Server does not pay to correct address.. Should Be: "+channel.getChangeAddressServer()+" Is: "+receivedTransaction.getOutput(1).getAddressFromP2PKHScript(Constants.getNetwork()));


		
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
		System.out.println(Tools.byteToString58(paymentOutput.bitcoinSerialize()));
		String secretHash = ScriptTools.getRofPaymentScript(paymentOutput);
		if(!secretHash.equals(newPayment.getSecretHash()))
			throw new Exception("SecretHash is not correct..");

		
		long amountShouldBe = newPayment.getAmount() + Tools.getTransactionFees( Constants.SIZE_OF_SETTLEMENT_TX );
		if(paymentOutput.getValue().value < amountShouldBe)
			throw new Exception("New payment output value is not sufficient");
		
		if(!ScriptTools.checkPaymentScript(paymentOutput, channel, keyWrapper, null, Constants.CLIENTSIDE, true))
			throw new Exception("Script of the added payment is not correct..");
		
		/**
		 * Sign it, such that we can give the hash to the server
		 * 	and make sure it spends our opening tx
		 */
		ECDSASignature clientSig = Tools.getSignature(receivedTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getClientKeyOnClient());
		ECDSASignature serverSig = ScriptTools.getSignatureOufOfMultisigInput(receivedTransaction.getInput(0));
		Script inputScript = Tools.getMultisigInputScript(clientSig, serverSig);
		receivedTransaction.getInput(0).setScriptSig(inputScript);
		
		inputScript.correctlySpends(receivedTransaction, 0, channel.getOpeningTx().getOutput(0).getScriptPubKey());
		
		/**
		 * Check that the transaction fees are sufficient for the size of the channel
		 */
		if(!Tools.checkTransactionFees(receivedTransaction.getMessageSize(), receivedTransaction, channel.getOpeningTx().getOutput(0)))
			throw new Exception("Transaction fees for channel transaction not correct.");
			
		
		channel.setChannelTxClientTemp(receivedTransaction);
		MySQLConnection.setKeysUsed(conn, keyWrapper);
		

		
		
	}
	
	

}
