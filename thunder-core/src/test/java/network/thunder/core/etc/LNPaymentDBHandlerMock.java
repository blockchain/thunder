package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.lightning.RevocationHash;
import network.thunder.core.mesh.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by matsjerratsch on 13/01/2016.
 */
public class LNPaymentDBHandlerMock extends DBHandlerMock {

    @Override
    public Channel getChannel (Node node) {
        Channel channel = new Channel();
        channel.channelStatus = new ChannelStatus();
        return channel;
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
