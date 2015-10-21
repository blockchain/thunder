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
package network.thunder.server.database.objects;

import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Transaction;

// TODO: Auto-generated Javadoc

/**
 * The Class TransactionWrapper.
 */
public class TransactionWrapper {

    /**
     * The id.
     */
    int id;

    /**
     * The hash.
     */
    String hash;

    /**
     * The channel id.
     */
    int channelId;

    /**
     * The payment id.
     */
    int paymentId;

    /**
     * The data.
     */
    byte[] data;

    /**
     * The transaction.
     */
    Transaction transaction;

    /**
     * The signature.
     */
    ECDSASignature signature;

    /**
     * Instantiates a new transaction wrapper.
     *
     * @param transaction the transaction
     * @param signature   the signature
     */
    public TransactionWrapper (Transaction transaction, ECDSASignature signature) {
        this.transaction = transaction;
        this.signature = signature;
    }

    /**
     * Instantiates a new transaction wrapper.
     *
     * @param t         the t
     * @param channelId the channel id
     */
    public TransactionWrapper (Transaction t, int channelId) {
        this.hash = t.getHashAsString();
        this.channelId = channelId;
        this.data = t.bitcoinSerialize();
    }

    /**
     * Instantiates a new transaction wrapper.
     *
     * @param t         the t
     * @param channelId the channel id
     * @param id        the id
     */
    public TransactionWrapper (Transaction t, int channelId, int id) {
        this.hash = t.getHashAsString();
        this.channelId = channelId;
        this.data = t.bitcoinSerialize();
        this.id = id;
    }

    /**
     * Gets the channel id.
     *
     * @return the channel id
     */
    public int getChannelId () {
        return channelId;
    }

    /**
     * Gets the data.
     *
     * @return the data
     */
    public byte[] getData () {
        return data;
    }

    /**
     * Gets the hash.
     *
     * @return the hash
     */
    public String getHash () {
        return hash;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public int getId () {
        return id;
    }

    /**
     * Gets the payment id.
     *
     * @return the payment id
     */
    public int getPaymentId () {
        return paymentId;
    }

    /**
     * Gets the signature.
     *
     * @return the signature
     */
    public ECDSASignature getSignature () {
        return signature;
    }

    /**
     * Gets the transaction.
     *
     * @return the transaction
     */
    public Transaction getTransaction () {
        return transaction;
    }
}
