package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.impl.factories.MesssageFactoryImpl;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentAMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentBMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentCMessage;
import network.thunder.core.communication.objects.messages.impl.message.lnpayment.LNPaymentDMessage;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.lightning.RevocationHash;
import org.bitcoinj.core.Transaction;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by matsjerratsch on 14/01/2016.
 */
public class LNPaymentMessageFactoryMock extends MesssageFactoryImpl implements LNPaymentMessageFactory {
    Random random = new Random();

    @Override
    public LNPaymentAMessage getMessageA (Channel channel, ChannelStatus statusTemp) {
        ChannelStatus channelStatus = new ChannelStatus();
        return new LNPaymentAMessage(channelStatus, getMockRevocationHash());
    }

    @Override
    public LNPaymentBMessage getMessageB (Channel channel) {
        return new LNPaymentBMessage(getMockRevocationHash());
    }

    @Override
    public LNPaymentCMessage getMessageC (Channel channel, Transaction transaction) {
        return new LNPaymentCMessage(getMockSig(), getMockSig());
    }

    @Override
    public LNPaymentDMessage getMessageD (Channel channel) {
        return new LNPaymentDMessage(new ArrayList<>());
    }

    private RevocationHash getMockRevocationHash() {
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        byte[] hash = Tools.hashSecret(secret);
        RevocationHash revocationHash = new RevocationHash(1, 1, secret, hash);
        return revocationHash;
    }

    private byte[] getMockSig() {
        byte[] sig = new byte[72];
        random.nextBytes(sig);
        return sig;
    }
}
