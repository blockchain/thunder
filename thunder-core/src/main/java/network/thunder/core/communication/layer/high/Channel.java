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
    public byte[] nodeKeyClient;
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
     * When we open the channel we set the depth to some high value and decrease it every X hours.
     * Whenever we commit to a new version of the channel, we use a new child derived from the depth.
     */
    public int serverChainDepth;
    public int serverChainChild;
    /*
     * We keep track of the key chain of the other party.
     * Doing so allows us to recreate and check old keys, as we know the depth of the current key we hold without poking around in the dark.
     */
    public int clientChainDepth;

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
    public List<TransactionSignature> anchorSignature;
    public int minConfirmationAnchor;

    public ChannelStatus channelStatus;
    public ChannelSignatures channelSignatures = new ChannelSignatures();
    /*
     * Upcounting version number to keep track which revocation-hash is used with which payments.
     * We increase it, whenever we commit to a new channel.
     */
    public int channelTxVersion;

    /*
     * Enum to mark the different phases.
     *
     * These are necessary, as we save the state back to the database after each communication.
     */
    public Phase phase;
    /*
     * Determines if the channel is ready to make/receive payments.
     * We set this to true once the opening txs have enough confirmations.
     * We set this to false if the channel is closed.
     */
    public boolean isReady;

    public boolean requestedClose;


    public List<TransactionSignature> closingSignatures;

    public void setNodeKeyClient (byte[] nodeKeyClient) {
        this.nodeKeyClient = nodeKeyClient;
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

    public void addAnchorOutputToAnchor() {
        List<TransactionOutput> outputList = new ArrayList<>();
        outputList.add(new TransactionOutput(
                Constants.getNetwork(),
                null,
                Coin.valueOf(channelStatus.amountClient+ channelStatus.amountServer),
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
        return ScriptTools.getAnchorOutputScriptP2SH(getKeyClient(), getKeyServer());
    }

    //endregion

    public Channel () {
        keyServer = new ECKey();
        channelStatus = new ChannelStatus();
        anchorTxHash = Sha256Hash.wrap(Tools.getRandomByte(32));

        System.out.println("keyServer = " + keyServer);

        masterPrivateKeyServer = Tools.getRandomByte(20);
    }

    public Channel (byte[] nodeId, long amount) {
        this();
        channelStatus.amountClient = amount;
        channelStatus.amountServer = amount;
        nodeKeyClient = nodeId;
        setIsReady(false);
    }

    public void retrieveDataFromOtherChannel (Channel channel) {
        keyClient = channel.keyServer;
        channelStatus.addressClient = channel.channelStatus.addressServer;
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

    //region Getter Setter


    public int getChannelTxVersion () {
        return channelTxVersion;
    }

    public void setChannelTxVersion (int channelTxVersion) {
        this.channelTxVersion = channelTxVersion;
    }

    public int getClientChainDepth () {
        return clientChainDepth;
    }

    public void setClientChainDepth (int clientChainDepth) {
        this.clientChainDepth = clientChainDepth;
    }

    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public ECKey getKeyClient () {
        return keyClient;
    }

    public void setKeyClient (ECKey keyClient) {
        this.keyClient = keyClient;
    }

    public ECKey getKeyServer () {
        return keyServer;
    }

    public void setKeyServer (ECKey keyServer) {
        this.keyServer = keyServer;
    }

    public byte[] getMasterPrivateKeyClient () {
        return masterPrivateKeyClient;
    }

    public void setMasterPrivateKeyClient (byte[] masterPrivateKeyClient) {
        this.masterPrivateKeyClient = masterPrivateKeyClient;
    }

    public byte[] getMasterPrivateKeyServer () {
        return masterPrivateKeyServer;
    }

    public void setMasterPrivateKeyServer (byte[] masterPrivateKeyServer) {
        this.masterPrivateKeyServer = masterPrivateKeyServer;
    }


    public Phase getPhase () {
        return phase;
    }

    public void setPhase (Phase phase) {
        this.phase = phase;
    }

    public int getServerChainChild () {
        return serverChainChild;
    }

    public void setServerChainChild (int serverChainChild) {
        this.serverChainChild = serverChainChild;
    }

    public int getServerChainDepth () {
        return serverChainDepth;
    }

    public void setServerChainDepth (int serverChainDepth) {
        this.serverChainDepth = serverChainDepth;
    }

//    /**
//     * New master key.
//     *
//     * @param masterKey the master key
//     * @throws Exception the exception
//     */
//    public void newMasterKey (RevocationHash masterKey) throws Exception {
//        if (getMasterPrivateKeyClient() != null) {
//            /*
//             * Make sure the old masterPrivateKey is a child of this one..
//			 */

//            DeterministicKey key = DeterministicKey.deserializeB58(masterKey.privateKey, Constants.getNetwork());
//            DeterministicHierarchy hierachy = new DeterministicHierarchy(key);

//            List<ChildNumber> childList = HashDerivation.getChildList(getMasterChainDepth() - masterKey.depth);
//            DeterministicKey keyDerived = hierachy.get(childList, true, true);

//            if (!HashDerivation.compareDeterministicKeys(keyDerived, getMasterPrivateKeyClient())) {
//                throw new Exception("The new masterPrivateKey is not a parent of the one we have..");
//            }
//        }

    /**
     * Gets the timestamp force close.
     *
     * @return the timestamp force close
     */
    public int getTimestampForceClose () {
        return timestampForceClose;
    }
//        setMasterPrivateKeyClient(masterKey.getSecretAsString());
//        setMasterChainDepth(masterKey.getDepth());
//    }

    /**
     * Sets the timestamp force close.
     *
     * @param timestampForceClose the new timestamp force close
     */
    public void setTimestampForceClose (int timestampForceClose) {
        this.timestampForceClose = timestampForceClose;
    }

    /**
     * Gets the timestamp open.
     *
     * @return the timestamp open
     */
    public int getTimestampOpen () {
        return timestampOpen;
    }

    /**
     * Sets the timestamp open.
     *
     * @param timestampOpen the new timestamp open
     */
    public void setTimestampOpen (int timestampOpen) {
        this.timestampOpen = timestampOpen;
    }

    /**
     * Checks if is ready.
     *
     * @return true, if is ready
     */
    public boolean isReady () {
        return isReady;
    }

    /**
     * Sets the ready.
     *
     * @param isReady the new ready
     */
    public void setReady (boolean isReady) {
        this.isReady = isReady;
    }

    public void setIsReady (boolean isReady) {
        this.isReady = isReady;
    }

    public enum Phase {
        NEUTRAL("0"),
        ESTABLISH_REQUESTED("11"),
        ESTABLISH_WAITING_FOR_BLOCKCHAIN_CONFIRMATION("12"),
        PAYMENT_REQUESTED("21"),
        UPDATE_REQUESTED("31"),
        CLOSE_REQUESTED_CLIENT("52"),
        CLOSE_REQUESTED_SERVER("53"),
        CLOSED("50");

        private String value;

        Phase (String value) {
            this.value = value;
        }

        public String getValue () {
            return value;
        }

    }

    //endregion
}