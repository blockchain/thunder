package network.thunder.core.etc;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.Fee;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

public class TestTools {
    private static final Logger log = Tools.getLogger();

    public static TransactionSignature corruptSignature (TransactionSignature signature) {
        byte[] sig = signature.encodeToBitcoin();
        Tools.copyRandomByteInByteArray(sig, 40, 4);
        return TransactionSignature.decodeFromBitcoin(sig, true);
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
        log.debug("Exchanged {1} ", message);
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
        channel.amountClient = 1100000;
        channel.amountServer = 900000;

        channel.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;
        channel.feePerByte = configuration.DEFAULT_FEE_PER_BYTE;
        channel.masterPrivateKeyServer = Tools.getRandomByte(20);
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

    public static ChannelStatusObject getRandomObject () {
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
