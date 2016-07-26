package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.helper.callback.ChannelOpenListener;
import org.bitcoinj.core.Sha256Hash;

public interface ChannelOpener {
    void openChannel (Channel channel, ChannelOpenListener callback);
    void onAnchorConfirmed (Sha256Hash channelHash);

    public static class NullChannelOpener implements ChannelOpener {

        @Override
        public void openChannel (Channel channel, ChannelOpenListener callback) {

        }

        @Override
        public void onAnchorConfirmed (Sha256Hash channelHash) {

        }
    }
}
