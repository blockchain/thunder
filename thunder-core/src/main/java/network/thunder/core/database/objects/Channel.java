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

import network.thunder.core.etc.Constants;
import network.thunder.core.etc.ScriptTools;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/*
 * TODO: We probably want very flexible rules for channels in the future. Currently, these rules are set as Constants in Constants.class.
 * Add all those constants as columns to the Channels table?
 */

public class Channel {

    private int id;
    private int nodeId;
    /*
     * Pubkeys for the anchor transactions
     * The 'A' ones will receive payments in case we want to exit the anchor prematurely.
     */
    private ECKey KeyClient;
    private ECKey KeyServer;
    private ECKey KeyClientA;
    private ECKey KeyServerA;
    /*
     * Revocation 'master hashes' for creating new revocation hashes for new payments.
     */
    private byte[] masterPrivateKeyClient;
    private byte[] masterPrivateKeyServer;
    /*
     * Keeping track of the revocation hashes we gave out.
     * When we open the channel we set the depth to some high value and decrease it every X hours.
     * Whenever we commit to a new version of the channel, we use a new child derived from the depth.
     */
    private int serverChainDepth;
    private int serverChainChild;
    /*
     * We keep track of the key chain of the other party.
     * Doing so allows us to recreate and check old keys, as we know the depth of the current key we hold without poking around in the dark.
     */
    private int clientChainDepth;
    /*
     * Current and initial balances in microsatoshi ( = 1 / 1000 satoshi )
     * We update the current balances whenever a payment is settled or refunded.
     * Open payments are included in the current amounts.
     */
    private long initialAmountServer;
    private long initialAmountClient;
    private long amountServer;
    private long amountClient;
    /*
     * Timestamps for the channel management.
     * For now we keep the force close timestamp. It is updated when the channel changed.
     * It is easy to keep track when to force broadcast a channel to the blockchain this way.
     */
    private int timestampOpen;
    private int timestampForceClose;
    /*
     * Signatures for broadcasting transactions.
     * Escape and FastEscape Transactions are for claiming our initial funds when something goes wrong before the first commitment or if the other party
     * tries to claim their initial funds after the first commitment.
     */
    private TransactionSignature escapeTxSig;
    private TransactionSignature escapeFastTxSig;
    private TransactionSignature channelTxSig;
    private TransactionSignature channelTxTempSig;
    /*
     * Upcounting version number to keep track which revocation-hash is used with which payments.
     * We increase it, whenever we commited to a new channel.
     */
    private int channelTxVersion;
    /*
     * Hashes to build each channel transaction.
     */
    private Sha256Hash openingTxHashServer;
    private Sha256Hash openingTxHashClient;
    /*
     * The secrets for making the opening transactions.
     * We only use them once or in case the other party tries to cheat on us.
     */
    private byte[] openingSecretServer;
    private byte[] openingSecretClient;
    private byte[] openingSecretHashServer;
    private byte[] openingSecretHashClient;
    /**
     * Enum to mark the different phases.
     * <p>
     * These are necessary, as we save the state back to the database after each communication.
     */
    private Phase phase;
    /*
     * Determines if the channel is ready to make/receive payments.
     * We set this to true once the opening txs have enough confirmations.
     * We set this to false if the channel is closed.
     */
    private boolean isReady;

    private Transaction anchorTransactionServer;

    public boolean verifyEscapeSignatures () {
        if (getEscapeFastTxSig() == null || getEscapeTxSig() == null) {
            return false;
        }

        Transaction escape = getEscapeTransactionServer();
        Transaction fastEscape = getFastEscapeTransactionServer();

        Sha256Hash hashEscape = escape.hashForSignature(0, getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
        Sha256Hash hashFastEscape = fastEscape.hashForSignature(0, getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);

        return (KeyClientA.verify(hashEscape, getEscapeTxSig()) && KeyClientA.verify(hashFastEscape, getEscapeFastTxSig()));
    }

    public Transaction getAnchorTransactionServer (Wallet wallet, HashMap<TransactionOutPoint, Integer> lockedOutputs) {
        if (anchorTransactionServer != null) {
            return anchorTransactionServer;
        }

        long serverAmount = getInitialAmountServer();

        Script anchorScriptServer = getScriptAnchorOutputServer();
        Script anchorScriptServerP2SH = ScriptBuilder.createP2SHOutputScript(anchorScriptServer);

        anchorTransactionServer = Tools.addOutAndInputs(serverAmount, wallet, lockedOutputs, anchorScriptServerP2SH);
        setOpeningTxHashServer(anchorTransactionServer.getHash());
        return anchorTransactionServer;
    }

    public Transaction getEscapeTransactionClient () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getOpeningTxHashClient(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountClient() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptEscapeOutputClient()));
        return escape;
    }

    public Transaction getEscapeTransactionServer () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getOpeningTxHashServer(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountServer() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptEscapeOutputServer()));
        return escape;
    }

    public Transaction getFastEscapeTransactionClient () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getOpeningTxHashClient(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountClient() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptFastEscapeOutputClient()));
        return escape;
    }

    public Transaction getFastEscapeTransactionServer () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getOpeningTxHashServer(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountServer() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptFastEscapeOutputClient()));
        return escape;
    }

    public Script getScriptAnchorOutputServer () {
        return ScriptTools.getAnchorOutputScript(getOpeningSecretHashServer(), getKeyClient(), getKeyClientA(), getKeyServer());
    }

    public Script getScriptAnchorOutputClient () {
        return ScriptTools.getAnchorOutputScript(getOpeningSecretHashServer(), getKeyServer(), getKeyServerA(), getKeyClient());
    }

    public Script getScriptEscapeOutputServer () {
        return ScriptTools.getEscapeOutputScript(getOpeningSecretHashServer(), getKeyServer(), getKeyClient(), Constants.ESCAPE_REVOCATION_TIME);
    }

    public Script getScriptFastEscapeOutputServer () {
        return ScriptTools.getFastEscapeOutputScript(getOpeningSecretHashClient(), getKeyServer(), getKeyClient(), Constants.ESCAPE_REVOCATION_TIME);
    }

    public Script getScriptEscapeOutputClient () {
        return ScriptTools.getEscapeOutputScript(getOpeningSecretHashClient(), getKeyClient(), getKeyServer(), Constants.ESCAPE_REVOCATION_TIME);
    }

    public Script getScriptFastEscapeOutputClient () {
        return ScriptTools.getFastEscapeOutputScript(getOpeningSecretHashServer(), getKeyClient(), getKeyServer(), Constants.ESCAPE_REVOCATION_TIME);
    }

    /**
     * Instantiates a new channel.
     *
     * @param result the result
     * @throws SQLException the SQL exception
     */
    public Channel (ResultSet result) throws SQLException {
        this.setId(result.getInt("id"));
        this.setNodeId(result.getInt("node_id"));

        this.setKeyClient(ECKey.fromPublicOnly(result.getBytes("key_client")));
        this.setKeyServer(ECKey.fromPrivate(result.getBytes("key_server")));

        this.setKeyClientA(ECKey.fromPublicOnly(result.getBytes("key_client_a")));
        this.setKeyServerA(ECKey.fromPrivate(result.getBytes("key_server_a")));

        this.setMasterPrivateKeyClient(result.getBytes("master_priv_key_client"));
        this.setClientChainDepth(result.getInt("client_chain_depth"));

        this.setMasterPrivateKeyServer(result.getBytes("master_priv_key_server"));
        this.setServerChainDepth(result.getInt("server_chain_depth"));
        this.setServerChainChild(result.getInt("server_chain_child"));

        this.setChannelTxVersion(result.getInt("channel_tx_version"));

        this.setInitialAmountServer(result.getLong("initial_amount_server"));
        this.setInitialAmountClient(result.getLong("initial_amount_client"));
        this.setAmountServer(result.getLong("amount_server"));
        this.setAmountClient(result.getLong("amount_client"));

        this.setTimestampOpen(result.getInt("timestamp_open"));
        this.setTimestampForceClose(result.getInt("timestamp_force_close"));

        this.setOpeningTxHashClient(Sha256Hash.wrap(result.getBytes("opening_tx_hash_client")));
        this.setOpeningTxHashClient(Sha256Hash.wrap(result.getBytes("opening_tx_hash_server")));
        this.setOpeningSecretClient(result.getBytes("opening_secret_client"));
        this.setOpeningSecretServer(result.getBytes("opening_secret_server"));
        this.setOpeningSecretHashClient(result.getBytes("opening_secret_hash_client"));
        this.setOpeningSecretHashServer(result.getBytes("opening_secret_hash_server"));

        this.setEscapeTxSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_tx_sig"), true));
        this.setEscapeFastTxSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_fast_tx_sig"), true));
        this.setChannelTxSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_fast_tx_sig"), true));
        this.setChannelTxTempSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_fast_tx_sig"), true));

        this.setPhase(Phase.valueOf(result.getString("phase")));
        this.setReady(Tools.intToBool(result.getInt("is_ready")));

    }

    public Channel () {
    }

    /**
     * Gets the amount client.
     *
     * @return the amount client
     */
    public long getAmountClient () {
        return amountClient;
    }

    /**
     * Sets the amount client.
     *
     * @param amountYou the new amount client
     */
    public void setAmountClient (long amountYou) {
        this.amountClient = amountYou;
    }

    /**
     * Gets the amount server.
     *
     * @return the amount server
     */
    public long getAmountServer () {
        return amountServer;
    }

    /**
     * Sets the amount server.
     *
     * @param amountMe the new amount server
     */
    public void setAmountServer (long amountMe) {
        this.amountServer = amountMe;
    }

    public TransactionSignature getChannelTxSig () {
        return channelTxSig;
    }

    public void setChannelTxSig (TransactionSignature channelTxSig) {
        this.channelTxSig = channelTxSig;
    }

    public TransactionSignature getChannelTxTempSig () {
        return channelTxTempSig;
    }

    public void setChannelTxTempSig (TransactionSignature channelTxTempSig) {
        this.channelTxTempSig = channelTxTempSig;
    }

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

    public TransactionSignature getEscapeFastTxSig () {
        return escapeFastTxSig;
    }

    public void setEscapeFastTxSig (TransactionSignature escapeFastTxSig) {
        this.escapeFastTxSig = escapeFastTxSig;
    }

    public TransactionSignature getEscapeTxSig () {
        return escapeTxSig;
    }

    public void setEscapeTxSig (TransactionSignature escapeTxSig) {
        this.escapeTxSig = escapeTxSig;
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

    /**
     * Gets the initial amount client.
     *
     * @return the initial amount client
     */
    public long getInitialAmountClient () {
        return initialAmountClient;
    }

    /**
     * Sets the initial amount client.
     *
     * @param initialAmountYou the new initial amount client
     */
    public void setInitialAmountClient (long initialAmountYou) {
        this.initialAmountClient = initialAmountYou;
    }

    /**
     * Gets the initial amount server.
     *
     * @return the initial amount server
     */
    public long getInitialAmountServer () {
        return initialAmountServer;
    }

    /**
     * Sets the initial amount server.
     *
     * @param initialAmountMe the new initial amount server
     */
    public void setInitialAmountServer (long initialAmountMe) {
        this.initialAmountServer = initialAmountMe;
    }

    public ECKey getKeyClient () {
        return KeyClient;
    }

    public void setKeyClient (ECKey keyClient) {
        KeyClient = keyClient;
    }

    public ECKey getKeyClientA () {
        return KeyClientA;
    }

    public void setKeyClientA (ECKey keyClientA) {
        KeyClientA = keyClientA;
    }

    public ECKey getKeyServer () {
        return KeyServer;
    }

    public void setKeyServer (ECKey keyServer) {
        KeyServer = keyServer;
    }

    public ECKey getKeyServerA () {
        return KeyServerA;
    }

    public void setKeyServerA (ECKey keyServerA) {
        KeyServerA = keyServerA;
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

    public int getNodeId () {
        return nodeId;
    }

    public void setNodeId (int nodeId) {
        this.nodeId = nodeId;
    }

    public byte[] getOpeningSecretClient () {
        return openingSecretClient;
    }

    public void setOpeningSecretClient (byte[] openingSecretClient) {
        this.openingSecretClient = openingSecretClient;
    }

    public byte[] getOpeningSecretServer () {
        return openingSecretServer;
    }

    public void setOpeningSecretServer (byte[] openingSecretServer) {
        this.openingSecretServer = openingSecretServer;
    }

    public Sha256Hash getOpeningTxHashClient () {
        return openingTxHashClient;
    }

    public void setOpeningTxHashClient (Sha256Hash openingTxHashClient) {
        this.openingTxHashClient = openingTxHashClient;
    }

    public Sha256Hash getOpeningTxHashServer () {
        return openingTxHashServer;
    }

    public void setOpeningTxHashServer (Sha256Hash openingTxHashServer) {
        this.openingTxHashServer = openingTxHashServer;
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
        CLOSED("50");

        private String value;

        private Phase (String value) {
            this.value = value;
        }

        public String getValue () {
            return value;
        }

    }

    public byte[] getOpeningSecretHashServer () {
        return openingSecretHashServer;
    }

    public void setOpeningSecretHashServer (byte[] openingSecretHashServer) {
        this.openingSecretHashServer = openingSecretHashServer;
    }

    public byte[] getOpeningSecretHashClient () {
        return openingSecretHashClient;
    }

    public void setOpeningSecretHashClient (byte[] openingSecretHashClient) {
        this.openingSecretHashClient = openingSecretHashClient;
    }
}