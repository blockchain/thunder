package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by matsjerratsch on 01/12/2015.
 */
public interface NodeObserver {
    void update (List<ByteBuffer> objectList);

}
