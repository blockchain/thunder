package network.thunder.core.etc;

import network.thunder.core.communication.layer.MesssageFactoryImpl;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.communication.layer.high.payments.messages.*;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.ArrayList;
import java.util.Collections;
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
        return new LNPaymentCMessage(new ChannelSignatures(getMockSig(), getMockSig()));
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

    private List<TransactionSignature> getMockSig () {
        ECKey k = new ECKey();
        Transaction t = new Transaction(Constants.getNetwork());
        t.addInput(Sha256Hash.ZERO_HASH, 0, Tools.getDummyScript());
        t.addOutput(Coin.ZERO, Tools.getDummyScript());

        return Collections.singletonList(Tools.getSignature(t, 0, t.getOutput(0),k));
    }
}
