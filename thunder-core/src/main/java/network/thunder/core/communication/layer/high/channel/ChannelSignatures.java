package network.thunder.core.communication.layer.high.channel;

import network.thunder.core.communication.layer.high.Channel;
import org.bitcoinj.crypto.TransactionSignature;

import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

public class ChannelSignatures {
    @OneToMany(targetEntity = Channel.class, mappedBy = "hash", fetch = FetchType.EAGER)
    public List<TransactionSignature> channelSignatures = new ArrayList<>();

    @OneToMany(targetEntity = Channel.class, mappedBy = "hash", fetch = FetchType.EAGER)
    public List<TransactionSignature> paymentSignatures = new ArrayList<>();

    public ChannelSignatures (List<TransactionSignature> channelSignatures, List<TransactionSignature> paymentSignatures) {
        this.channelSignatures = channelSignatures;
        this.paymentSignatures = paymentSignatures;
    }

    public ChannelSignatures () {
    }
}
