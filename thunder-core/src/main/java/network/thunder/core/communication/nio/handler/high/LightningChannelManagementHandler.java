/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package network.thunder.core.communication.nio.handler.high;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.Type;
import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageA;
import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageB;
import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageC;
import network.thunder.core.communication.objects.lightning.establish.EstablishChannelMessageD;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.ScriptTools;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.Node;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Handler for opening new channels.
 * <p>
 * Channel establishment consists of 2 requests and responses.
 * If the process gets interrupted at any point (eg network failure), it has to be started from scratch again (no recovery).
 * <p>
 * The complete channel will only get saved to the database once the four requests has been completed.
 */
public class LightningChannelManagementHandler extends ChannelInboundHandlerAdapter {

    public boolean initialized = false;
    ArrayList<Channel> channelList = new ArrayList<>();
    private Node node;
    private boolean isServer = false;
    private ECKey key;

    protected Channel newChannel;

    private int status = 0;

    Wallet wallet;
    HashMap<TransactionOutPoint, Integer> lockedOutputs;

    public LightningChannelManagementHandler (HashMap<TransactionOutPoint, Integer> lockedOutputs, Wallet wallet, boolean isServer, Node node) {
        this.lockedOutputs = lockedOutputs;
        this.wallet = wallet;
        this.isServer = isServer;
        this.node = node;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) {
        initialize();
        if (channelList.size() > 0) {
            ctx.fireChannelActive();
        }
        if (!isServer) {
            sendEstablishMessageA(ctx);
        }
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {

        Message message = (Message) msg;
        if (message.type >= 110 && message.type < 199) {

            if (message.type == Type.ESTABLISH_CHANNEL_A) {
                EstablishChannelMessageA m = new Gson().fromJson(message.data, EstablishChannelMessageA.class);
                prepareNewChannel();

                newChannel.setInitialAmountServer(m.getClientAmount());
                newChannel.setAmountServer(m.getClientAmount());
                newChannel.setInitialAmountClient(m.getServerAmount());
                newChannel.setAmountClient(m.getServerAmount());

                newChannel.setKeyClient(ECKey.fromPublicOnly(m.getPubKey()));
                newChannel.setKeyClientA(ECKey.fromPublicOnly(m.getPubKeyFE()));
                newChannel.setAnchorSecretHashClient(m.getSecretHashFE());

                sendEstablishMessageB(ctx);

            } else if (message.type == Type.ESTABLISH_CHANNEL_B) {
                EstablishChannelMessageB m = new Gson().fromJson(message.data, EstablishChannelMessageB.class);

                newChannel.setKeyClient(ECKey.fromPublicOnly(m.getPubKey()));
                newChannel.setKeyClientA(ECKey.fromPublicOnly(m.getPubKeyFE()));

                newChannel.setAnchorSecretHashClient(m.getSecretHashFE());
                newChannel.setAmountClient(m.getServerAmount());
                newChannel.setInitialAmountClient(m.getServerAmount());

                newChannel.setAnchorTxHashClient(Sha256Hash.wrap(m.getAnchorHash()));

                sendEstablishMessageC(ctx);
            } else if (message.type == Type.ESTABLISH_CHANNEL_C) {
                EstablishChannelMessageC m = new Gson().fromJson(message.data, EstablishChannelMessageC.class);

                newChannel.setAnchorTxHashClient(Sha256Hash.wrap(m.getAnchorHash()));
                newChannel.setEscapeTxSig(TransactionSignature.decodeFromBitcoin(m.getSignatureE(), true));
                newChannel.setFastEscapeTxSig(TransactionSignature.decodeFromBitcoin(m.getSignatureFE(), true));

                if (!newChannel.verifyEscapeSignatures()) {
                    throw new Exception("Signature does not match..");
                }

                Transaction escape = newChannel.getEscapeTransactionServer();

                System.out.println(escape);
                System.out.println(Tools.bytesToHex(escape.bitcoinSerialize()));

                Transaction getFundsFromEscape = new Transaction(Constants.getNetwork());
                getFundsFromEscape.addInput(escape.getOutput(0));
                getFundsFromEscape.addOutput(Coin.valueOf(escape.getOutput(0).getValue().value - 1000), wallet.freshReceiveAddress());

                TransactionSignature signature = Tools.getSignature(getFundsFromEscape, 0, newChannel.getScriptEscapeOutputServer().getProgram(), newChannel
                        .getKeyServer());

                getFundsFromEscape.getInput(0).setScriptSig(ScriptTools.getEscapeInputTimeoutScript(newChannel.getAnchorSecretHashServer(), newChannel
                        .getKeyServer(), newChannel.getKeyClient(), Constants.ESCAPE_REVOCATION_TIME, signature.encodeToBitcoin()));

                getFundsFromEscape.getInput(0).getScriptSig().correctlySpends(getFundsFromEscape, 0, escape.getOutput(0).getScriptPubKey());

                System.out.println(getFundsFromEscape);
                System.out.println(Tools.bytesToHex(getFundsFromEscape.bitcoinSerialize()));

                sendEstablishMessageD(ctx);
            } else if (message.type == Type.ESTABLISH_CHANNEL_D) {
                EstablishChannelMessageD m = new Gson().fromJson(message.data, EstablishChannelMessageD.class);

                newChannel.setEscapeTxSig(TransactionSignature.decodeFromBitcoin(m.getSignatureE(), true));
                newChannel.setFastEscapeTxSig(TransactionSignature.decodeFromBitcoin(m.getSignatureFE(), true));

                if (!newChannel.verifyEscapeSignatures()) {
                    throw new Exception("Signature does not match..");
                }
            }

            //TODO: Do all the channel opening stuff here..
        } else {
            ctx.fireChannelRead(msg);
        }

    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void initialize () {
        //TODO: Add all the database stuff here to get a complete list of all channels related with this node
        //Also, this is the place to ask for a new channel in case we don't have one yet (or want another one..)

        initialized = true;
    }

    public void prepareNewChannel () {
        newChannel = new Channel();
        newChannel.setInitialAmountServer((long) (node.context.balance * 0.1));
        newChannel.setAmountServer((long) (node.context.balance * 0.1));

        newChannel.setInitialAmountClient((long) (node.context.balance * 0.1));
        newChannel.setAmountClient((long) (node.context.balance * 0.1));

        //TODO: Change base key selection method (maybe completely random?)
        newChannel.setKeyServer(ECKey.fromPrivate(Tools.hashSha(node.context.nodeKey.getPrivKeyBytes(), 2)));
        newChannel.setKeyServerA(ECKey.fromPrivate(Tools.hashSha(node.context.nodeKey.getPrivKeyBytes(), 4)));
        newChannel.setMasterPrivateKeyServer(Tools.hashSha(node.context.nodeKey.getPrivKeyBytes(), 6));
        byte[] secretFE = Tools.hashSecret(Tools.hashSha(node.context.nodeKey.getPrivKeyBytes(), 8));
        newChannel.setAnchorSecretServer(secretFE);
        newChannel.setAnchorSecretHashServer(Tools.hashSecret(secretFE));
        newChannel.setServerChainDepth(1000);
        newChannel.setServerChainChild(0);
        newChannel.setIsReady(false);

        status = 1;

    }

    public void sendEstablishMessageA (ChannelHandlerContext ctx) {
        prepareNewChannel();

        EstablishChannelMessageA message = new EstablishChannelMessageA(
                newChannel.getKeyServer().getPubKey(),
                newChannel.getKeyServerA().getPubKey(),
                newChannel.getAnchorSecretHashServer(),
                newChannel.getInitialAmountServer(),
                newChannel.getInitialAmountClient()
        );

        ctx.writeAndFlush(new Message(message, Type.ESTABLISH_CHANNEL_A));

        status = 1;
    }

    public void sendEstablishMessageB (ChannelHandlerContext ctx) {

        Transaction anchor = newChannel.getAnchorTransactionServer(wallet, lockedOutputs);

        System.out.println(anchor);
        System.out.println(Tools.bytesToHex(anchor.bitcoinSerialize()));

        EstablishChannelMessageB message = new EstablishChannelMessageB(
                newChannel.getKeyServer().getPubKey(),
                newChannel.getKeyServerA().getPubKey(),
                newChannel.getAnchorSecretHashServer(),
                newChannel.getInitialAmountServer(),
                anchor.getHash().getBytes());

        ctx.writeAndFlush(new Message(message, Type.ESTABLISH_CHANNEL_B));
        status = 2;
    }

    public void sendEstablishMessageC (ChannelHandlerContext ctx) {

        Transaction anchor = newChannel.getAnchorTransactionServer(wallet, lockedOutputs);

        Transaction escape = newChannel.getEscapeTransactionClient();
        Transaction fastEscape = newChannel.getFastEscapeTransactionClient();

        TransactionSignature escapeSig = Tools.getSignature(escape, 0, newChannel.getScriptAnchorOutputClient().getProgram(), newChannel.getKeyServerA());
        TransactionSignature fastEscapeSig = Tools.getSignature(fastEscape, 0, newChannel.getScriptAnchorOutputClient().getProgram(), newChannel
                .getKeyServerA());

        EstablishChannelMessageC message = new EstablishChannelMessageC(
                anchor.getHash().getBytes(),
                escapeSig.encodeToBitcoin(),
                fastEscapeSig.encodeToBitcoin());

        ctx.writeAndFlush(new Message(message, Type.ESTABLISH_CHANNEL_C));
        status = 3;
    }

    public void sendEstablishMessageD (ChannelHandlerContext ctx) {

        Transaction escape = newChannel.getEscapeTransactionClient();
        Transaction fastEscape = newChannel.getFastEscapeTransactionClient();

        TransactionSignature escapeSig = Tools.getSignature(escape, 0, newChannel.getScriptAnchorOutputClient().getProgram(), newChannel.getKeyServerA());
        TransactionSignature fastEscapeSig = Tools.getSignature(fastEscape, 0, newChannel.getScriptAnchorOutputClient().getProgram(), newChannel
                .getKeyServerA());

        EstablishChannelMessageD message = new EstablishChannelMessageD(
                escapeSig.encodeToBitcoin(),
                fastEscapeSig.encodeToBitcoin());

        ctx.writeAndFlush(new Message(message, Type.ESTABLISH_CHANNEL_D));
        status = 4;
    }

}
