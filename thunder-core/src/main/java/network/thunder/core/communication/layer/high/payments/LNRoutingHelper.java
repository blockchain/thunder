package network.thunder.core.communication.layer.high.payments;

import java.util.List;

public interface LNRoutingHelper {
    List<byte[]> getRoute (byte[] source, byte[] destination, long amount, float weightPrivacy, float weightCost, float weightLatency);
}
