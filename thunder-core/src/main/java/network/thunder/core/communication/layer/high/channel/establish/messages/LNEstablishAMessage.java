package network.thunder.core.communication.layer.high.channel.establish.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.core.ECKey;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public class LNEstablishAMessage implements LNEstablish {
    public byte[] pubKeyEscape;
    public byte[] pubKeyFastEscape;
    public byte[] secretHashFastEscape;
    public byte[] revocationHash;
    public long clientAmount;
    public long serverAmount;

    public LNEstablishAMessage (byte[] pubKeyEscape, byte[] pubKeyFastEscape, byte[] secretHashFastEscape, byte[] revocationHash, long clientAmount, long
            serverAmount) {
        this.pubKeyEscape = pubKeyEscape;
        this.pubKeyFastEscape = pubKeyFastEscape;
        this.secretHashFastEscape = secretHashFastEscape;
        this.revocationHash = revocationHash;
        this.clientAmount = clientAmount;
        this.serverAmount = serverAmount;
    }

    @Override
    public void saveToChannel (Channel channel) {
        channel.setInitialAmountServer(this.clientAmount);
        channel.setAmountServer(this.clientAmount);
        channel.setInitialAmountClient(this.serverAmount);
        channel.setAmountClient(this.serverAmount);
        channel.setKeyClient(ECKey.fromPublicOnly(this.pubKeyEscape));
        channel.setKeyClientA(ECKey.fromPublicOnly(this.pubKeyFastEscape));
        channel.setAnchorSecretHashClient(this.secretHashFastEscape);
        channel.setAnchorRevocationHashClient(this.revocationHash);
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(pubKeyEscape);
        Preconditions.checkNotNull(pubKeyFastEscape);
        Preconditions.checkNotNull(secretHashFastEscape);
        Preconditions.checkNotNull(revocationHash);
    }

    @Override
    public String toString () {
        return "LNEstablishAMessage{" +
                "serverAmount=" + serverAmount +
                ", clientAmount=" + clientAmount +
                '}';
    }
}
