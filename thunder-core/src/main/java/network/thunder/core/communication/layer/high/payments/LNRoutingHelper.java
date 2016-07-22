package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;

import java.util.List;

public interface LNRoutingHelper {
    List<ChannelStatusObject> getRoute (byte[] source, byte[] destination, long amount, float weightPrivacy, float weightCost, float weightLatency);
}
