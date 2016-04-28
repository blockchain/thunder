package network.thunder.core.communication.layer.middle.broadcasting.gossip;

import java.nio.ByteBuffer;
import java.util.List;

public interface NodeObserver {
    void update (List<ByteBuffer> objectList);

}
