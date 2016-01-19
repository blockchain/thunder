package network.thunder.core.communication.objects.messages.impl.factories;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

/**
 * Created by matsjerratsch on 08/01/2016.
 */
public class LNPaymentMessageFactoryImpl extends MesssageFactoryImpl implements LNPaymentMessageFactory {

    public LNPaymentMessageFactoryImpl (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    DBHandler dbHandler;

    @Override
    public LNPaymentAMessage getMessageA (Channel channel, ChannelStatus statusTemp) {
        return new LNPaymentAMessage(statusTemp, dbHandler.createRevocationHash(channel));
    }

    @Override
    public LNPaymentBMessage getMessageB (Channel channel) {
        return new LNPaymentBMessage(dbHandler.createRevocationHash(channel));
    }

    @Override
    public LNPaymentCMessage getMessageC (Channel channel, Transaction channelTransaction) {

        //The channelTransaction is finished, we just need to produce the signatures..
        TransactionSignature signature1 = Tools.getSignature(channelTransaction, 0, channel.getScriptAnchorOutputClient().getProgram(), channel.getKeyServer());
        TransactionSignature signature2 = Tools.getSignature(channelTransaction, 1, channel.getScriptAnchorOutputServer().getProgram(), channel.getKeyServer());
        return new LNPaymentCMessage(signature1.encodeToBitcoin(), signature2.encodeToBitcoin());
    }

    @Override
    public LNPaymentDMessage getMessageD (Channel channel) {
        return new LNPaymentDMessage(dbHandler.getOldRevocationHashes(channel));
    }
}
