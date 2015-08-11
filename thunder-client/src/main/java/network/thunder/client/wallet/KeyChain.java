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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Output;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.SideConstants;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionConfidence;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.DeterministicSeed;

import com.lambdaworks.codec.Base64;

public class KeyChain {
	
	public WalletAppKit kit;
	
	public Wallet wallet;
	public PeerGroup peerGroup;
	public TransactionStorage transactionStorage;
	
	public Connection conn;
	
	String KEY = "episode slice essence biology cream broccoli agree poverty sentence piano eyebrow air";
	
	
	public KeyChain(Connection conn) throws Exception {
		this.conn = conn;		
	}
	
	public void start() throws Exception {
		
        String seedCode = "episode slice essence biology cream broccoli agree poverty sentence piano eyebrow air";
        String passphrase = "";
        Long creationtime = 1409478661L;
        DeterministicSeed seed = new DeterministicSeed(seedCode, null, passphrase, creationtime);

		
		kit = new WalletAppKit(Constants.getNetwork(), new File("."), SideConstants.WALLET_FILE);
//		kit.restoreWalletFromSeed(seed);
		

        // In case you want to connect with your local bitcoind tell the kit to connect to localhost.
        // You must do that in reg test mode.
//        kit.connectToLocalHost();
        

        // Now we start the kit and sync the blockchain.
        // bitcoinj is working a lot with the Google Guava libraries. The WalletAppKit extends the AbstractIdleService. Have a look at the introduction to Guava services: https://code.google.com/p/guava-libraries/wiki/ServiceExplained
        kit.startAsync();
        kit.awaitRunning();

        
//        kit.wallet().reset();

        

        // To observe wallet events (like coins received) we implement a EventListener class that extends the AbstractWalletEventListener bitcoinj then calls the different functions from the EventListener class
        WalletListener wListener = new WalletListener();
        kit.wallet().addEventListener(wListener);

        // Ready to run. The kit syncs the blockchain and our wallet event listener gets notified when something happens.
        // To test everything we create and print a fresh receiving address. Send some coins to that address and see if everything works.
//        System.out.println(kit.wallet().toString(false, false, false, null));
        System.out.println(kit.wallet().getBalance().toFriendlyString());
        System.out.println("send money to: " + kit.wallet().freshReceiveAddress().toString());
		
        System.out.println(Tools.byteToString58(kit.wallet().getKeyChainSeed().getSeedBytes()));
        System.out.println(Tools.byteToString58(kit.wallet().getKeyChainSeed().getSecretBytes()));
        System.out.println(kit.wallet().getKeyChainSeed().getMnemonicCode());

        
        peerGroup = kit.peerGroup();
		wallet = kit.wallet();
		MySQLConnection.deleteAllOutputs(conn);
		for (TransactionOutput o : wallet.calculateAllSpendCandidates()) {
			System.out.println(o.getParentTransaction().getConfidence().getDepthInBlocks() + " " +o);
        	try {
        		if(transactionStorage != null)
        			transactionStorage.onTransaction(o.getParentTransaction());
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if(o.getParentTransaction().getConfidence().getDepthInBlocks() >= Constants.MIN_CONFIRMATION_TIME) {
				try {
			    	Output output = new Output();
			    	output.setVout(o.getIndex());
			    	output.setHash(o.getParentTransaction().getHash().toString());
			    	output.setValue(o.getValue().value);
			    	output.setPrivateKey(new String(Base64.encode(wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript(Constants.getNetwork()).getHash160()).getPrivKeyBytes())));
			    	output.setTransactionOutput(o);
					
			    	MySQLConnection.addOutput(conn, output);
	
		    	} catch(Exception e) {
		    		e.printStackTrace();
		    	}
			}
		}
		
//		while(true) {
//			System.out.println(wallet.toString());
//			try {
//				Thread.sleep(10000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
	
	 class WalletListener extends AbstractWalletEventListener {

	        @Override
	        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
	        	
	        	
	        	
	        	
	            System.out.println("-----> coins resceived: " + tx.getHashAsString());
	            System.out.println("received: " + tx.getValue(wallet));
	            System.out.println("send money to: " + kit.wallet().freshReceiveAddress().toString());
	        }

	        @Override
	        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
	        	
	        	try {
	        		if(transactionStorage != null)
	        			transactionStorage.onTransaction(tx);
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	
	            TransactionConfidence confidence = tx.getConfidence();
	            
	        	if(confidence.getDepthInBlocks() < 10 ) {
		            System.out.println("-----> confidence changed: " + tx.getHashAsString());
		            System.out.println("new block depth: " + confidence.getDepthInBlocks());
	        	}
	        	
	            if(confidence.getDepthInBlocks() == Constants.MIN_CONFIRMATION_TIME) {
		        	for(TransactionOutput o : tx.getOutputs()) {
		        		try {
			        	Output output = new Output();
			        	output.setVout(o.getIndex());
			        	output.setHash(tx.getHashAsString());
			        	output.setValue(o.getValue().value);
			        	output.setPrivateKey(new String(Base64.encode(wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript(Constants.getNetwork()).getHash160()).getPrivKeyBytes())));
				    	output.setTransactionOutput(o);

		        		try {
							MySQLConnection.addOutput(conn, output);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
//							e.printStackTrace();
						}
			        	} catch(Exception e) {
//			        		e.printStackTrace();
			        	}
		        	}
	            }
	        }

	        @Override
	        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
	            System.out.println("coins sent");
	        }

	        @Override
	        public void onReorganize(Wallet wallet) {
	        }

	        @Override
	        public void onWalletChanged(Wallet wallet) {
	        }

	        @Override
	        public void onKeysAdded(List<ECKey> keys) {
	            System.out.println("new key added");
	        }

//	        @Override
//	        public void onScriptsChanged(Wallet wallet, List<Script> scripts, boolean isAddingScripts) {
//	            System.out.println("new script added");
//	        }
	    }
	 
	 

}
