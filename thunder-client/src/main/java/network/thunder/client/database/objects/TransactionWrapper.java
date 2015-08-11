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
package network.thunder.client.database.objects;

import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Transaction;

public class TransactionWrapper {

	int id;
	String hash;
	int channelId;
	int paymentId;
	byte[] data;
	
	Transaction transaction;
	ECDSASignature signature;
	
	public TransactionWrapper(Transaction transaction, ECDSASignature signature) {
		this.transaction = transaction;
		this.signature = signature;
	}
	
	public Transaction getTransaction() {
		return transaction;
	}

	public ECDSASignature getSignature() {
		return signature;
	}

	public TransactionWrapper(Transaction t, int channelId) {
		this.hash = t.getHashAsString();
		this.channelId = channelId;
		this.data = t.bitcoinSerialize();
	}
	public TransactionWrapper(Transaction t, int channelId, int id) {
		this.hash = t.getHashAsString();
		this.channelId = channelId;
		this.data = t.bitcoinSerialize();
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public String getHash() {
		return hash;
	}

	public int getChannelId() {
		return channelId;
	}

	public int getPaymentId() {
		return paymentId;
	}

	public byte[] getData() {
		return data;
	}
}
