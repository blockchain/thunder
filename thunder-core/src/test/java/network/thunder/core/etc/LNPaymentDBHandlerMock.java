package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.subobjects.PaymentSecret;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.lightning.RevocationHash;
import network.thunder.core.mesh.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by matsjerratsch on 13/01/2016.
 */
public class LNPaymentDBHandlerMock extends DBHandlerMock {
    public static final long INITIAL_AMOUNT_CHANNEL = 10000000;
    List<PaymentWrapper> payments = new ArrayList<>();
    List<PaymentSecret> secrets = new ArrayList<>();

    @Override
    public Channel getChannel (Node node) {
        Channel channel = new Channel();
        channel.channelStatus = new ChannelStatus();
        channel.amountServer = INITIAL_AMOUNT_CHANNEL;
        channel.amountClient = INITIAL_AMOUNT_CHANNEL;
        channel.channelStatus.amountServer = INITIAL_AMOUNT_CHANNEL;
        channel.channelStatus.amountClient = INITIAL_AMOUNT_CHANNEL;
        return channel;
    }

    @Override
    public void addPayment (PaymentWrapper paymentWrapper) {
        if (payments.contains(paymentWrapper)) {
            throw new RuntimeException("Double payment added?");
        }
        payments.add(paymentWrapper);
        System.out.println(payments.size());
    }

    @Override
    public void updatePayment (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.receiver = paymentWrapper.receiver;
                p.sender = paymentWrapper.sender;
                p.statusReceiver = paymentWrapper.statusReceiver;
                p.statusSender = paymentWrapper.statusSender;
            }
        }
    }

    @Override
    public void updatePaymentSender (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.statusSender = paymentWrapper.statusSender;
            }
        }
    }

    @Override
    public void updatePaymentReceiver (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.statusReceiver = paymentWrapper.statusReceiver;
            }
        }
    }

    @Override
    public void updatePaymentAddReceiverAddress (PaymentSecret secret, byte[] receiver) {
        for (PaymentWrapper p : payments) {
            if (p.paymentData.secret.equals(secret)) {
                p.receiver = receiver;
            }
        }
    }

    @Override
    public PaymentWrapper getPayment (PaymentSecret paymentSecret) {
        System.out.println(payments.size());

        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment;
            }
        }
        return null;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {
        if (secrets.contains(secret)) {
            PaymentSecret oldSecret = secrets.get(secrets.indexOf(secret));
            oldSecret.secret = secret.secret;
        } else {
            secrets.add(secret);
        }
    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        if (!secrets.contains(secret)) {
            return null;
        }
        return secrets.get(secrets.indexOf(secret));
    }

    @Override
    public byte[] getSenderOfPayment (PaymentSecret paymentSecret) {
        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment.sender;
            }
        }
        return null;
    }

    @Override
    public byte[] getReceiverOfPayment (PaymentSecret paymentSecret) {
        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment.receiver;
            }
        }
        return null;
    }

    @Override
    public RevocationHash createRevocationHash (Channel channel) {
        byte[] secret = new byte[20];
        new Random().nextBytes(secret);
        byte[] secretHash = Tools.hashSecret(secret);
        RevocationHash hash = new RevocationHash(1, 1, secret, secretHash);
        return hash;
    }

    @Override
    public List<RevocationHash> getOldRevocationHashes (Channel channel) {
        return new ArrayList<>();
    }
}
