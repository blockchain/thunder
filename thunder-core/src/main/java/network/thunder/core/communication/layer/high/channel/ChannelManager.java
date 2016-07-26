package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.helper.callback.ChannelOpenListener;
import network.thunder.core.helper.callback.ResultCommand;
import org.bitcoinj.core.Sha256Hash;

import java.util.concurrent.locks.Lock;

public interface ChannelManager {

    void setup ();

    Lock getChannelLock (Sha256Hash channelHash);

    void openChannel (NodeKey node, ChannelOpenListener channelOpenListener);
    void closeChannel (Channel channel, ResultCommand callback);

    void addChannelOpener (NodeKey node, ChannelOpener channelOpener);
    void removeChannelOpener (NodeKey node);
    void addChannelCloser (NodeKey node, ChannelCloser channelCloser);
    void removeChannelCloser (NodeKey node);
}
