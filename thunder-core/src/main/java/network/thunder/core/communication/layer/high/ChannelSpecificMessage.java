package network.thunder.core.communication.layer.high;

import org.bitcoinj.core.Sha256Hash;

public interface ChannelSpecificMessage {
    Sha256Hash getChannelHash();
    void setChannelHash(Sha256Hash hash);
}
