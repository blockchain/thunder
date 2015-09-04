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
package network.thunder.client.wallet;

import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.SideConstants;
import network.thunder.client.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

import java.sql.Connection;
import java.util.Timer;
import java.util.TimerTask;

public class TransactionTimerTask extends TimerTask {

	PeerGroup peerGroup;
	Transaction transaction;

	String secretHash;
	int channelId;
	Connection conn;
	boolean serverIssuedTransaction;
	TransactionOutput output;
	Transaction settlementTransaction;

	Timer timer;

	public TransactionTimerTask (PeerGroup peerGroup, Transaction transaction) {
		this.peerGroup = peerGroup;
		this.transaction = transaction;
	}

	public TransactionTimerTask (Timer timer, Connection conn, PeerGroup peerGroup, Transaction transaction, String secretHash, int channelId, boolean
			serverIssuedTransaction, TransactionOutput output, Transaction settlementTransaction) {
		this.peerGroup = peerGroup;
		this.transaction = transaction;
		this.conn = conn;
		this.secretHash = secretHash;
		this.channelId = channelId;
		this.serverIssuedTransaction = serverIssuedTransaction;
		this.output = output;
		this.settlementTransaction = settlementTransaction;
		this.timer = timer;
	}

	@Override
	public void run () {
		if (secretHash != null) {
			/**
			 * Before we actually broadcasts a refund tx, check if we really don't have the secret,
			 * 	as it's possible we collected it somewhere else..
			 */
			try {
				Payment payment = MySQLConnection.getPayment(conn, secretHash, channelId);
				if (payment.getSecret() != null) {
					/**
					 * We actually got the secret for this payment..
					 */
					Channel channel = MySQLConnection.getChannel(conn, channelId);
					ECKey key = MySQLConnection.getKeyCurrentlyUsed(conn, channel);

					ECDSASignature signature1;
					try {
						signature1 = ScriptTools.getSignatureOufOfMultisigInput(settlementTransaction.getInput(0));
					} catch (Exception e) {
						signature1 = null;
					}
					ECDSASignature signature2 = Tools.getSignature(settlementTransaction, 0, output, key);
					Script inputScript;

					if (payment.paymentToServer) {
						/**
						 * Payments towards the server that has been revealed by the receiver
						 *  and should thus be paid to the server aswell..
						 *
						 * If the Server issued this transaction, he is the only one who can claim the settlement.
						 */
						if (serverIssuedTransaction && !SideConstants.RUNS_ON_SERVER) {
							return;
						}
					} else {
						/**
						 * Payments towards the client, that he revealed the secret for.
						 * Usually this should no longer be in the channel, but it might happen.
						 * We pay these out to the client for now, I can't think of a case, where we get here
						 * 	and are allowed to claim these funds..
						 *
						 * If the Client issued this transaction, he is the only one who can claim the settlement.
						 */
						if (!serverIssuedTransaction && SideConstants.RUNS_ON_SERVER) {
							return;
						}
					}
					if (SideConstants.RUNS_ON_SERVER) {
						inputScript = ScriptTools.getSettlementScriptSig(channel, signature1, signature2, payment.getSecret(), serverIssuedTransaction,
								payment.paymentToServer);
					} else {
						inputScript = ScriptTools.getSettlementScriptSig(channel, signature2, signature1, payment.getSecret(), serverIssuedTransaction,
								payment.paymentToServer);
					}
					/**
					 * Settlements have no locktime, therefore we should broadcast them straight-away.
					 * TODO: This might be a problem if we come back here very often..
					 */
					settlementTransaction.getInput(0).setScriptSig(inputScript);
					peerGroup.broadcastTransaction(settlementTransaction);

				} else {
					TransactionTimerTask newTask = new TransactionTimerTask(peerGroup, transaction);
					timer.schedule(newTask, 2 * 60 * 1000);
					return;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		peerGroup.broadcastTransaction(transaction);
	}

}
