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

import network.thunder.client.communications.objects.UpdateChannelRequestTwo;
import network.thunder.client.communications.objects.UpdateChannelResponseTwo;
import network.thunder.client.database.MySQLConnection;
import network.thunder.client.database.objects.Channel;
import network.thunder.client.database.objects.KeyWrapper;
import network.thunder.client.database.objects.Payment;
import network.thunder.client.database.objects.Secret;
import network.thunder.client.etc.Constants;
import network.thunder.client.etc.ScriptTools;
import network.thunder.client.etc.Tools;

import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.script.Script;

public class UpdateChannelHandlerTwo {
	public Connection conn;
	public Channel channel;	
	
	public int totalAmountOfPayments = 0;
	public ArrayList<Payment> paymentList;
	
	public int amountNewPayments = 0;
	
	
	/**
	 * Payments that are currently in the channel, with payments that should get removed
	 * 		and other payments that should get added.
	 */
	public ArrayList<Payment> currentPayments;
	
	
	/**
	 * currentPayments without the keys that we exposed in the request.
	 * Check back this list with the payments we receive and remove them.
	 */
	ArrayList<Payment> currentPaymentsTemp = new ArrayList<Payment>();
	
	/**
	 * New List of payments of our old channel, that should remain in the new channel.
	 * These payments MUST be included in the transaction that we get back.
	 */
	ArrayList<Payment> currentPaymentsNew = new ArrayList<Payment>();
	
	/**
	 * List of payments where we or the server exposed the secret.
	 * These payments MUST NOT be included in the updated channel.
	 * Instead the balances should be updated accordingly.
	 */
	public ArrayList<Payment> oldPayments = new ArrayList<Payment>();
	
	/**
	 * List of payments that has been added from the server to this channel
	 */
	public ArrayList<Payment> newPayments = new ArrayList<Payment>();
	
	
	/**
	 * List of all payments in the new channel
	 */
	public ArrayList<Payment> newPaymentsTotal = new ArrayList<Payment>();
	
	ArrayList<Payment> paymentsForUpdatedChannelTemp = new ArrayList<Payment>();
	ArrayList<Payment> deletedPayments = new ArrayList<Payment>();

	
	
	
	public UpdateChannelRequestTwo request() throws Exception {
		UpdateChannelRequestTwo request = new UpdateChannelRequestTwo();
		
		channel.getChannelTxClient();
		channel.getChannelTxClientTemp();
		
		currentPayments = MySQLConnection.getPaymentsIncludedInChannel(conn, channel.getId());
		ArrayList<Secret> secretsForSentPayments = new ArrayList<Secret>();
		for(Payment p : currentPayments) {
			if(!p.paymentToServer) {
				if(p.getSecret() != null) {
					secretsForSentPayments.add(new Secret(p.getSecretHash(), p.getSecret()));
					oldPayments.add(p);
				} else {
					currentPaymentsTemp.add(p);
				}
			} else {
				currentPaymentsTemp.add(p);
			}
		}
		
		
		/**
		 * Payments that should not be included in the channel anymore, because the we canceled it.
		 */
		ArrayList<String> deletedPayments = new ArrayList<String>();
		for(Payment p : currentPaymentsTemp) {
			if(!p.paymentToServer) {
				if(Tools.currentTime() - p.getTimestampCreated() < Constants.TIME_TOTAL_PAYMENT && p.getPhase() != 6 && p.getSecret() != null ) {
					paymentsForUpdatedChannelTemp.add(p);
				} else {
					deletedPayments.add(p.getSecretHash());
					p.setPhase(5);
					oldPayments.add(p);
				}
			} else {
				paymentsForUpdatedChannelTemp.add(p);
			}
		}

		currentPaymentsTemp = paymentsForUpdatedChannelTemp;
		
		this.amountNewPayments+=secretsForSentPayments.size();

        for(Secret s : secretsForSentPayments)
            System.out.println(s.secretHash);
		/**
		 * Clean our keylist first, such that only these new keys are used now.
		 */
//		MySQLConnection.deleteUnusedAndExposedKeysFromUs(conn, channel);
//		MySQLConnection.deleteUnusedKeyFromOtherSide(conn, channel);
		
//		request.keyList = MySQLConnection.createKeys(conn, channel, (totalAmountOfPayments-secretsForSentPayments.size())*Constants.KEYS_PER_PAYMENT_CLIENTSIDE+1); 
		request.keyList = MySQLConnection.createKeys(conn, channel, 1); 
		request.secretList = secretsForSentPayments;
		request.removedPayments = deletedPayments;
		return request;
	}
	
	public void evaluate(UpdateChannelResponseTwo m) throws Exception {
		

		MySQLConnection.addKey(conn, m.keyList, channel.getId(), false);
		
	
		/**
		 * Payments that are in the channel already, that the server should receive.
		 * If he has produced the correct secret, the amount of the payment should be
		 * 		added to his balance and the payment is no longer present in the new channel.
		 */
		for(Secret secret : m.secretList) {
			if(!secret.verify())
				throw new Exception("Secret does not hash to correct value");
			
			for(Payment p : currentPaymentsTemp) {
				if(p.paymentToServer) {
					if(p.getSecretHash().equals(secret.secretHash)) {
						/**
						 * A secret has been exposed for a payment towards the server.
						 * This payment is therefore settled, the secret is the proof for us
						 * 		that we sent this payment.
						 */
						p.setSecret(secret.secret);
						oldPayments.add(p);
						break;
					}
					
				}
			}
		}
		this.amountNewPayments+=m.secretList.size();

		
		/**
		 * Payments that we sent that got canceled by the receiver.
		 * The server sent us a list removedPayments including the hashes of the payments.
		 */
		paymentsForUpdatedChannelTemp = new ArrayList<Payment>();
		for(Payment p : currentPaymentsTemp) {
			if(p.paymentToServer) {
				boolean cont = false;
				for(String hash : m.removedPayments) {
					if(hash.equals(p.getSecretHash())) {
						p.setPhase(5);
						oldPayments.add(p);
						MySQLConnection.updatePayment(conn, p);
						cont = true;
						break;
					}
				}
				if(cont) continue;
				if(p.getPhase() == 5) {
					throw new Exception("A payment that we have marked as refunded is included again?!");
				}
				paymentsForUpdatedChannelTemp.add(p);

			}
		}
		currentPaymentsTemp = paymentsForUpdatedChannelTemp;
		
		for(Payment p : currentPaymentsTemp) {
			if(p.getSecret() == null) {
//			if(p.isIncludeInReceiverChannel()) {
					currentPaymentsNew.add(p);
			}
		}
		
		/**
		 * We have completed the list of payments that MUST be in the new channel
		 * 		currentPaymentsNew
		 * and also the list of payments that MUST NOT be in the new channel
		 * 		oldPayments
		 */
		Transaction receivedTransaction = new Transaction(Constants.getNetwork(), Tools.stringToByte(m.channelTransaction));
		

		
		KeyWrapper keyWrapper = MySQLConnection.getKeysPubOnly(conn, channel, true);

		ArrayList<String> newSecretsReceived = new ArrayList<String>();
		
		boolean[] tempList = new boolean[currentPaymentsNew.size()];
		
		int i = 0;
		for(TransactionOutput output : receivedTransaction.getOutputs()) {
			if(i<2) {
				i++;
				continue;
			}
			
			String secretHash = ScriptTools.getRofPaymentScript(output);
			long amount = ( output.getValue().value - Tools.getTransactionFees(Constants.SIZE_OF_SETTLEMENT_TX) );
			
			for(Payment p : oldPayments) {
				if(p.getSecretHash().equals(secretHash)) {
                    System.out.println(receivedTransaction);
                    System.out.println(p.toStringFull());
                    System.out.println(p.toString());
                    throw new Exception("Payment that has been exposed is still in the channel transaction..");
                }
			}
			
			boolean found = false;
			
			Payment newPayment = null;
			
			for(int j=0; j<currentPaymentsNew.size(); ++j) {
				if(currentPaymentsNew.get(j).getSecretHash().equals(secretHash)) {
					newPayment = currentPaymentsNew.get(j);
					found = true;
					tempList[j] = true;
				}
			}
			
			if(!found) {
				newSecretsReceived.add(secretHash);
				newPayment = MySQLConnection.getPayment(conn, secretHash, channel.getId());
				
				if (newPayment != null) {
					if(newPayment.paymentToServer)
						throw new Exception("This should not happen, a payment towards the server has been added (again?)");
					newPayments.add(newPayment);
				}
			} 
			
			if(newPayment == null) {
				/**
				 * A payment has been included, that is not in our database.
				 * We cannot redeem this payment, we shall tell the server that this payment
				 * 		should be dropped.
				 * 
				 * Also take care that the amount is not deducted from our balance.
				 * 
				 * TODO: Add these into the database, and change the protocol.
				 */
				long amountWithFee = output.getValue().value - Tools.getTransactionFees(Constants.SIZE_OF_SETTLEMENT_TX);
				long amountWithoutFee = Tools.calculateServerFeeReverse(amountWithFee) + amountWithFee;
				newPayment = new Payment(0, channel.getId(), amountWithoutFee, null);
				newPayment.setSecretHash(secretHash);
				newPayment.setPhase(6);
				MySQLConnection.addPayment(conn, newPayment);
				newPayments.add(newPayment);
//				throw new Exception("Unknown Payment added to the channel");
			}
			if(!newPayment.paymentToServer) {
				int feeStatus = Tools.checkServerFee(amount, newPayment.getAmount());
				if(feeStatus == -1) {
					throw new Exception("Server Fee is not correct..");
				} else {
					if(feeStatus == 1) {
						/**
						 * Fee was too low? Calculate the proper payment amount..
						 */
						long amountWithFee = output.getValue().value - Tools.getTransactionFees(Constants.SIZE_OF_SETTLEMENT_TX);
						long amountWithoutFee = Tools.calculateServerFeeReverse(amountWithFee) + amountWithFee;
						newPayment.setAmount(amountWithoutFee);
					}
				}
			} else {
				if(amount != newPayment.getAmount())
					throw new Exception("Server Fee is not correct..");
			}
			if(!ScriptTools.checkPaymentScript(output, channel, keyWrapper, secretHash, Constants.CLIENTSIDE, newPayment.paymentToServer ))
				throw new Exception("Payment Script is not correct..");
			
			
			i++;
		}
		
		for(boolean b : tempList) {
			if(!b) 
				throw new Exception("Payment that should be in the channel has not been supplied..");
		}
		
		/**
		 * Calculate the exact server balance after the settlement
		 */
		long serverBalance = channel.getAmountServer();
		for(Payment p : newPayments) {
			serverBalance -= p.getAmount() - Tools.calculateServerFee(p.getAmount());
			newPaymentsTotal.add(p);
		}
		for(Payment p : currentPaymentsNew) {
			if(p.paymentToServer) {
				serverBalance += p.getAmount();
			} else {
				serverBalance -= p.getAmount() - Tools.calculateServerFee(p.getAmount());
			}
			newPaymentsTotal.add(p);
		}
		for(Payment p : oldPayments) {
			if(p.getPhase() != 5 && p.getPhase() != 6) {
				if(p.paymentToServer) {
					serverBalance += p.getAmount();
				} else {
					serverBalance -= p.getAmount() - Tools.calculateServerFee(p.getAmount());
				}
			}
		}
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
		 * Sign it, such that we can give the hash to the server
		 * 	and make sure it spends our opening tx
		 */
		ECDSASignature clientSig = Tools.getSignature(receivedTransaction, 0, channel.getOpeningTx().getOutput(0), channel.getClientKeyOnClient());
		ECDSASignature serverSig = ScriptTools.getSignatureOufOfMultisigInput(receivedTransaction.getInput(0));
		Script inputScript = Tools.getMultisigInputScript(clientSig, serverSig);
		receivedTransaction.getInput(0).setScriptSig(inputScript);
		
		inputScript.correctlySpends(receivedTransaction, 0, channel.getOpeningTx().getOutput(0).getScriptPubKey());
		
//		Transaction channelTransaction = channel.getChannelTxClientTemp();
//		System.out.println(channelTransaction.toString());
		
		/**
		 * Currently we only check that the fee, as the value of all other outputs has been checked.
		 * It is difficult to determine the exact client change, as the client has to pay all the fees.
		 */
//		if(receivedTransaction.getOutput(0).getValue().value > channelTransaction.getOutput(1).getValue().value )
//			throw new Exception("Client change is too high");
		if(receivedTransaction.getOutput(1).getValue().value > serverBalance  ) {
            System.out.println(receivedTransaction);
            System.out.println(serverBalance);
            throw new Exception("Server change is too high");
        }
		if(!Tools.checkTransactionFees(receivedTransaction.getMessageSize(), receivedTransaction, channel.getOpeningTx().getOutput(0)))
			throw new Exception("Transaction fees for channel transaction not correct.");
		
//		System.out.println(receivedTransaction);
		
		channel.setChannelTxClientTemp(receivedTransaction);
		MySQLConnection.setKeysUsed(conn, keyWrapper);
		MySQLConnection.updatePayment(conn, oldPayments);
		
	
	}
}
