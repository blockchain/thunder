package network.thunder.core.mesh;

import com.sun.javafx.geom.Edge;
import network.thunder.core.communication.objects.p2p.P2PDataObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyChannelObject;

import java.util.ArrayList;

public class Network {

    ArrayList<Node> nodeList = new ArrayList<>();
    ArrayList<Edge> edgeList = new ArrayList<>();

    public void addNode (Node node) {
        if (nodeList.contains(node)) {
            return;
        }

        nodeList.add(node);
    }

    public void initialize (ArrayList<P2PDataObject> nodeList) {
        //First load up all the nodes in memory..
        for (P2PDataObject obj : nodeList) {
            if (obj instanceof PubkeyChannelObject) {
                PubkeyChannelObject pubkeyChannelObject = (PubkeyChannelObject) obj;
                addNode(new Node(pubkeyChannelObject.pubkeyA));
                addNode(new Node(pubkeyChannelObject.pubkeyB));
            }
        }

        //Now build the grid between the nodes
    }

}
