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

import network.thunder.client.communications.objects.EstablishChannelRequestOne;
import network.thunder.client.communications.objects.EstablishChannelResponseOne;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.Script;
import org.spongycastle.util.Arrays;

import java.sql.Connection;

/**
 * First Request for establishing the channel.
 * <p>
 * Request: 	- PubKey of the client, that will be part of the 2-of-2 multisig channel
 * - Maximum timeframe in days, needed for setting up the refund transactions
 * - TotalAmount of the channel
 * - ClientAmount of the channel
 * - changeAddressClient for all channel payouts in the future
 * <p>
 * Response: 	- PubKey of the server, that will be part of the 2-of-2 multisig channel
 * - TXunsigned, a transaction with the outputs needed for the channel and the server-change
 * - changeAddressClient for all channel payouts in the future
 *
 * @author PC
 */
public class EstablishChannelHandlerOne {

	public Connection conn;
	public int timeInDays;
	public Channel channel;

	public void evaluateResponse (EstablishChannelResponseOne m) throws Exception {

		/**
		 * Create the unsigned transaction
		 *
		 * For simplicity, the output for the channel should be the first of the TX..
		 * Also the client-key is the first in the multisig
		 */

		channel.setPubKeyServer(m.pubKey);
		channel.setChangeAddressServer(m.changeAddress);

		Transaction transactionFromServer = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.txUnsigned));

		//		System.out.println(transactionFromServer.toString());
		//		System.out.println(transactionFromServer.bitcoinSerialize().length);
		//
		//		System.out.println(transactionFromServer.getOutput(0).bitcoinSerialize().length);
		//		System.out.println(transactionFromServer.getOutput(1).bitcoinSerialize().length);

		/**
		 * Check if the first output match the channel output
		 */
		Script correctScript = ScriptTools.getMultisigOutputScript(channel);
		byte[] serverScript = transactionFromServer.getOutput(0).getScriptBytes();

		if (!Arrays.areEqual(correctScript.getProgram(), serverScript)) {
			throw new Exception("Channel output script does not match..");
		}

		if (!(transactionFromServer.getOutput(0).getValue().value == (channel.getAmountClient() + channel.getAmountServer()))) {
			throw new Exception("Channel output does not contain the proper value. Got: " + transactionFromServer.getOutput(0).getValue().value + " Should " +
					"be:" +
					" " + (channel.getAmountClient() + channel.getAmountServer()));
		}

		/**
		 * Seems everything is okay..
		 */

		channel.setOpeningTx(transactionFromServer);
		MySQLConnection.updateChannel(conn, channel);

		return;
	}

	public EstablishChannelRequestOne request () throws Exception {

		channel.setTimestampOpen(Tools.currentTime());
		channel.setTimestampClose(Tools.currentTime() + timeInDays * 24 * 60 * 60);
		channel.setKeyChainChild(1);
		channel.setKeyChainDepth(timeInDays * 24 * 60 * 60 / Constants.TIMEFRAME_PER_KEY_DEPTH);

		/**
		 * Rules for the channel:
		 * 	(1) Total value < MAX_CHANNEL_VALUE
		 * 	(2) serverShare < MAX_SERVER_SHARE
		 * 	(3) Total value > MIN_CHANNEL_VALUE
		 * 	(4) clientShare > serverShare
		 */
		//		if(channel.getInitialAmountClient() + channel.getInitialAmountServer() > Constants.MAX_CHANNEL_VALUE)
		//			throw new Exception("Channel value is too high. Currently the maximum is "+Constants.MAX_CHANNEL_VALUE+"satoshi.");
		//
		//		if(channel.getInitialAmountServer() > Constants.MAX_SERVER_SHARE)
		//			throw new Exception("Server share too high. Currently the maximum is "+Constants.MAX_SERVER_SHARE+"satoshi.");
		//
		//		if(channel.getInitialAmountServer() > channel.getInitialAmountClient())
		//			throw new Exception("Server share higher than client share..");
		//
		//		if(channel.getInitialAmountClient() + channel.getInitialAmountServer() < Constants.MIN_CHANNEL_VALUE)
		//			throw new Exception("Channel value is too low. Currently the minimum is "+Constants.MIN_CHANNEL_VALUE+"satoshi.");

		EstablishChannelRequestOne m = new EstablishChannelRequestOne();

		m.pubKey = channel.getPubKeyClient();
		m.timeInDays = timeInDays;
		m.changeAddress = channel.getChangeAddressClient();
		m.totalAmount = channel.getInitialAmountClient() + channel.getInitialAmountServer();
		m.clientAmount = channel.getInitialAmountClient();

		return m;
	}

}
