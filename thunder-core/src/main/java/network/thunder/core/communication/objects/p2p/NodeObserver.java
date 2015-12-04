package network.thunder.core.communication.objects.p2p;

import java.util.List;

/**
 * Created by matsjerratsch on 01/12/2015.
 */
public interface NodeObserver {
    void update ();

    void update (List<P2PDataObject> objectList);

}
