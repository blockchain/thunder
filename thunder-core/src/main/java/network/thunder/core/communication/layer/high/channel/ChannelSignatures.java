package network.thunder.core.communication.layer.high.channel;

import com.google.gson.Gson;
import org.bitcoinj.crypto.TransactionSignature;

import java.util.ArrayList;
import java.util.List;

public class ChannelSignatures {
    private final static Gson GSON = new Gson();
    public List<TransactionSignature> channelSignatures = new ArrayList<>();
    public List<TransactionSignature> paymentSignatures = new ArrayList<>();

    public ChannelSignatures (List<TransactionSignature> channelSignatures, List<TransactionSignature> paymentSignatures) {
        this.channelSignatures = channelSignatures;
        this.paymentSignatures = paymentSignatures;
    }

    public ChannelSignatures () {
    }

    public ChannelSignatures copy () {
        ChannelSignatures c = new ChannelSignatures();
        c.channelSignatures = new ArrayList<>(this.channelSignatures);
        c.paymentSignatures = new ArrayList<>(this.paymentSignatures);
        return c;
    }

    public String serialize () {
        return GSON.toJson(this);
    }

    public static ChannelSignatures deserialise (String json) {
        return GSON.fromJson(json, ChannelSignatures.class);
    }
}
