package network.thunder.core.etc;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.LNOnionHelper;
import network.thunder.core.communication.layer.high.payments.LNOnionHelperImpl;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.Fee;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.persistent.SQLDBHandler;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class TestTools {
    private static final Logger log = Tools.getLogger();

    public static PaymentData getMockPaymentData (ECKey key1, ECKey key2) {
        LNConfiguration configuration = new LNConfiguration();
        PaymentData paymentData = new PaymentData();
        paymentData.sending = true;
        paymentData.amount = 10000;
        paymentData.secret = new PaymentSecret(Tools.getRandomByte(20));
        paymentData.timestampOpen = Tools.currentTime();
        paymentData.timestampRefund = paymentData.timestampOpen + configuration.DEFAULT_REFUND_DELAY * 10;

        LNOnionHelper onionHelper = new LNOnionHelperImpl();
        List<byte[]> route = new ArrayList<>();
        route.add(key1.getPubKey());
        route.add(key2.getPubKey());

        paymentData.onionObject = onionHelper.createOnionObject(route, null);

        return paymentData;
    }

    public static DBHandler getTestDBHandler() {
        return new SQLDBHandler(Tools.getH2InMemoryDataSource());
    }

    public static TransactionSignature corruptSignature (TransactionSignature signature) {
        byte[] sig = signature.encodeToBitcoin();
        Tools.copyRandomByteInByteArray(sig, 40, 4);
        return TransactionSignature.decodeFromBitcoin(sig, true);
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
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

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to, Class expectedMessage, long timeout) throws InterruptedException {
        long timeNow = System.currentTimeMillis();
        while(true) {
            long timeDiff = System.currentTimeMillis() - timeNow;
            Object message = from.readOutbound();

            if(message != null || timeDiff > timeout) {
                assertThat(message, instanceOf(expectedMessage));
                if (message != null) {
                    to.writeInbound(message);
                }
                return;
            }
            Thread.sleep(10);
        }
    }

    public static void exchangeMessagesDuplex (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to);
        exchangeMessages(to, from);
    }

    public static Channel getMockChannel (LNConfiguration configuration) {
        Channel channel = new Channel();

        channel.keyClient = new ECKey();

        channel.nodeKeyClient = new NodeKey(new ECKey());

        Transaction anchor = new Transaction(Constants.getNetwork());
        anchor.addInput(Sha256Hash.wrap(Tools.getRandomByte(32)), 0, Tools.getDummyScript());
        anchor.addInput(Sha256Hash.wrap(Tools.getRandomByte(32)), 1, Tools.getDummyScript());

        anchor.addOutput(Coin.valueOf(2000000), Tools.getDummyScript());

        channel.anchorTx = anchor;
        channel.anchorTxHash = anchor.getHash();
        channel.amountClient = 1100000;
        channel.amountServer = 900000;

        channel.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;
        channel.feePerByte = configuration.DEFAULT_FEE_PER_BYTE;
        channel.masterPrivateKeyServer = Tools.getRandomByte(20);
        channel.masterPrivateKeyClient = Tools.getRandomByte(20);
        channel.shaChainDepthCurrent = 1;
        channel.revoHashServerCurrent = new RevocationHash(1, channel.masterPrivateKeyServer);
        channel.revoHashClientCurrent = new RevocationHash(1, Tools.getRandomByte(20));

        channel.revoHashServerNext = new RevocationHash(2, channel.masterPrivateKeyServer);
        channel.revoHashClientNext = new RevocationHash(2, Tools.getRandomByte(20));

        channel.revoHashServerNextNext = new RevocationHash(3, channel.masterPrivateKeyServer);
        channel.revoHashClientNextNext = new RevocationHash(3, Tools.getRandomByte(20));

        channel.addressClient = new Address(Constants.getNetwork(), Tools.getRandomByte(20));
        channel.addressServer = new Address(Constants.getNetwork(), Tools.getRandomByte(20));

        return channel;
    }

    public static PubkeyIPObject getRandomObjectIpObject () {
        PubkeyIPObject obj = new PubkeyIPObject();

        Random random = new Random();

        obj.hostname = random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255) + "." + random.nextInt(255);

        obj.pubkey = Tools.getRandomByte(33);
        obj.timestamp = Tools.currentTime();
        obj.port = 8992;

        obj.signature = Tools.getRandomByte(65);

        return obj;
    }

    public static ChannelStatusObject getRandomObjectStatusObject () {
        ChannelStatusObject obj = new ChannelStatusObject();

        obj.pubkeyA = Tools.getRandomByte(33);
        obj.pubkeyB = Tools.getRandomByte(33);

        obj.infoA = Tools.getRandomByte(60);
        obj.infoB = Tools.getRandomByte(60);

        obj.timestamp = Tools.currentTime();

        obj.signatureA = Tools.getRandomByte(65);
        obj.signatureB = Tools.getRandomByte(65);

        obj.feeA = new Fee(1, 1);
        obj.feeB = new Fee(1, 1);

        return obj;
    }

    public static PubkeyChannelObject getRandomObjectChannelObject () {
        PubkeyChannelObject obj = new PubkeyChannelObject();


        obj.nodeKeyB = Tools.getRandomByte(33);
        obj.channelKeyB = Tools.getRandomByte(33);

        obj.nodeKeyA = Tools.getRandomByte(33);
        obj.channelKeyA = Tools.getRandomByte(33);

        obj.txidAnchor = Tools.getRandomByte(32);

        obj.signatureA = Tools.getRandomByte(65);
        obj.signatureB = Tools.getRandomByte(65);

        obj.timestamp = Tools.currentTime();

        return obj;
    }

    @NotNull
    public static Set<Script.VerifyFlag> getVerifyFlags () {
        Set<Script.VerifyFlag> flags = new HashSet<>();
        flags.add(Script.VerifyFlag.CHECKLOCKTIMEVERIFY);
        flags.add(Script.VerifyFlag.CLEANSTACK);
        flags.add(Script.VerifyFlag.DERSIG);
        flags.add(Script.VerifyFlag.LOW_S);
        flags.add(Script.VerifyFlag.MINIMALDATA);
        flags.add(Script.VerifyFlag.NULLDUMMY);
        flags.add(Script.VerifyFlag.P2SH);
        flags.add(Script.VerifyFlag.SIGPUSHONLY);
        flags.add(Script.VerifyFlag.STRICTENC);
        return flags;
    }

    public static LNConfiguration getZeroFeeConfiguration () {
        LNConfiguration configuration = new LNConfiguration();
        configuration.DEFAULT_FEE_PER_BYTE = 0;
        configuration.DEFAULT_FEE_PER_BYTE_CLOSING = 0;
        configuration.MIN_FEE_PER_BYTE = 0;
        configuration.MIN_FEE_PER_BYTE_CLOSING = 0;
        configuration.MAX_FEE_PER_BYTE = 0;
        configuration.MAX_FEE_PER_BYTE_CLOSING = 0;
        return configuration;
    }

    public static List<ChannelStatusObject> translateECKeyToRoute (List<ECKey> keyList) {
        List<ChannelStatusObject> list = new ArrayList<>();
        List<ECKey> route = new ArrayList<>(keyList);
        ECKey keyOne = route.remove(0);

        for (ECKey key : route) {
            ChannelStatusObject object = new ChannelStatusObject();
            object.minTimeout = 30;
            object.feeA = new Fee(1, 100);
            object.feeB = new Fee(1, 100);
            object.latency = 1;
            object.pubkeyA = keyOne.getPubKey();
            object.pubkeyB = key.getPubKey();
            list.add(object);

            keyOne = key;
        }

        return list;
    }

}
