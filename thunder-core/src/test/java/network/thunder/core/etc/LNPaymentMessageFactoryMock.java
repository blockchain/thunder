package network.thunder.core.etc;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.messages.*;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LNPaymentMessageFactoryMock extends MesssageFactoryImpl implements LNPaymentMessageFactory {
    Random random = new Random();

    @Override
    public LNPaymentAMessage getMessageA (Channel channel, ChannelUpdate statusTemp) {
        if (statusTemp == null) {
            statusTemp = new ChannelUpdate();
        }
        return new LNPaymentAMessage(statusTemp, getMockRevocationHash());
    }

    @Override
    public LNPaymentBMessage getMessageB (Channel channel) {
        return new LNPaymentBMessage(getMockRevocationHash());
    }

    @Override
    public LNPaymentCMessage getMessageC (Channel channel, List<TransactionSignature> channelSignatures, List<TransactionSignature> paymentSignatures) {
        List<byte[]> list = new ArrayList<>();
        list.add(getMockSig());
        list.add(getMockSig());
        return new LNPaymentCMessage(getMockSig(), getMockSig(), list);
    }

    @Override
    public LNPaymentDMessage getMessageD (Channel channel) {
        return new LNPaymentDMessage(new ArrayList<>());
    }

    private RevocationHash getMockRevocationHash () {
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        byte[] hash = Tools.hashSecret(secret);
        RevocationHash revocationHash = new RevocationHash(1, 1, secret, hash);
        return revocationHash;
    }

    private byte[] getMockSig () {
        byte[] sig = new byte[72];
        random.nextBytes(sig);
        return sig;
    }
}
