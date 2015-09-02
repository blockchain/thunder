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
package network.thunder.client.communications.establishchannel;

import java.sql.Connection;
import java.util.ArrayList;

import network.thunder.client.communications.objects.EstablishChannelRequestTwo;
import network.thunder.client.communications.objects.EstablishChannelResponseTwo;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Output;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
/**			
 * Second Request for establishing the channel.
 * 
 * Request: 	Transaction to 2-of-2 multisig, with additional inputs needed signed
 * 
 * Response: 	Refund transaction signed by the Server, with the initial channel transaction as an input
 * 					The client must use this hash to send us a refund prior to us broadcasting the channel.
 * 
 * 							 
 * @author PC
 *
 */
public class EstablishChannelHandlerTwo {
	
	public Connection conn;
	public Channel channel;
    public ArrayList<Output> outputArrayListlist;
	
	public EstablishChannelRequestTwo request() throws Exception {
		
		/**
		 * We have to add inputs to fulfill our part of the channel
		 */
		Transaction transactionFromServer = channel.getOpeningTx();
		
		Transaction signedTransaction = MySQLConnection.getOutAndInputsForChannel(conn, outputArrayListlist, channel.getId(), channel.getInitialAmountClient(), transactionFromServer, channel.getChangeAddressClientAsAddress(), true, true);
		
//		System.out.println(signedTransaction.toString());
		
		EstablishChannelRequestTwo m = new EstablishChannelRequestTwo();
		
		m.transaction = Tools.byteToString(signedTransaction.bitcoinSerialize());
		
		return m;
	}
	
	public void evaluateResponse(EstablishChannelResponseTwo m) throws Exception {
		/**
		 * Check that the refund transaction is correct..
		 * 
		 * Check all basic properties first
		 * 
		 * Calculate and add our signature then, so we can check if the refund transaction
		 * 	actually spends the channel output..
		 *	
		 */
		Transaction openingTransaction = channel.getOpeningTx();
		Transaction refundTransaction = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.refundTransaction));

		TransactionOutput refundOutputClient = refundTransaction.getOutput(0);
		
		if(refundOutputClient.getValue().value != (channel.getInitialAmountClient() - Tools.getTransactionFees(1, 2)))
			throw new Exception("Refund value is not correct.. Should be: "+(channel.getInitialAmountClient() - Tools.getTransactionFees(1, 2))+" Is: "+refundOutputClient.getValue().value);
		
		if(!refundOutputClient.getAddressFromP2PKHScript(Constants.getNetwork()).toString().equals(channel.getChangeAddressClient()))
			throw new Exception("Refund does not pay to correct address.. Should be: "+channel.getChangeAddressClient()+" Is: "+refundOutputClient.getAddressFromP2PKHScript(Constants.getNetwork()).toString());
		
		if(Math.abs(refundTransaction.getLockTime() - channel.getTimestampClose()) > Constants.MAX_CHANNEL_CREATION_TIME)
			throw new Exception("Refund LockTime does not match.. Should be: "+channel.getTimestampClose() + " Is: "+refundTransaction.getLockTime());
		
		if(refundTransaction.getInput(0).getSequenceNumber() != 0)
			throw new Exception("Refund SequenceNumber is not 0..");
		
		
		/**
		 * Sign the transaction ourselves, such that it can be spent..
		 */
		ECDSASignature clientSignature = Tools.getSignature(refundTransaction, 0, openingTransaction.getOutput(0), channel.getClientKeyOnClient() );
		ECDSASignature serverSignature = ECDSASignature.decodeFromDER(Tools.stringToByte(m.refundServerSig));
		
		Script inputScript = Tools.getMultisigInputScript(clientSignature, serverSignature);

		
		/**
		 * Check if the refund TX actually spends the transaction
		 */
		TransactionInput input = refundTransaction.getInput(0);
		input.setScriptSig(inputScript);
		
		Script outputScript = openingTransaction.getOutput(0).getScriptPubKey();
		inputScript.correctlySpends(refundTransaction, 0, outputScript);
		
		channel.setOpeningTxHash(refundTransaction.getInput(0).getOutpoint().getHash().toString());
		channel.setRefundTxServer(refundTransaction);
		channel.setRefundTxClient(refundTransaction);

	}
	
	

}
