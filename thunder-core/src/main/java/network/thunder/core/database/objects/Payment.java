/*
 * ThunderNetwork - Server Client Architecture to send Off-Chain Bitcoin Payments
 * Copyright (C) 2015 Mats Jerratsch <matsjj@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package network.thunder.core.database.objects;

import network.thunder.core.etc.Tools;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Payment {

    /**
     * Whether *in this context* this payment is towards us or towards the node.
     * Depends on the node we are currently talking with.
     */
    public boolean paymentToServer;

    int id;
    int channelIdSender;
    int channelIdReceiver;

    long amount;

    /*
     * Different phases of a payment:
     * <p>
     * 0 - sender requested payment
     * 1 - payment request complete - include in sender channel
     * also add it to the receivers channel next time..
     * <p>
     * 2 -
     * 3 - receiver channel updated, include in both channels
     * 4 - receiver released the secret
     * <p>
     * 10 - settled with sender only
     * 5 - settled with receiver only
     * 11 - payment settled
     * 5 - receiver/server requested refund
     * 6 - receiver refunded/timeouted
     * 12 - receiver and sender refunded (so it's settled aswell..)
     */ int phaseReceiver;
    int phaseSender;
    long fee;

	/*
     * Revocation hash and preimage for this payment.
	 * If we know the preimage, it means the payment made it to the final receiver and we can pull the funds.
	 */

    byte[] secretHash;
    byte[] secret;

    int timestampAddedSender;
    int timestampAddedReceiver;
    int timestampSettledSender;
    int timestampSettledReceiver;

    boolean includeInSenderChannel;
    boolean includeInReceiverChannel;
    boolean includeInReceiverChannelTemp;
    boolean includeInSenderChannelTemp;

	/*
	 * We use the version flags to allow for easier bruteforcing of the P2SH script.
	 * If we don't know at all which payments might be used together with which revocation hashes,
	 *  brute forcing it might be an expensive and lengthy task.
	 */

    int versionAddedSender;
    int versionAddedReceiver;
    int versionSettledSender;
    int versionSettledReceiver;

    /**
     * Instantiates a new payment.
     *
     * @param result the result
     * @throws SQLException the SQL exception
     */
    public Payment (ResultSet result) throws SQLException {
        id = result.getInt("id");
        channelIdReceiver = result.getInt("channel_id_receiver");
        channelIdSender = result.getInt("channel_id_sender");
        amount = result.getLong("amount");
        fee = result.getLong("fee");
        phaseSender = result.getInt("phase_sender");
        phaseReceiver = result.getInt("phase_receiver");

        includeInReceiverChannel = Tools.intToBool(result.getInt("include_in_receiver_channel"));
        includeInSenderChannel = Tools.intToBool(result.getInt("include_in_sender_channel"));

        includeInReceiverChannelTemp = Tools.intToBool(result.getInt("include_in_receiver_channel_temp"));
        includeInSenderChannelTemp = Tools.intToBool(result.getInt("include_in_sender_channel_temp"));

        secretHash = result.getBytes("secret_hash");
        secret = result.getBytes("secret");

        timestampAddedReceiver = result.getInt("timestamp_added_receiver");
        timestampAddedSender = result.getInt("timestamp_added_sender");
        timestampSettledReceiver = result.getInt("timestamp_settled_receiver");
        timestampSettledSender = result.getInt("timestamp_settled_sender");

        versionAddedReceiver = result.getInt("version_added_receiver");
        versionAddedSender = result.getInt("version_added_sender");
        versionSettledReceiver = result.getInt("version_settled_receiver");
        versionSettledSender = result.getInt("version_settled_sender");
    }

    /**
     * Gets the amount.
     *
     * @return the amount
     */
    public long getAmount () {
        return amount;
    }

    /**
     * Sets the amount.
     *
     * @param amount the new amount
     */
    public void setAmount (long amount) {
        this.amount = amount;
    }

    /**
     * Gets the channel id receiver.
     *
     * @return the channel id receiver
     */
    public int getChannelIdReceiver () {
        return channelIdReceiver;
    }

    /**
     * Sets the channel id receiver.
     *
     * @param channelIdReceiver the new channel id receiver
     */
    public void setChannelIdReceiver (int channelIdReceiver) {
        this.channelIdReceiver = channelIdReceiver;
    }

    /**
     * Gets the channel id sender.
     *
     * @return the channel id sender
     */
    public int getChannelIdSender () {
        return channelIdSender;
    }

    /**
     * Sets the channel id sender.
     *
     * @param channelIdSender the new channel id sender
     */
    public void setChannelIdSender (int channelIdSender) {
        this.channelIdSender = channelIdSender;
    }

    public long getFee () {
        return fee;
    }

    public void setFee (long fee) {
        this.fee = fee;
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
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId (int id) {
        this.id = id;
    }

    public int getPhase () {
        if (paymentToServer) {
            return this.phaseSender;
        } else {
            return this.phaseReceiver;
        }
    }

    public void setPhase (int phase) {
        if (paymentToServer) {
            this.phaseSender = phase;
        } else {
            this.phaseReceiver = phase;
        }
    }

    public int getPhaseReceiver () {
        return phaseReceiver;
    }

    public void setPhaseReceiver (int phaseReceiver) {
        this.phaseReceiver = phaseReceiver;
    }

    public int getPhaseSender () {
        return phaseSender;
    }

    public void setPhaseSender (int phaseSender) {
        this.phaseSender = phaseSender;
    }

    public byte[] getSecret () {
        return secret;
    }

    public void setSecret (byte[] secret) {
        this.secret = secret;
    }

    public byte[] getSecretHash () {
        return secretHash;
    }

    public void setSecretHash (byte[] secretHash) {
        this.secretHash = secretHash;
    }

    public int getTimestampAddedReceiver () {
        return timestampAddedReceiver;
    }

    public void setTimestampAddedReceiver (int timestampAddedReceiver) {
        this.timestampAddedReceiver = timestampAddedReceiver;
    }

    public int getTimestampAddedSender () {
        return timestampAddedSender;
    }

    public void setTimestampAddedSender (int timestampAddedSender) {
        this.timestampAddedSender = timestampAddedSender;
    }

    public int getTimestampSettledReceiver () {
        return timestampSettledReceiver;
    }

    public void setTimestampSettledReceiver (int timestampSettledReceiver) {
        this.timestampSettledReceiver = timestampSettledReceiver;
    }

    public int getTimestampSettledSender () {
        return timestampSettledSender;
    }

    public void setTimestampSettledSender (int timestampSettledSender) {
        this.timestampSettledSender = timestampSettledSender;
    }

    public int getVersionAddedReceiver () {
        return versionAddedReceiver;
    }

    public void setVersionAddedReceiver (int versionAddedReceiver) {
        this.versionAddedReceiver = versionAddedReceiver;
    }

    public int getVersionAddedSender () {
        return versionAddedSender;
    }

    public void setVersionAddedSender (int versionAddedSender) {
        this.versionAddedSender = versionAddedSender;
    }

    public int getVersionSettledReceiver () {
        return versionSettledReceiver;
    }

    public void setVersionSettledReceiver (int versionSettledReceiver) {
        this.versionSettledReceiver = versionSettledReceiver;
    }

    public int getVersionSettledSender () {
        return versionSettledSender;
    }

    public void setVersionSettledSender (int versionSettledSender) {
        this.versionSettledSender = versionSettledSender;
    }

    /**
     * Checks if is include in receiver channel.
     *
     * @return true, if is include in receiver channel
     */
    public boolean isIncludeInReceiverChannel () {
        return includeInReceiverChannel;
    }

    /**
     * Sets the include in receiver channel.
     *
     * @param includeInReceiverChannel the new include in receiver channel
     */
    public void setIncludeInReceiverChannel (boolean includeInReceiverChannel) {
        this.includeInReceiverChannel = includeInReceiverChannel;
    }

    public boolean isIncludeInReceiverChannelTemp () {
        return includeInReceiverChannelTemp;
    }

    public void setIncludeInReceiverChannelTemp (boolean includeInReceiverChannelTemp) {
        this.includeInReceiverChannelTemp = includeInReceiverChannelTemp;
    }

    /**
     * Checks if is include in sender channel.
     *
     * @return true, if is include in sender channel
     */
    public boolean isIncludeInSenderChannel () {
        return includeInSenderChannel;
    }

    /**
     * Sets the include in sender channel.
     *
     * @param includeInSenderChannel the new include in sender channel
     */
    public void setIncludeInSenderChannel (boolean includeInSenderChannel) {
        this.includeInSenderChannel = includeInSenderChannel;
    }

    public boolean isIncludeInSenderChannelTemp () {
        return includeInSenderChannelTemp;
    }

    public void setIncludeInSenderChannelTemp (boolean includeInSenderChannelTemp) {
        this.includeInSenderChannelTemp = includeInSenderChannelTemp;
    }

    public boolean isPaymentToServer () {
        return paymentToServer;
    }

    public void setPaymentToServer (boolean paymentToServer) {
        this.paymentToServer = paymentToServer;
    }

    public void setIncludedInChannel (boolean includedInChannelTemp) {
        if (paymentToServer) {
            setIncludeInSenderChannel(includedInChannelTemp);
        } else {
            setIncludeInReceiverChannel(includedInChannelTemp);
        }
    }

    public void setIncludedInChannelTemp (boolean includedInChannelTemp) {
        if (paymentToServer) {
            setIncludeInSenderChannelTemp(includedInChannelTemp);
        } else {
            setIncludeInReceiverChannelTemp(includedInChannelTemp);
        }
    }
}