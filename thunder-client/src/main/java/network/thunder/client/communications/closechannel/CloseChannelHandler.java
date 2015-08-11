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
package network.thunder.client.communications.closechannel;

import java.sql.Connection;
import java.util.ArrayList;

import network.thunder.client.communications.objects.CloseChannelRequest;
import network.thunder.client.communications.objects.CloseChannelResponse;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.script.Script;

public class CloseChannelHandler {
	public Connection conn;
	public Channel channel;
	
	public Transaction receivedTransaction;
	
	public CloseChannelRequest request() throws Exception {

		
		CloseChannelRequest request = new CloseChannelRequest();
		
		Transaction channelTransaction = new Transaction(Constants.getNetwork());
		
		
		ArrayList<Payment> paymentList = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
		
		long serverAmount = channel.getAmountServer();
		
		for(Payment p : paymentList) {
			if(p.paymentToServer) {
				serverAmount += p.getAmount();
			}
		}

		channelTransaction.addOutput(Coin.valueOf(0), channel.getChangeAddressClientAsAddress());
		channelTransaction.addOutput(Coin.valueOf(serverAmount), channel.getChangeAddressServerAsAddress());
		
		channelTransaction.addInput(new Sha256Hash(channel.getOpeningTxHash()), 0, Tools.getDummyScript());
		
		long clientAmount = channel.getOpeningTx().getOutput(0).getValue().value - serverAmount - Tools.getTransactionFees(channelTransaction.getMessageSize() + 2 * 72);
		
		channelTransaction.getOutput(0).setValue(Coin.valueOf(clientAmount));
		
		ECDSASignature clientSig = Tools.getSignature(channelTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getClientKeyOnClient());
		Script inputScript = ScriptTools.getMultisigInputScript(clientSig);
		channelTransaction.getInput(0).setScriptSig(inputScript);


		System.out.println(channelTransaction);
		
		request.channelTransaction = Tools.byteToString(channelTransaction.bitcoinSerialize());
		return request;		
		
	}
	
	
	public void evaluateResponse(CloseChannelResponse m) throws Exception {	
		receivedTransaction = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.channelTransaction));
		
		System.out.println(receivedTransaction);
		

	}
}
