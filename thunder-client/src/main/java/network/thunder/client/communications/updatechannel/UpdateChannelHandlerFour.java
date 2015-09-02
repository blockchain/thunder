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
package network.thunder.client.communications.updatechannel;

import java.sql.Connection;
import java.util.ArrayList;

import network.thunder.client.communications.objects.UpdateChannelRequestFour;
import network.thunder.client.communications.objects.UpdateChannelResponseFour;
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

public class UpdateChannelHandlerFour {
	public Connection conn;
	public Channel channel;	
	
	int totalAmountOfPayments = 0;

	
	/**
	 * List of all payments in the new channel
	 */
	public ArrayList<Payment> newPaymentsTotal = new ArrayList<Payment>();
	
	public Sha256Hash serverHash;
	
	
	public UpdateChannelRequestFour request() throws Exception {
		UpdateChannelRequestFour request = new UpdateChannelRequestFour();
		
		
		
		
		/**
		 * Create the transaction for the server to get his part of the channel back
		 * 
		 */
		Transaction revokeTransaction = new Transaction(Constants.getNetwork());
		revokeTransaction.addOutput(Coin.valueOf(channel.getChannelTxServerTemp().getOutput(1).getValue().value - Tools.getTransactionFees(Constants.SIZE_REVOKE_TX)), channel.getChangeAddressServerAsAddress());
		revokeTransaction.addInput(serverHash, 1, Tools.getDummyScript());
		Tools.setTransactionLockTime(revokeTransaction, channel.getTimestampRefunds());
		ECDSASignature clientSig = Tools.getSignature(revokeTransaction, 0, channel.getChannelTxServerTemp().getOutput(1), channel.getClientKeyOnClient());
		revokeTransaction.getInput(0).setScriptSig(ScriptTools.getMultisigInputScript(clientSig));
		
		
		
		
		/**
		 * Create the refunds and settlement transactions for each payment, 
		 * 	based on the hash we got from the server
		 */
		ArrayList<String> refundList = new ArrayList<String>();
		ArrayList<String> settlementList = new ArrayList<String>();
		
		
		
		for(int i=0; i<newPaymentsTotal.size(); ++i) {
			
			TransactionOutput output = channel.getChannelTxServerTemp().getOutput(i+2);
			Payment payment = newPaymentsTotal.get(i);
			
			Transaction refund = new Transaction(Constants.getNetwork());
			refund.addInput(serverHash, i+2, Tools.getDummyScript());
			refund.getInput(0).setSequenceNumber(0);
			
			if(payment.paymentToServer) {
				refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressClientAsAddress());
				refund.setLockTime(channel.getTimestampRefunds());
			} else {
				refund.addOutput(Coin.valueOf(payment.getAmount()), channel.getChangeAddressServerAsAddress());
				int time = payment.getTimestampAddedToReceiver();
				if(time==0) time = Tools.currentTime();
				refund.setLockTime(time + Constants.TIME_TO_REVEAL_SECRET);
			}
			
			
			
			Transaction settlement = new Transaction(Constants.getNetwork());
			settlement.addInput(serverHash, i+2, Tools.getDummyScript());
			
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
		
		


		request.paymentRefunds = refundList;
		request.paymentSettlements = settlementList;
		request.revokeTransaction = Tools.byteToString(revokeTransaction.bitcoinSerialize());
		request.transactionHash = Tools.byteToString(channel.getChannelTxClientTemp().getHash().getBytes());

		return request;
	}
	
	public void evaluate(UpdateChannelResponseFour m) throws Exception {
		
	
		
		
		Transaction channelTransaction = channel.getChannelTxClientTemp();
		Sha256Hash clientHash = channel.getChannelTxClientTemp().getHash();


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

		for(int i=0; i<newPaymentsTotal.size(); ++i) {
			Payment payment = newPaymentsTotal.get(i);
			Transaction refund = refundList.get(i);
			Transaction settlement = settlementList.get(i);
			Transaction additional = additionalList.get(i);

			
			if(payment.paymentToServer) {
				ScriptTools.checkTransaction(refund, (i+2), clientHash, payment.getAmount(), channel.getChangeAddressClient(), channel.getTimestampRefunds());
				ScriptTools.checkTransaction(settlement, (i+2), clientHash, payment.getAmount(), channel.getChangeAddressServer(), 0);
				ScriptTools.checkTransaction(additional, (i+2), serverHash, payment.getAmount(), channel.getChangeAddressClient(), channel.getTimestampRefunds());

			} else {
				ScriptTools.checkTransaction(refund, (i+2), clientHash, payment.getAmount(), channel.getChangeAddressServer(), channel.getTimestampRefunds());
				ScriptTools.checkTransaction(settlement, (i+2), clientHash, payment.getAmount(), channel.getChangeAddressClient(), 0);
				ScriptTools.checkTransaction(additional, (i+2), serverHash, payment.getAmount(), channel.getChangeAddressClient(), 0);
				
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
		ScriptTools.checkTransaction(revoke, 0, clientHash, channel.getChannelTxClientTemp().getOutput(0).getValue().value-Tools.getTransactionFees(Constants.SIZE_REVOKE_TX), channel.getChangeAddressClient(), channel.getTimestampRefunds());

//		if(Tools.checkSignature(revoke, 0, channelTransaction.getOutput(0), channel.getClientKeyOnServer(), revoke.getInput(0).getScriptSig().getChunks().get(1).data))
//			throw new Exception("Revoke Client Signature is not correct");
		
		
		
		
		
		channel.setChannelTxRevokeClientTemp(revoke);
		
		
		
		
		
		
		
		
		
	}
}
