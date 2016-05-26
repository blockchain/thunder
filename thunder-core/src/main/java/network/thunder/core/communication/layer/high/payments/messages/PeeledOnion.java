package network.thunder.core.communication.layer.high.payments.messages;

import network.thunder.core.communication.NodeKey;
import org.bitcoinj.core.ECKey;

import java.util.Arrays;

public class PeeledOnion {
    public PeeledOnion (OnionObject onionObject, byte[] data) {
        this.onionObject = onionObject;
        parseMessage(data);
    }

    void parseMessage (byte[] data) {
        byte[] pubkeyOfNextHop = new byte[33];
        System.arraycopy(data, 0, pubkeyOfNextHop, 0, 33);

        byte[] emptyData = new byte[OnionObject.KEY_LENGTH];

        if (Arrays.equals(emptyData, pubkeyOfNextHop)) {
            isLastHop = true;
        } else {
            nextHop = new NodeKey(ECKey.fromPublicOnly(pubkeyOfNextHop));
        }
    }

    public boolean isLastHop;
    public byte[] payload;

    public NodeKey nextHop;
    public long amount;

    public OnionObject onionObject;
}
