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

import network.thunder.client.communications.objects.EstablishChannelRequestThree;
import network.thunder.client.communications.objects.EstablishChannelResponseThree;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.etc.Tools;
import network.thunder.client.wallet.TransactionStorage;

import org.bitcoinj.core.Transaction;
/**			
 * Second Request for establishing the channel.
 * 
 * Request: 	Refund of the opening transaction
 * 
 * Response: 	
 * 							 
 * @author PC
 *
 */
public class EstablishChannelHandlerThree {
	
	public Connection conn;
	public Channel channel;
	public TransactionStorage transactionStorage;
	
	public EstablishChannelResponseThree request() throws Exception {
		
		/**
		 * We have the signed refund transaction already in our channel
		 */
		Transaction refundTransaction = channel.getRefundTxClient();
//		System.out.println(refundTransaction.toString());
		
		EstablishChannelResponseThree m = new EstablishChannelResponseThree();
		m.refundTransactionSignature = Tools.byteToString(refundTransaction.getInput(0).getScriptSig().getChunks().get(1).data);

		return m;
	}
	
	public void evaluateResponse(EstablishChannelRequestThree m) throws Exception {
		

		
		channel.setEstablishPhase(4);
		transactionStorage.addOpenedChannel(channel);
		MySQLConnection.updateChannel(conn, channel);
		
		
		/**
		 * No response from the server..
		 *	
		 */

	}
	
	

}
