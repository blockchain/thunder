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
package network.thunder.core.communication.layer.high;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import network.thunder.core.communication.layer.high.payments.updates.PaymentNew;
import network.thunder.core.communication.layer.high.payments.updates.PaymentRedeem;
import network.thunder.core.communication.layer.high.payments.updates.PaymentRefund;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.ScriptTools;
import network.thunder.core.helper.wallet.WalletHelper;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Channel {

    public int id;
    public NodeKey nodeKeyClient;
    /*
     * Pubkeys for the anchor transactions
     * The 'A' ones will receive payments in case we want to exit the anchor prematurely.
     */
    public ECKey keyClient;
    public ECKey keyServer;

    public int csvDelay;

    /*
     * Revocation 'master hashes' for creating new revocation hashes for new payments.
     */
    public byte[] masterPrivateKeyClient;
    public byte[] masterPrivateKeyServer;
    /*
     * Keeping track of the revocation hashes we gave out.
     * When we open the channel we set the index to some high value and decrease it every X hours.
     * Whenever we commit to a new version of the channel, we use a new child derived from the index.
     */
    public int shaChainDepthCurrent;

    /*
     * Timestamps for the channel management.
     * For now we keep the force close timestamp. It is updated when the channel changed.
     * It is easy to keep track when to force broadcast a channel to the blockchain this way.
     */
    public int timestampOpen;
    public int timestampForceClose;
    /*
     * We also want to save the actual transactions as soon as we see them on the network / create them.
     * While this might not be completely necessary, it allows for efficient lookup in case anything goes wrong and we need it.
     */
    public Sha256Hash anchorTxHash;
    public Transaction anchorTx;
    public int anchorBlockHeight;
    public int minConfirmationAnchor;
    public Transaction spendingTx;

    public ChannelSignatures channelSignatures = new ChannelSignatures();

    /*
     * Data used to construct the channel transactions
     */
    public long amountClient;
    public long amountServer;

    public List<PaymentData> paymentList = new ArrayList<>();

    public int feePerByte;

    //Various revocation hashes are stored here. They get swapped downwards after an exchange (Next->Current; NextNext->Next)
    //Current revocation hash is the one that we have a current valid channel transaction with
    public RevocationHash revoHashClientCurrent;
    public RevocationHash revoHashServerCurrent;

    //Next revocation hash is the hash used when creating a new channel transaction
    public RevocationHash revoHashClientNext;
    public RevocationHash revoHashServerNext;

    //NextNext is the new hash exchanged on the begin of an exchange
    //For now there is no need to store it in the database
    transient public RevocationHash revoHashClientNextNext;
    transient public RevocationHash revoHashServerNextNext;

    public Address addressClient;
    public Address addressServer;

    /*
     * Enum to mark the different phases.
     *
     * These are necessary, as we save the state back to the database after each communication.
     */
    public Phase phase;

    public List<TransactionSignature> closingSignatures;

    public Channel copy () {
        Channel channel = new Channel();
        channel.channelSignatures = this.channelSignatures.copy();

        channel.masterPrivateKeyServer = this.masterPrivateKeyServer;
        channel.masterPrivateKeyClient = this.masterPrivateKeyClient;
        channel.phase = this.phase;
        channel.anchorTx = this.anchorTx == null ? null : new Transaction(Constants.getNetwork(), this.anchorTx.bitcoinSerialize());
        channel.anchorTxHash = this.anchorTxHash;
        channel.closingSignatures = closingSignatures == null ? null : new ArrayList<>(this.closingSignatures);
        channel.id = this.id;
        channel.keyClient = this.keyClient;
        channel.keyServer = this.keyServer;
        channel.nodeKeyClient = this.nodeKeyClient;
        channel.minConfirmationAnchor = this.minConfirmationAnchor;
        channel.shaChainDepthCurrent = this.shaChainDepthCurrent;
        channel.timestampForceClose = this.timestampForceClose;
        channel.timestampOpen = this.timestampOpen;

        channel.amountClient = this.amountClient;
        channel.amountServer = this.amountServer;
        channel.feePerByte = this.feePerByte;
        channel.csvDelay = this.csvDelay;
        channel.addressClient = this.addressClient;
        channel.addressServer = this.addressServer;
        channel.shaChainDepthCurrent = this.shaChainDepthCurrent;
        channel.revoHashClientCurrent = revoHashClientCurrent == null ? null : revoHashClientCurrent.copy();
        channel.revoHashClientNext = revoHashClientNext == null ? null : revoHashClientNext.copy();
        channel.revoHashClientNextNext = revoHashClientNextNext == null ? null : revoHashClientNextNext.copy();
        channel.revoHashServerCurrent = revoHashServerCurrent == null ? null : revoHashServerCurrent.copy();
        channel.revoHashServerNext = revoHashServerNext == null ? null : revoHashServerNext.copy();
        channel.revoHashServerNextNext = revoHashServerNextNext == null ? null : revoHashServerNextNext.copy();

        channel.paymentList = new ArrayList<>(this.paymentList.stream().map(PaymentData::cloneObject).collect(Collectors.toList()));

        return channel;
    }

    public Channel reverse () {
        Channel channel = copy();

        ECKey tempKey = channel.keyServer;
        channel.keyServer = channel.keyClient;
        channel.keyClient = tempKey;

        long tempAmount = channel.amountServer;
        channel.amountServer = channel.amountClient;
        channel.amountClient = tempAmount;

        RevocationHash tempRevocationHash = channel.revoHashServerCurrent;
        channel.revoHashServerCurrent = channel.revoHashClientCurrent;
        channel.revoHashClientCurrent = tempRevocationHash;

        RevocationHash tempRevocationHashNext = channel.revoHashServerNext;
        channel.revoHashServerNext = channel.revoHashClientNext;
        channel.revoHashClientNext = tempRevocationHashNext;

        RevocationHash tempRevocationHashNextNext = channel.revoHashServerNextNext;
        channel.revoHashServerNextNext = channel.revoHashClientNextNext;
        channel.revoHashClientNextNext = tempRevocationHashNextNext;

        Address tempAddress = channel.addressServer;
        channel.addressServer = channel.addressClient;
        channel.addressClient = tempAddress;

        reverseSending(channel.paymentList);

        return channel;
    }

    private List<PaymentData> reverseSending (List<PaymentData> paymentDataList) {
        for (PaymentData payment : paymentDataList) {
            payment.sending = !payment.sending;
        }
        return paymentDataList;
    }

    public void applyUpdate (ChannelUpdate update, boolean ourUpdate) {
        for (PaymentRefund refund : update.refundedPayments) {
            PaymentData paymentData = paymentList.get(refund.paymentIndex);
            if (paymentData.sending) {
                amountServer += paymentData.amount;
            } else {
                amountClient += paymentData.amount;
            }
        }
        for (PaymentRedeem redeem : update.redeemedPayments) {
            PaymentData paymentData = paymentList.get(redeem.paymentIndex);
            if (paymentData.sending) {
                amountClient += paymentData.amount;
            } else {
                amountServer += paymentData.amount;
            }
        }
        for (PaymentNew payment : update.newPayments) {
            if (ourUpdate) {
                amountServer -= payment.amount;
            } else {
                amountClient -= payment.amount;
            }
        }

        List<Integer> removedIndexes = update.getRemovedPaymentIndexes();

        //Sort in descending order, such that we can remove the payments in the list without side effects
        removedIndexes.sort((i1, i2) -> (i2 - i1));

        for (Integer removedIndex : removedIndexes) {
            paymentList.remove(removedIndex.intValue());
        }

        for (PaymentNew payment : update.newPayments) {
            paymentList.add(new PaymentData(payment, ourUpdate));
        }

        this.feePerByte = update.feePerByte;
        this.csvDelay = update.csvDelay;
    }

    public void applyNextRevoHash () {
        Preconditions.checkNotNull(this.revoHashClientNext);
        Preconditions.checkNotNull(this.revoHashServerNext);

        this.revoHashClientCurrent = this.revoHashClientNext;
        this.revoHashServerCurrent = this.revoHashServerNext;

        this.revoHashClientNext = null;
        this.revoHashServerNext = null;
    }

    public void applyNextNextRevoHash () {
        Preconditions.checkNotNull(this.revoHashClientNextNext);
        Preconditions.checkNotNull(this.revoHashServerNextNext);

        this.revoHashClientNext = this.revoHashClientNextNext;
        this.revoHashServerNext = this.revoHashServerNextNext;

        this.revoHashClientNextNext = null;
        this.revoHashServerNextNext = null;
    }

    @Override
    public String toString () {
        return "Channel{" +
                "amountClient=" + amountClient +
                ", amountServer=" + amountServer +
                ", revoServer=" + revoHashServerCurrent +
                ", revoClient=" + revoHashClientCurrent +
                ", revoServerNext=" + revoHashServerNext +
                ", revoClientNext=" + revoHashClientNext +
                ", paymentList=" + paymentList.size() +
                '}';
    }

    public Script getAnchorScript () {
        return getAnchorScriptOutput();
    }

    public Sha256Hash hash;

    public Sha256Hash getHash () {
        if (hash != null) {
            return hash;
        }
        //TODO for now we take the two anchor hashes, this works until we have multiple anchors..
        List<byte[]> list = new ArrayList<>();

        list.add(keyServer.getPubKey());
        list.add(keyClient.getPubKey());
        list.sort(UnsignedBytes.lexicographicalComparator());
        ByteBuffer byteBuffer = ByteBuffer.allocate(list.get(0).length + list.get(1).length);
        byteBuffer.put(list.get(0));
        byteBuffer.put(list.get(1));
        this.hash = Sha256Hash.of(byteBuffer.array());
        return hash;
    }

    public Transaction getAnchorTransactionServer (WalletHelper walletHelper) {
        //TODO test if anchor is complete and just return it..
        if (anchorTx == null) {
            anchorTx = new Transaction(Constants.getNetwork());
        }
        fillAnchorTransactionWithoutSignatures(walletHelper);
        return anchorTx;
    }

    public void addAnchorOutputToAnchor () {
        List<TransactionOutput> outputList = new ArrayList<>();
        outputList.add(new TransactionOutput(
                Constants.getNetwork(),
                null,
                Coin.valueOf(amountClient + amountServer),
                getAnchorScript().getProgram()));

        outputList.addAll(anchorTx.getOutputs());

        Transaction tx = new Transaction(Constants.getNetwork());

        anchorTx.getInputs().stream().forEach(tx::addInput);
        outputList.stream().forEach(tx::addOutput);

        this.anchorTx = tx;
    }

    public void fillAnchorTransactionWithoutSignatures (WalletHelper walletHelper) {
        long totalAmount = amountServer + amountClient;

        if (anchorTx == null) {
            Script anchorScriptServer = getAnchorScriptOutput();
            Script anchorScriptServerP2SH = ScriptTools.scriptToP2SH(anchorScriptServer);
            anchorTx = new Transaction(Constants.getNetwork());
            anchorTx.addOutput(Coin.valueOf(totalAmount), anchorScriptServerP2SH);
        }

        anchorTx = walletHelper.addInputs(anchorTx, amountServer, feePerByte);

        anchorTxHash = anchorTx.getHash();
    }

    //region Script Getter
    public Script getAnchorScriptOutput () {
        return ScriptTools.getAnchorOutputScriptP2SH(keyClient, keyServer);
    }

    //endregion

    public Channel () {
        keyServer = new ECKey();
        anchorTxHash = Sha256Hash.wrap(Tools.getRandomByte(32));

        masterPrivateKeyServer = Tools.getRandomByte(20);
    }

    public Channel (byte[] nodeId, long amount) {
        this();
        amountClient = amount;
        amountServer = amount;
        nodeKeyClient = new NodeKey(nodeId);
    }

    public void retrieveDataFromOtherChannel (Channel channel) {
        keyClient = ECKey.fromPublicOnly(channel.keyServer.getPubKey());
        addressClient = channel.addressServer;
        revoHashClientCurrent = channel.revoHashServerCurrent;
        revoHashClientNext = channel.revoHashServerNext;
        revoHashClientNextNext = channel.revoHashServerNextNext;
        anchorTx = channel.anchorTx;
        anchorTxHash = channel.anchorTxHash;

        masterPrivateKeyClient = channel.masterPrivateKeyServer;
    }

    public enum Phase {
        NEUTRAL(0),
        OPEN(1),
        ESTABLISH_REQUESTED(11),
        ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION(12),
        CLOSE_REQUESTED_CLIENT(52),
        CLOSE_REQUESTED_SERVER(53),
        CLOSE_ON_CHAIN(54),
        CLOSED(50);

        private int value;

        Phase (int value) {
            this.value = value;
        }

        public int getValue () {
            return value;
        }

    }

    //endregion
}