package network.thunder.core.etc;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class TestTools {
    public static TransactionSignature corruptSignature (TransactionSignature signature) {
        byte[] sig = signature.encodeToBitcoin();
        Tools.copyRandomByteInByteArray(sig, 40, 4);
        return TransactionSignature.decodeFromBitcoin(sig, true);
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
        System.out.println(message);
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to, Class expectedMessage) {
        Object message = from.readOutbound();
        assertThat(message, instanceOf(expectedMessage));
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessagesDuplex (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to);
        exchangeMessages(to, from);
    }

    public static Channel getMockChannel (LNConfiguration configuration) {
        Channel channel = new Channel();

        Transaction anchor = new Transaction(Constants.getNetwork());
        anchor.addInput(Sha256Hash.wrap(Tools.getRandomByte(32)), 0, Tools.getDummyScript());
        anchor.addInput(Sha256Hash.wrap(Tools.getRandomByte(32)), 1, Tools.getDummyScript());

        anchor.addOutput(Coin.valueOf(2000000), Tools.getDummyScript());

        channel.anchorTx = anchor;
        channel.anchorTxHash = anchor.getHash();
        channel.channelStatus.amountClient = 1100000;
        channel.channelStatus.amountServer = 900000;

        channel.channelStatus.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;
        channel.channelStatus.feePerByte = configuration.DEFAULT_FEE_PER_BYTE;
        channel.masterPrivateKeyServer = Tools.getRandomByte(20);
        channel.shaChainDepth = 1;
        channel.channelStatus.revoHashServerCurrent = new RevocationHash(1, channel.masterPrivateKeyServer);
        channel.channelStatus.revoHashClientCurrent = new RevocationHash(1, Tools.getRandomByte(20));

        channel.channelStatus.revoHashServerNext = new RevocationHash(2, channel.masterPrivateKeyServer);
        channel.channelStatus.revoHashClientNext = new RevocationHash(2, Tools.getRandomByte(20));

        channel.channelStatus.addressClient = new Address(Constants.getNetwork(), Tools.getRandomByte(20));
        channel.channelStatus.addressServer = new Address(Constants.getNetwork(), Tools.getRandomByte(20));

        return channel;
    }
}
