package network.thunder.core.communication.layer.high.channel.establish.messages;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

public class LNEstablishMessageFactoryImpl extends MesssageFactoryImpl implements LNEstablishMessageFactory {

    @Override
    public LNEstablishAMessage getEstablishMessageA (Channel channel) {
        LNEstablishAMessage message = new LNEstablishAMessage(
                channel.keyServer,
                channel.anchorTx,
                channel.channelStatus.revocationHashServer,
                channel.channelStatus.amountClient,
                channel.channelStatus.amountServer,
                0,  //TODO add some reasonable minConfirmations
                channel.channelStatus.addressServer,
                channel.channelStatus.feePerByte,
                channel.channelStatus.csvDelay);
        return message;
    }

    @Override
    public LNEstablishBMessage getEstablishMessageB (TransactionSignature channelSignature) {
        LNEstablishBMessage message = new LNEstablishBMessage(
                channelSignature
        );

        return message;
    }

    @Override
    public LNEstablishCMessage getEstablishMessageC (Transaction transaction) {
        LNEstablishCMessage message = new LNEstablishCMessage(
                transaction
        );
        return message;
    }

    @Override
    public LNEstablishDMessage getEstablishMessageD () {
        LNEstablishDMessage message = new LNEstablishDMessage();
        return message;
    }

}
