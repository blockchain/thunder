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

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.helper.WalletHelper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.ScriptTools;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;

/*
 * TODO: We probably want very flexible rules for channels in the future. Currently, these rules are set as Constants in Constants.class.
 * Add all those constants as columns to the Channels table?
 */

public class Channel {

    public int id;
    public int nodeId;
    /*
     * Pubkeys for the anchor transactions
     * The 'A' ones will receive payments in case we want to exit the anchor prematurely.
     */
    public ECKey keyClient;
    public ECKey keyServer;
    public ECKey keyClientA;
    public ECKey keyServerA;
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
     * Current and initial balances in microsatoshi ( = 1 / 1000 satoshi )
     * We update the current balances whenever a payment is settled or refunded.
     * Open payments are included in the current amounts.
     */
    public long initialAmountServer;
    public long initialAmountClient;
    public long amountServer;
    public long amountClient;
    /*
     * Timestamps for the channel management.
     * For now we keep the force close timestamp. It is updated when the channel changed.
     * It is easy to keep track when to force broadcast a channel to the blockchain this way.
     */
    public int timestampOpen;
    public int timestampForceClose;
    /*
     * Signatures for broadcasting transactions.
     * Escape and FastEscape Transactions are for claiming our initial funds when something goes wrong before the first commitment or if the other party
     * tries to claim their initial funds after the first commitment.
     */
    public TransactionSignature escapeTxSig;
    public TransactionSignature fastEscapeTxSig;
    public TransactionSignature channelTxSig;
    public TransactionSignature channelTxTempSig;

    /*
     * We also want to save the actual transactions as soon as we see them on the network / create them.
     * While this might not be completely necessary, it allows for efficient lookup in case anything goes wrong and we need it.
     */
    public Transaction anchorTransactionServer;
    public Transaction anchorTransactionClient;
    public Transaction escapeTxServer;
    public Transaction escapeTxClient;
    public Transaction fastEscapeTxServer;
    public Transaction fastEscapeTxClient;

    /*
     * Upcounting version number to keep track which revocation-hash is used with which payments.
     * We increase it, whenever we commit to a new channel.
     */
    public int channelTxVersion;
    /*
     * Hashes to build each channel transaction.
     */
    public Sha256Hash anchorTxHashServer;
    public Sha256Hash anchorTxHashClient;
    /*
     * The secrets for making the opening transactions.
     * We only use them once or in case the other party tries to cheat on us.
     *
     * The revocation gets revealed to allow for the first commitment, the secret should stay hidden until
     * the end of the channel.
     */
    public byte[] anchorSecretServer;
    public byte[] anchorSecretClient;
    public byte[] anchorSecretHashServer;
    public byte[] anchorSecretHashClient;
    public byte[] anchorRevocationServer;
    public byte[] anchorRevocationHashServer;
    public byte[] anchorRevocationClient;
    public byte[] anchorRevocationHashClient;
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

    public ChannelStatus channelStatus;

    //region Transaction Getter
    /*
     * Various convenience methods for obtaining the different transactions
     */

    public boolean verifyEscapeSignatures () {
        if (getFastEscapeTxSig() == null || getEscapeTxSig() == null) {
            return false;
        }

        Transaction escape = getEscapeTransactionServer();
        Transaction fastEscape = getFastEscapeTransactionServer();

        Sha256Hash hashEscape = escape.hashForSignature(0, getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);
        Sha256Hash hashFastEscape = fastEscape.hashForSignature(0, getScriptAnchorOutputServer(), Transaction.SigHash.ALL, false);

        return (keyClientA.verify(hashEscape, getEscapeTxSig()) && keyClientA.verify(hashFastEscape, getFastEscapeTxSig()));
    }

    public Transaction getAnchorTransactionServer (WalletHelper walletHelper) {
        if (anchorTransactionServer != null) {
            return anchorTransactionServer;
        }

        long serverAmount = getInitialAmountServer();

        Script anchorScriptServer = getScriptAnchorOutputServer();
        Script anchorScriptServerP2SH = ScriptBuilder.createP2SHOutputScript(anchorScriptServer);

        Transaction transaction = new Transaction(Constants.getNetwork());
        transaction.addOutput(Coin.valueOf(serverAmount), anchorScriptServerP2SH);

        anchorTransactionServer = walletHelper.completeInputs(transaction);
        setAnchorTxHashServer(anchorTransactionServer.getHash());
        return anchorTransactionServer;
    }

    public Transaction getEscapeTransactionClient () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getAnchorTxHashClient(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountClient() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptEscapeOutputClient()));
        return escape;
    }

    public Transaction getEscapeTransactionServer () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getAnchorTxHashServer(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountServer() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptEscapeOutputServer()));

        if (getEscapeTxSig() != null) {
            //We have everything we need to sign it..
            Script outputScript = ScriptTools.getAnchorOutputScript(getAnchorSecretHashServer(), keyClient, keyClientA, keyServer);

            TransactionSignature serverSig = Tools.getSignature(escape, 0, outputScript.getProgram(), keyServer);
            Script inputScript = ScriptTools.getEscapeInputScript(
                    getEscapeTxSig().encodeToBitcoin(),
                    serverSig.encodeToBitcoin(),
                    getAnchorSecretServer(),
                    getAnchorSecretHashServer(),
                    getKeyClient(),
                    getKeyClientA(),
                    getKeyServer());

            escape.getInput(0).setScriptSig(inputScript);
        }

        return escape;
    }

    public Transaction getFastEscapeTransactionClient () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getAnchorTxHashClient(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountClient() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptFastEscapeOutputClient()));
        return escape;
    }

    public Transaction getFastEscapeTransactionServer () {
        Transaction escape = new Transaction(Constants.getNetwork());
        escape.addInput(getAnchorTxHashServer(), 0, Tools.getDummyScript());
        Coin output = Coin.valueOf(getInitialAmountServer() - Tools.getTransactionFees(5, 5)); //TODO maybe a better choice for fees?
        escape.addOutput(output, ScriptBuilder.createP2SHOutputScript(getScriptFastEscapeOutputClient()));

        if (getFastEscapeTxSig() != null) {
            //We have everything we need to sign it..
            Script outputScript = ScriptTools.getAnchorOutputScript(getAnchorSecretHashServer(), keyClient, keyClientA, keyServer);

            TransactionSignature serverSig = Tools.getSignature(escape, 0, outputScript.getProgram(), keyServer);
            Script inputScript = ScriptTools.getEscapeInputScript(
                    getEscapeTxSig().encodeToBitcoin(),
                    serverSig.encodeToBitcoin(),
                    getAnchorSecretServer(),
                    getAnchorSecretHashServer(),
                    getKeyClient(),
                    getKeyClientA(),
                    getKeyServer());

            escape.getInput(0).setScriptSig(inputScript);
        }

        return escape;
    }

    /* Get signed transaction for claiming OUR funds that are locked up in a Escape transaction after the timeout.
     */
    public Transaction getEscapeTimeoutRedemptionTransaction (Address payoutAddress, Sha256Hash parent) {
        Transaction redeem = new Transaction(Constants.getNetwork());

        redeem.addInput(parent, 0, Tools.getDummyScript());
        redeem.addOutput(Coin.valueOf(getInitialAmountServer() - Tools.getTransactionFees(5, 5)), payoutAddress); //TODO

        Script outputScript = ScriptTools.getEscapeOutputScript(
                getAnchorRevocationHashServer(),
                getKeyServer(),
                getKeyClient(),
                Constants.ESCAPE_REVOCATION_TIME
        );

        TransactionSignature serverSig = Tools.getSignature(redeem, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getEscapeInputTimeoutScript(
                getAnchorRevocationHashServer(),
                getKeyServer(),
                getKeyClient(),
                Constants.ESCAPE_REVOCATION_TIME,
                serverSig.encodeToBitcoin());

        redeem.getInput(0).setScriptSig(inputScript);

        return redeem;
    }

    /* Get signed transaction for claiming THE OTHER PARTIES funds that are locked up in a Escape transaction after the
     * other party cheated on us by trying to claim their funds even though we engaged in a channel already.
     */
    public Transaction getEscapeRevocationRedemptionTransaction (Address payoutAddress, Sha256Hash parent) {
        if (getAnchorRevocationClient() == null) {
            throw new RuntimeException("We don't have the secret of the client, can't construct revocation..");
        }
        Transaction redeem = new Transaction(Constants.getNetwork());

        redeem.addInput(parent, 0, Tools.getDummyScript());
        redeem.addOutput(Coin.valueOf(getInitialAmountClient() - Tools.getTransactionFees(5, 5)), payoutAddress); //TODO

        //Have to take care here with the server-client notation, since the escape is from the client points of view
        Script outputScript = ScriptTools.getEscapeOutputScript(
                getAnchorRevocationHashClient(),
                getKeyClient(),
                getKeyServer(),
                Constants.ESCAPE_REVOCATION_TIME
        );

        TransactionSignature serverSig = Tools.getSignature(redeem, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getEscapeInputRevocationScript(
                getAnchorRevocationHashClient(),
                getKeyClient(),
                getKeyServer(),
                Constants.ESCAPE_REVOCATION_TIME,
                serverSig.encodeToBitcoin(),
                getAnchorRevocationClient());

        redeem.getInput(0).setScriptSig(inputScript);

        return redeem;
    }

    /* Get signed transaction for claiming OUR funds that are locked up in a Escape transaction after the timeout.
     */
    public Transaction getFastEscapeTimeoutRedemptionTransaction (Address payoutAddress, Sha256Hash parent) {
        Transaction redeem = new Transaction(Constants.getNetwork());

        redeem.addInput(parent, 0, Tools.getDummyScript());
        redeem.addOutput(Coin.valueOf(getInitialAmountServer() - Tools.getTransactionFees(5, 5)), payoutAddress); //TODO

        Script outputScript = ScriptTools.getEscapeOutputScript(
                getAnchorSecretHashServer(),
                getKeyServer(),
                getKeyClient(),
                Constants.ESCAPE_REVOCATION_TIME
        );

        TransactionSignature serverSig = Tools.getSignature(redeem, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getEscapeInputTimeoutScript(
                getAnchorSecretHashServer(),
                getKeyServer(),
                getKeyClient(),
                Constants.ESCAPE_REVOCATION_TIME,
                serverSig.encodeToBitcoin());

        redeem.getInput(0).setScriptSig(inputScript);

        return redeem;
    }

    /* Get signed transaction for claiming OUR funds locked up in a FastEscape transaction using the secret the other party used
     * in their escape transaction.
     */
    public Transaction getFastEscapeRevocationRedemptionTransaction (Address payoutAddress, Sha256Hash parent) {
        if (getAnchorSecretClient() == null) {
            throw new RuntimeException("We don't have the secret of the client, can't construct revocation..");
        }
        Transaction redeem = new Transaction(Constants.getNetwork());

        redeem.addInput(parent, 0, Tools.getDummyScript());
        redeem.addOutput(Coin.valueOf(getInitialAmountClient() - Tools.getTransactionFees(5, 5)), payoutAddress); //TODO

        //Have to take care here with the server-client notation, since the escape is from the client points of view
        Script outputScript = ScriptTools.getFastEscapeOutputScript(
                getAnchorSecretHashClient(),
                getKeyClient(),
                getKeyServer(),
                Constants.ESCAPE_REVOCATION_TIME
        );

        TransactionSignature serverSig = Tools.getSignature(redeem, 0, outputScript.getProgram(), keyServer);

        Script inputScript = ScriptTools.getEscapeInputRevocationScript(
                getAnchorSecretHashClient(),
                getKeyClient(),
                getKeyServer(),
                Constants.ESCAPE_REVOCATION_TIME,
                serverSig.encodeToBitcoin(),
                getAnchorSecretClient());

        redeem.getInput(0).setScriptSig(inputScript);

        return redeem;
    }
    //endregion

    //region Script Getter
    public Script getScriptAnchorOutputServer () {
        return ScriptTools.getAnchorOutputScript(getAnchorSecretHashServer(), getKeyClient(), getKeyClientA(), getKeyServer());
    }

    public Script getScriptAnchorOutputClient () {
        return ScriptTools.getAnchorOutputScript(getAnchorSecretHashClient(), getKeyServer(), getKeyServerA(), getKeyClient());
    }

    public Script getScriptEscapeOutputServer () {
        return ScriptTools.getEscapeOutputScript(getAnchorRevocationHashServer(), getKeyServer(), getKeyClient(), Constants.ESCAPE_REVOCATION_TIME);
    }

    public Script getScriptEscapeOutputClient () {
        return ScriptTools.getEscapeOutputScript(getAnchorRevocationHashClient(), getKeyClient(), getKeyServer(), Constants.ESCAPE_REVOCATION_TIME);
    }

    public Script getScriptFastEscapeOutputServer () {
        return ScriptTools.getFastEscapeOutputScript(getAnchorSecretHashClient(), getKeyServer(), getKeyClient(), Constants.ESCAPE_REVOCATION_TIME);
    }

    public Script getScriptFastEscapeOutputClient () {
        return ScriptTools.getFastEscapeOutputScript(getAnchorSecretHashServer(), getKeyClient(), getKeyServer(), Constants.ESCAPE_REVOCATION_TIME);
    }

    //endregion

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

        this.setAnchorTxHashClient(Sha256Hash.wrap(result.getBytes("anchor_tx_hash_client")));
        this.setAnchorTxHashClient(Sha256Hash.wrap(result.getBytes("anchor_tx_hash_server")));
        this.setAnchorSecretClient(result.getBytes("anchor_secret_client"));
        this.setAnchorSecretServer(result.getBytes("anchor_secret_server"));
        this.setAnchorSecretHashClient(result.getBytes("anchor_secret_hash_client"));
        this.setAnchorSecretHashServer(result.getBytes("anchor_secret_hash_server"));

        this.setAnchorRevocationClient(result.getBytes("anchor_revocation_client"));
        this.setAnchorRevocationServer(result.getBytes("anchor_revocation_server"));
        this.setAnchorRevocationHashClient(result.getBytes("anchor_revocation_hash_client"));
        this.setAnchorRevocationHashServer(result.getBytes("anchor_revocation_hash_server"));

        this.setEscapeTxClient(new Transaction(Constants.getNetwork(), result.getBytes("escape_tx_client")));
        this.setEscapeTxServer(new Transaction(Constants.getNetwork(), result.getBytes("escape_tx_server")));
        this.setFastEscapeTxClient(new Transaction(Constants.getNetwork(), result.getBytes("escape_fast_tx_client")));
        this.setFastEscapeTxServer(new Transaction(Constants.getNetwork(), result.getBytes("escape_fast_tx_server")));

        this.setEscapeTxSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_tx_sig"), true));
        this.setFastEscapeTxSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_fast_tx_sig"), true));
        this.setChannelTxSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_fast_tx_sig"), true));
        this.setChannelTxTempSig(TransactionSignature.decodeFromBitcoin(result.getBytes("escape_fast_tx_sig"), true));

        this.setPhase(Phase.valueOf(result.getString("phase")));
        this.setReady(Tools.intToBool(result.getInt("is_ready")));

    }

    public Channel () {
        keyClient = new ECKey();
        keyClientA = new ECKey();
        keyServer = new ECKey();
        keyServerA = new ECKey();

        anchorTxHashServer = Sha256Hash.wrap(Tools.getRandomByte(32));
        anchorTxHashClient = Sha256Hash.wrap(Tools.getRandomByte(32));

        masterPrivateKeyClient = Tools.getRandomByte(20);
        masterPrivateKeyServer = Tools.getRandomByte(20);

        anchorSecretClient = Tools.getRandomByte(20);
        anchorSecretServer = Tools.getRandomByte(20);
        anchorRevocationClient = Tools.getRandomByte(20);
        anchorRevocationServer = Tools.getRandomByte(20);

        anchorSecretHashClient = Tools.hashSecret(anchorSecretClient);
        anchorSecretHashServer = Tools.hashSecret(anchorSecretServer);
        anchorRevocationHashClient = Tools.hashSecret(anchorRevocationClient);
        anchorRevocationHashServer = Tools.hashSecret(anchorRevocationServer);

        initialAmountClient = 1000000;
        initialAmountServer = 1000000;
        amountClient = 1000000;
        amountServer = 1000000;

        channelStatus = new ChannelStatus();
        channelStatus.amountClient = amountClient;
        channelStatus.amountServer = amountServer;
        channelStatus.feePerByte = 10;
        channelStatus.csvDelay = 24 * 60 * 60;

        anchorTransactionServer = new Transaction(Constants.getNetwork());
        anchorTransactionServer.addInput(Sha256Hash.wrap(Tools.getRandomByte(32)), 0, Tools.getDummyScript());
        anchorTransactionServer.addOutput(Coin.valueOf(1000000), getScriptAnchorOutputServer());

    }

    public void retrieveDataFromOtherChannel (Channel channel) {
        keyClient = channel.keyServer;
        keyClientA = channel.keyServerA;

        masterPrivateKeyClient = channel.masterPrivateKeyServer;
        anchorSecretClient = channel.anchorSecretServer;
        anchorSecretHashClient = channel.anchorSecretHashServer;
        anchorRevocationClient = channel.anchorRevocationServer;
        anchorRevocationHashClient = channel.anchorRevocationHashServer;
        anchorTxHashClient = channel.anchorTxHashServer;

        anchorTransactionClient = channel.anchorTransactionServer;
    }

    //region Getter Setter

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

    public TransactionSignature getFastEscapeTxSig () {
        return fastEscapeTxSig;
    }

    public void setFastEscapeTxSig (TransactionSignature fastEscapeTxSig) {
        this.fastEscapeTxSig = fastEscapeTxSig;
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
        return keyClient;
    }

    public void setKeyClient (ECKey keyClient) {
        this.keyClient = keyClient;
    }

    public ECKey getKeyClientA () {
        return keyClientA;
    }

    public void setKeyClientA (ECKey keyClientA) {
        this.keyClientA = keyClientA;
    }

    public ECKey getKeyServer () {
        return keyServer;
    }

    public void setKeyServer (ECKey keyServer) {
        this.keyServer = keyServer;
    }

    public ECKey getKeyServerA () {
        return keyServerA;
    }

    public void setKeyServerA (ECKey keyServerA) {
        this.keyServerA = keyServerA;
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

    public byte[] getAnchorSecretClient () {
        return anchorSecretClient;
    }

    public void setAnchorSecretClient (byte[] anchorSecretClient) {
        this.anchorSecretClient = anchorSecretClient;
    }

    public byte[] getAnchorSecretServer () {
        return anchorSecretServer;
    }

    public void setAnchorSecretServer (byte[] anchorSecretServer) {
        this.anchorSecretServer = anchorSecretServer;
    }

    public Sha256Hash getAnchorTxHashClient () {
        return anchorTxHashClient;
    }

    public void setAnchorTxHashClient (Sha256Hash anchorTxHashClient) {
        this.anchorTxHashClient = anchorTxHashClient;
    }

    public Sha256Hash getAnchorTxHashServer () {
        return anchorTxHashServer;
    }

    public void setAnchorTxHashServer (Sha256Hash anchorTxHashServer) {
        this.anchorTxHashServer = anchorTxHashServer;
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

    public byte[] getAnchorSecretHashServer () {
        return anchorSecretHashServer;
    }

    public void setAnchorSecretHashServer (byte[] anchorSecretHashServer) {
        this.anchorSecretHashServer = anchorSecretHashServer;
    }

    public byte[] getAnchorSecretHashClient () {
        return anchorSecretHashClient;
    }

    public void setAnchorSecretHashClient (byte[] anchorSecretHashClient) {
        this.anchorSecretHashClient = anchorSecretHashClient;
    }

    public Transaction getAnchorTransactionServer () {
        return anchorTransactionServer;
    }

    public void setAnchorTransactionServer (Transaction anchorTransactionServer) {
        this.anchorTransactionServer = anchorTransactionServer;
    }

    public Transaction getAnchorTransactionClient () {
        return anchorTransactionClient;
    }

    public void setAnchorTransactionClient (Transaction anchorTransactionClient) {
        this.anchorTransactionClient = anchorTransactionClient;
    }

    public Transaction getEscapeTxServer () {
        return escapeTxServer;
    }

    public void setEscapeTxServer (Transaction escapeTxServer) {
        this.escapeTxServer = escapeTxServer;
    }

    public Transaction getEscapeTxClient () {
        return escapeTxClient;
    }

    public void setEscapeTxClient (Transaction escapeTxClient) {
        this.escapeTxClient = escapeTxClient;
    }

    public Transaction getFastEscapeTxServer () {
        return fastEscapeTxServer;
    }

    public void setFastEscapeTxServer (Transaction fastEscapeTxServer) {
        this.fastEscapeTxServer = fastEscapeTxServer;
    }

    public Transaction getFastEscapeTxClient () {
        return fastEscapeTxClient;
    }

    public void setFastEscapeTxClient (Transaction fastEscapeTxClient) {
        this.fastEscapeTxClient = fastEscapeTxClient;
    }

    public byte[] getAnchorRevocationServer () {
        return anchorRevocationServer;
    }

    public void setAnchorRevocationServer (byte[] anchorRevocationServer) {
        this.anchorRevocationServer = anchorRevocationServer;
    }

    public byte[] getAnchorRevocationHashServer () {
        return anchorRevocationHashServer;
    }

    public void setAnchorRevocationHashServer (byte[] anchorRevocationHashServer) {
        this.anchorRevocationHashServer = anchorRevocationHashServer;
    }

    public byte[] getAnchorRevocationClient () {
        return anchorRevocationClient;
    }

    public void setAnchorRevocationClient (byte[] anchorRevocationClient) {
        this.anchorRevocationClient = anchorRevocationClient;
    }

    public byte[] getAnchorRevocationHashClient () {
        return anchorRevocationHashClient;
    }

    public void setAnchorRevocationHashClient (byte[] anchorRevocationHashClient) {
        this.anchorRevocationHashClient = anchorRevocationHashClient;
    }
    //endregion
}