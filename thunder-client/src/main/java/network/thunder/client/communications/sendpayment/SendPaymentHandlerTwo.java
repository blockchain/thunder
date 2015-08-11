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

import network.thunder.client.communications.objects.SendPaymentRequestTwo;
import network.thunder.client.communications.objects.SendPaymentResponseTwo;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
/**			
 * Second Request for making a payment.
 * 
 * Request: 	- transaction for the serverchange of his channeltransaction
 * 				- array with settlement transactions for each payment, with the new channelhash
 * 				- array with refund transactions for each payment, with the new channelhash
 * 				- hash of the clientside channeltransaction
 * 
 * Response: 	- transaction for the clientchange of our channeltransaction
 * 				- array with refund transactions for each payment, with the new channelhash
 * 
 * 							 
 * @author PC
 *
 */
public class SendPaymentHandlerTwo {
	
	public Connection conn;
	public Channel channel;
	
	public Payment newPayment;
	public String channelHash;
	
	public ArrayList<Payment> paymentList;
	
	Sha256Hash channelHashYours;
	
	
	
	public SendPaymentRequestTwo request() throws Exception {
		
		channelHashYours = new Sha256Hash(Tools.stringToByte(channelHash));
		
		/**
		 * Create the transaction for the server to get his part of the channel back
		 * 
		 */
		Transaction revokeTransaction = new Transaction(Constants.getNetwork());
		revokeTransaction.addOutput(Coin.valueOf(channel.getAmountServer()), channel.getChangeAddressServerAsAddress());
		revokeTransaction.addInput(channelHashYours, 1, Tools.getDummyScript());
		revokeTransaction = Tools.setTransactionLockTime(revokeTransaction, channel.getTimestampRefunds());
		ECDSASignature clientSig = Tools.getSignature(revokeTransaction, 0, channel.getChannelTxServerTemp().getOutput(1), channel.getClientKeyOnClient());
		revokeTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(clientSig));
		
		
		
		
		/**
		 * Create the refunds and settlement transactions for each payment, 
		 * 	based on the hash we got from the server
		 */
		ArrayList<String> refundList = new ArrayList<String>();
		ArrayList<String> settlementList = new ArrayList<String>();
		
		
		
		for(int i=0; i<paymentList.size(); ++i) {
			
			TransactionOutput output = channel.getChannelTxServerTemp().getOutput(i+2);
			Payment payment = paymentList.get(i);
			
			Transaction refund = new Transaction(Constants.getNetwork());
			refund.addInput(channelHashYours, i+2, Tools.getDummyScript());
			refund.getInput(0).setSequenceNumber(0);
			
			if(payment.paymentToServer) {
				refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressClientAsAddress());
				refund.setLockTime(channel.getTimestampRefunds());
			} else {
				refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressServerAsAddress());
				refund.setLockTime(payment.getTimestampCreated() + Constants.TIME_TO_REVEAL_SECRET);
			}
			
			
			
			Transaction settlement = new Transaction(Constants.getNetwork());
			settlement.addInput(channelHashYours, i+2, Tools.getDummyScript());
			
			if(payment.paymentToServer) {
				settlement.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressServerAsAddress());
			} else {
				settlement.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressClientAsAddress());
			}
			
			clientSig = Tools.getSignature(refund, 0, output, channel.getClientKeyOnClient());
			refund.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(clientSig));
			refund.getInput(0).setSequenceNumber(0);
			
			clientSig = Tools.getSignature(settlement, 0, output, channel.getClientKeyOnClient());
			settlement.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(clientSig));

			
			refundList.add(Tools.byteToString(refund.bitcoinSerialize()));
			settlementList.add(Tools.byteToString(settlement.bitcoinSerialize()));
			
			
		}
		/**
		 * Update our database with the channel.
		 */
		channel.setChannelTxRevokeServerTemp(revokeTransaction);


		/**
		 * Fill request for the server.
		 */
		SendPaymentRequestTwo m = new SendPaymentRequestTwo();
		m.paymentRefunds = refundList;
		m.paymentSettlements = settlementList;
		m.channelHash = Tools.byteToString(channel.getChannelTxClientTemp().getHash().getBytes());
		m.revokeTransaction = Tools.byteToString(revokeTransaction.bitcoinSerialize());
		
		
		return m;
	}
	
	public void evaluateResponse(SendPaymentResponseTwo m) throws Exception {
		/**
		 * We got a 3 lists back with refunds, settlements and additional tx for each payment.
		 * Make sure all of those are correct and spends the corresponding output of our channel.
		 * 
		 * Also check the revoke tx that we got, whether it spends our change correctly.
		 *	
		 */
		Transaction channelTransaction = channel.getChannelTxClientTemp();
		Sha256Hash channelHashOur = channel.getChannelTxClientTemp().getHash();

		
		
		ArrayList<Transaction> refundList = new ArrayList<Transaction>();
		ArrayList<Transaction> settlementList = new ArrayList<Transaction>();
		ArrayList<Transaction> additionalList = new ArrayList<Transaction>();
		
		for(String s : m.paymentAdditionals) {
			additionalList.add(new Transaction(Constants.getNetwork(), Tools.stringToByte(s)));
		}
		for(String s : m.paymentRefunds) {
			refundList.add(new Transaction(Constants.getNetwork(), Tools.stringToByte(s)));
		}
		for(String s : m.paymentSettlements) {
			settlementList.add(new Transaction(Constants.getNetwork(), Tools.stringToByte(s)));
		}
		

		
		
		
		
		/**
		 * TODO: This loop is the bottleneck here, probably a way to improve it.
		 */
		for(int i=0; i<paymentList.size(); ++i) {
			Payment payment = paymentList.get(i);
			Transaction refund = refundList.get(i);
			Transaction settlement = settlementList.get(i);
			Transaction additional = additionalList.get(i);
			
			
//			System.out.println("RefundTransaction"+refund.toString());
			
			if(refund.getInputs().size() != 1)
				throw new Exception("Refund Input Size is not 1");
			if(settlement.getInputs().size() != 1)
				throw new Exception("Settlement Input Size is not 1");
			if(additional.getInputs().size() != 1)
				throw new Exception("Additional Input Size is not 1");
			
			if(refund.getOutputs().size() != 1)
				throw new Exception("Refund Output Size is not 1");
			if(settlement.getOutputs().size() != 1)
				throw new Exception("Settlement Output Size is not 1");
			if(additional.getOutputs().size() != 1)
				throw new Exception("Additional Output Size is not 1");
			
			if(!Tools.compareHash(refund.getInput(0).getOutpoint().getHash(), channelHashOur))
				throw new Exception("Refund Input Hash is not correct");
			if(!Tools.compareHash(settlement.getInput(0).getOutpoint().getHash(), channelHashOur))
				throw new Exception("Settlement Input Hash is not correct");
			if(!Tools.compareHash(additional.getInput(0).getOutpoint().getHash(), channelHashYours))
				throw new Exception("Additional Input Hash is not correct");
			
			if(refund.getInput(0).getOutpoint().getIndex() != (i+2))
				throw new Exception("Refund Input Index is not correct");			
			if(settlement.getInput(0).getOutpoint().getIndex() != (i+2))
				throw new Exception("Settlement Input Index is not correct");			
			if(additional.getInput(0).getOutpoint().getIndex() != (i+2))
				throw new Exception("Additional Input Index is not correct");

			if(refund.getOutput(0).getValue().value != payment.getAmount() )
				throw new Exception("Refund Output value not correct");
			if(settlement.getOutput(0).getValue().value != payment.getAmount() )
				throw new Exception("Settlement Output value not correct");
			if(additional.getOutput(0).getValue().value != payment.getAmount() )
				throw new Exception("Additional Output value not correct");
			
			if(payment.paymentToServer) {
				if(!Tools.checkTransactionLockTime(refund, channel.getTimestampRefunds()))
					throw new Exception("Refund LockTime is not correct.");
			}
			if(payment.paymentToServer) {
				if(!refund.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
					throw new Exception("Refund Output Address is not correct");
				if(!additional.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
					throw new Exception("Settlement Output Address is not correct");
				if(!settlement.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressServer()))
					throw new Exception("Additional Output Address is not correct");
				
				if(!Tools.checkTransactionLockTime(additional, channel.getTimestampRefunds()))
					throw new Exception("Additional LockTime is not correct.");
			} else {
				if(!refund.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressServer()))
					throw new Exception("Refund Output Address is not correct");
				if(!additional.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
					throw new Exception("Settlement Output Address is not correct");
				if(!settlement.getOutput(0).getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
					throw new Exception("Additional Output Address is not correct");
			}
			
			if(Tools.checkSignature(refund, 0, channelTransaction.getOutput(i+2), channel.getServerKeyOnClient(), refund.getInput(0).getScriptSig().getChunks().get(1).data))
				throw new Exception("Refund Server Signature is not correct");
			if(Tools.checkSignature(settlement, 0, channelTransaction.getOutput(i+2), channel.getServerKeyOnClient(), refund.getInput(0).getScriptSig().getChunks().get(1).data))
				throw new Exception("Settlement Server Signature is not correct");
			
			
			Script outputScript = channel.getChannelTxServerTemp().getOutput(i+2).getScriptPubKey();
			Script inputScript = additional.getInput(0).getScriptSig();
			if(payment.paymentToServer) {
				inputScript.correctlySpends(additional, 0, outputScript);
			} else {
				/**
				 * TODO: Write a helper to check the settlement against the temporary key from the server
				 * 				as we cannot use correctlySpends without R
				 */
			}
			

			
			if(payment.paymentToServer) {
				payment.setSettlementTxSenderTemp(settlement);
				payment.setAddTxSenderTemp(additional);
				payment.setRefundTxSenderTemp(refund);
			} else {
				payment.setSettlementTxReceiverTemp(settlement);
				payment.setAddTxReceiverTemp(additional);
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

		
		if(revoke.getInputs().size() != 1)
			throw new Exception("Revoke Input Size is not 1");
		if(revoke.getOutputs().size() != 1)
			throw new Exception("Revoke Output Size is not 1");
		if(!Tools.compareHash(revoke.getInput(0).getOutpoint().getHash(), channelHashOur))
			throw new Exception("Revoke Input Hash is not correct");
		if(revoke.getInput(0).getOutpoint().getIndex() != 0)
			throw new Exception("Revoke Input Index is not correct");	
		if( ( channelTransaction.getOutput(0).getValue().value - revoke.getOutput(0).getValue().value) != Tools.getTransactionFees(Constants.SIZE_REVOKE_TX) )
			throw new Exception("Revoke Output value not correct");
		if(!Tools.checkTransactionLockTime(revoke, channel.getTimestampRefunds()))
			throw new Exception("Revoke TimeLock is not correct.");
//		if(Tools.checkSignature(revoke, 0, channelTransaction.getOutput(0), channel.getServerKeyOnClient(), revoke.getInput(0).getScriptSig().getChunks().get(1).data))
//			throw new Exception("Revoke Server Signature is not correct");

		
//		Script outputScript = channel.getChannelTxClientTemp().getOutput(0).getScriptPubKey();
//		Script inputScript = revoke.getInput(0).getScriptSig();
//		inputScript.correctlySpends(additional, 0, outputScript);

		/**
		 * Validation complete, all transactions we got from the server are correct..
		 */
		
		channel.setChannelTxRevokeClientTemp(revoke);
		MySQLConnection.updateChannel(conn, channel);
		

		
	}

}
