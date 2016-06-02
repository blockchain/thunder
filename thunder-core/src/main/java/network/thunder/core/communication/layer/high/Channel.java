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

import com.google.common.primitives.UnsignedBytes;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.ScriptTools;
import network.thunder.core.helper.wallet.WalletHelper;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Channel {

    public int id;
    public NodeKey nodeKeyClient;
    private Sha256Hash hash;
    /*
     * Pubkeys for the anchor transactions
     * The 'A' ones will receive payments in case we want to exit the anchor prematurely.
     */
    public ECKey keyClient;
    public ECKey keyServer;
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
    public int minConfirmationAnchor;

    public ChannelStatus channelStatus;
    public ChannelSignatures channelSignatures = new ChannelSignatures();
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
        channel.channelStatus = this.channelStatus.copy();

        channel.timestampOpen = this.timestampOpen;
        channel.masterPrivateKeyServer = this.masterPrivateKeyServer;
        channel.masterPrivateKeyClient = this.masterPrivateKeyClient;
        channel.phase = this.phase;
        channel.anchorTx = new Transaction(Constants.getNetwork(), this.anchorTx.bitcoinSerialize());
        channel.anchorTxHash = this.anchorTxHash;
        channel.closingSignatures = closingSignatures == null ? null : new ArrayList<>(this.closingSignatures);
        channel.hash = this.hash;
        channel.id = this.id;
        channel.keyClient = this.keyClient;
        channel.keyServer = this.keyServer;
        channel.nodeKeyClient = this.nodeKeyClient;
        channel.minConfirmationAnchor = this.minConfirmationAnchor;
        channel.shaChainDepthCurrent = this.shaChainDepthCurrent;
        channel.timestampForceClose = this.timestampForceClose;
        channel.timestampOpen = this.timestampOpen;
        return channel;
    }

    public Script getAnchorScript () {
        return getAnchorScriptOutput();
    }

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
        hash = Sha256Hash.of(byteBuffer.array());
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
                Coin.valueOf(channelStatus.amountClient + channelStatus.amountServer),
                getAnchorScript().getProgram()));

        outputList.addAll(anchorTx.getOutputs());

        Transaction tx = new Transaction(Constants.getNetwork());

        anchorTx.getInputs().stream().forEach(tx::addInput);
        outputList.stream().forEach(tx::addOutput);

        this.anchorTx = tx;
    }

    public void fillAnchorTransactionWithoutSignatures (WalletHelper walletHelper) {
        long totalAmount = channelStatus.amountServer + channelStatus.amountClient;

        if (anchorTx == null) {
            Script anchorScriptServer = getAnchorScriptOutput();
            Script anchorScriptServerP2SH = ScriptBuilder.createP2SHOutputScript(anchorScriptServer);
            anchorTx = new Transaction(Constants.getNetwork());
            anchorTx.addOutput(Coin.valueOf(totalAmount), anchorScriptServerP2SH);
        }

        anchorTx = walletHelper.addInputs(anchorTx, channelStatus.amountServer, channelStatus.feePerByte);

        anchorTxHash = anchorTx.getHash();
    }

    //region Script Getter
    public Script getAnchorScriptOutput () {
        return ScriptTools.getAnchorOutputScriptP2SH(keyClient, keyServer);
    }

    //endregion

    public Channel () {
        keyServer = new ECKey();
        channelStatus = new ChannelStatus();
        anchorTxHash = Sha256Hash.wrap(Tools.getRandomByte(32));

        masterPrivateKeyServer = Tools.getRandomByte(20);
    }

    public Channel (byte[] nodeId, long amount) {
        this();
        channelStatus.amountClient = amount;
        channelStatus.amountServer = amount;
        nodeKeyClient = new NodeKey(nodeId);
    }

    public void retrieveDataFromOtherChannel (Channel channel) {
        keyClient = channel.keyServer;
        channelStatus.addressClient = channel.channelStatus.addressServer;
        channelStatus.revoHashClientCurrent = channel.channelStatus.revoHashServerCurrent;
        channelStatus.revoHashClientNext = channel.channelStatus.revoHashServerNext;
        anchorTx = channel.anchorTx;
        anchorTxHash = channel.anchorTxHash;

        masterPrivateKeyClient = channel.masterPrivateKeyServer;
    }

    @Override
    public String toString () {
        return "Channel{" +
                "channelStatus=" + channelStatus +
                '}';
    }

    public enum Phase {
        NEUTRAL(0),
        OPEN(1),
        ESTABLISH_REQUESTED(11),
        ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION(12),
        PAYMENT_REQUESTED(21),
        UPDATE_REQUESTED(31),
        CLOSE_REQUESTED_CLIENT(52),
        CLOSE_REQUESTED_SERVER(53),
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