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

import java.nio.channels.NotYetConnectedException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;

import network.thunder.client.api.ThunderContext;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.Output;
import network.thunder.client.etc.Constants;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionConfidence.ConfidenceType;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletEventListener;
import org.bitcoinj.script.Script;

public class TransactionStorage implements WalletEventListener {
	
	Connection conn;
	PeerGroup peerGroup;

	private static boolean first = true;
	
	ArrayList<Channel> openingTransactions = new ArrayList<Channel>();
	ArrayList<Channel> channelTransactions = new ArrayList<Channel>();
	
	HashMap<Integer, ArrayList<TransactionTimerTask>> additionalTransactionList = new HashMap<Integer, ArrayList<TransactionTimerTask>>();
	HashMap<Integer, TransactionTimerTask> refundTransactionList = new HashMap<Integer, TransactionTimerTask>();
	HashMap<Integer, TransactionTimerTask> channelTransactionList = new HashMap<Integer, TransactionTimerTask>();
	
	Timer timer;

    ArrayList<Output> outputList = new ArrayList<>();
	
	private TransactionStorage() {}
	
	public static TransactionStorage initialize(Connection conn, ArrayList<Output> outputList)  throws SQLException {
		TransactionStorage storage = new TransactionStorage();
		storage.conn = conn;
        storage.outputList = outputList;
		
		
		storage.channelTransactions = MySQLConnection.getChannelTransactions(conn);
		storage.openingTransactions = MySQLConnection.getOpeningTransactions(conn);
		
		return storage;
		
	}
	
	public void rebroadcastOpeningTransactions(Peer peer) throws NotYetConnectedException, SQLException {
		for(Channel c : openingTransactions) {
			peer.sendMessage(c.getOpeningTx());
		}
	}
	
	
	public void addOpenedChannel(Channel channel) {
		openingTransactions.add(channel);
	}
	
	private void addFinishedChannel(Channel channel) throws SQLException {
		channelTransactions.add(channel);
		int i=0;
		for(Channel c : openingTransactions) {
			if(c.getPubKeyClient().equals(channel.getPubKeyClient())) {
				openingTransactions.remove(i);
				c.setReady(true);
				c.setEstablishPhase(0);
				MySQLConnection.updateChannel(conn, channel);
				return;
			}
			i++;
		}
	}
	
	public void updateOutputs(Wallet wallet, boolean forceUpdate) {
		boolean update = forceUpdate;
		for (TransactionOutput o : wallet.calculateAllSpendCandidates()) {
			if(o.getParentTransaction().getConfidence().getDepthInBlocks() < 10) {
				update = true;
			}
		}
		if(!update && !first) 
			return;
		first = false;
		outputList.clear();
		for (TransactionOutput o : wallet.calculateAllSpendCandidates()) {
			if(o.getParentTransaction().getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATION_TIME) {
				outputList.add(new Output(o, wallet));
			}
		}
	}
	
	public void onTransaction(Transaction transaction) throws SQLException {
		for(Channel c : openingTransactions) {
			if(transaction.getHashAsString().equals(c.getOpeningTxHash())) {
				
				TransactionConfidence confidence = transaction.getConfidence();
				
				if(confidence.getConfidenceType() == ConfidenceType.DEAD) {
					//TODO: Remove this transaction and the channel, somehow it got overwritten..
				}
				
				if(confidence.getDepthInBlocks() >= Constants.MIN_CONFIRMATION_TIME_FOR_CHANNEL) {
					c.setOpeningTx(transaction);
					addFinishedChannel(c);
					return;
				}

			}
		}

		
		boolean channelOutput = false;
		for(TransactionInput input : transaction.getInputs()) {
			if(input.getOutpoint().getIndex() == 0) {
				for(Channel c : channelTransactions) {
					if(c.getChannelTxServer() != null) {
						if(input.getOutpoint().getHash().toString().equals(c.getChannelTxServer().getHashAsString())) {
							//TODO: Somehow the channel got closed, find out how (correctly?) and act accordingly..
						}
					}
				}
			}
		}
		
		
	}

	@Override
	public void onKeysAdded(List<ECKey> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCoinsReceived(Wallet arg0, Transaction arg1, Coin arg2,
			Coin arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCoinsSent(Wallet arg0, Transaction arg1, Coin arg2, Coin arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onReorganize(Wallet arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onScriptsChanged(Wallet arg0, List<Script> arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTransactionConfidenceChanged(Wallet arg0, Transaction arg1) {
		try {
			onTransaction(arg1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void onWalletChanged(Wallet arg0) {
		// TODO Auto-generated method stub
		updateOutputs(arg0, false);

	}

}
