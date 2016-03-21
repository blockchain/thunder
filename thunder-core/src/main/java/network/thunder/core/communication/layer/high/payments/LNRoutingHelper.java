package network.thunder.core.communication.layer.high.payments;

import java.util.List;

/**
 * Created by matsjerratsch on 12/02/2016.
 */
public interface LNRoutingHelper {
    List<byte[]> getRoute (byte[] source, byte[] destination, long amount, float weightPrivacy, float weightCost, float weightLatency);
}
