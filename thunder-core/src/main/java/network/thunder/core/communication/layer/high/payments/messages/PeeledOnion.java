package network.thunder.core.communication.layer.high.payments.messages;

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
            System.out.println("We are the last hop..");
            isLastHop = true;
        } else {
            nextHop = ECKey.fromPublicOnly(pubkeyOfNextHop);
        }
    }

    public boolean isLastHop;
    public byte[] payload;

    public ECKey nextHop;
    public long amount;

    public OnionObject onionObject;
}
