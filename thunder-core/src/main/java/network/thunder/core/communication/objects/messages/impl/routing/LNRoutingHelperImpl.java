package network.thunder.core.communication.objects.messages.impl.routing;

import com.sun.javafx.geom.Edge;
import network.thunder.core.communication.objects.messages.impl.message.gossip.objects.ChannelStatusObject;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNRoutingHelper;
import network.thunder.core.database.DBHandler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by matsjerratsch on 12/02/2016.
 */
public class LNRoutingHelperImpl implements LNRoutingHelper {

    DBHandler dbHandler;

    List<ByteBuffer> nodes = new ArrayList<>();
    Map<ByteBuffer, List<ChannelStatusObject>> network = new HashMap<>();

    public HashMap<ByteBuffer, Double> latencyTo = new HashMap<>();
    public HashMap<ByteBuffer, Double> costTo = new HashMap<>();
    HashMap<ByteBuffer, Double> distancesTo = new HashMap<>();

    HashMap<ByteBuffer, Boolean> onQueue = new HashMap<>();
    ArrayList<ByteBuffer> queue = new ArrayList<>();

    HashMap<ByteBuffer, List<ChannelStatusObject>> routeList = new HashMap<>();

    public LNRoutingHelperImpl (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    int cost;

    Iterable<Edge> cycle;

    ByteBuffer source;

    private float weightPrivacy;
    private float weightLatency;
    private float weightCost;

    private long amount;

    @Override
    public List<byte[]> getRoute (byte[] source, byte[] destination, long amount, float weightPrivacy, float weightCost, float weightLatency) {

        List<ChannelStatusObject> channelList = dbHandler.getTopology();
        for (ChannelStatusObject channel : channelList) {

            ByteBuffer nodeA = ByteBuffer.wrap(channel.pubkeyA);
            ByteBuffer nodeB = ByteBuffer.wrap(channel.pubkeyB);

            if (!nodes.contains(nodeA)) {
                nodes.add(nodeA);
            }
            if (!nodes.contains(nodeB)) {
                nodes.add(nodeB);
            }

            List<ChannelStatusObject> connectedEdges;
            if (network.containsKey(nodeA)) {
                connectedEdges = network.get(nodeA);
                if (!connectedEdges.contains(channel)) {
                    connectedEdges.add(channel);
                }
            } else {
                connectedEdges = new ArrayList<>();
                connectedEdges.add(channel);
                network.put(nodeA, connectedEdges);
            }

            if (network.containsKey(nodeB)) {
                connectedEdges = network.get(nodeB);
                if (!connectedEdges.contains(channel)) {
                    connectedEdges.add(channel);
                }
            } else {
                connectedEdges = new ArrayList<>();
                connectedEdges.add(channel);
                network.put(nodeB, connectedEdges);
            }
        }
        this.source = ByteBuffer.wrap(source);
        execute();
        return pathTo(ByteBuffer.wrap(destination));
    }

    private void execute () {
        for (ByteBuffer node : nodes) {

            distancesTo.put(node, Double.MAX_VALUE);
            latencyTo.put(node, 0.0);
            costTo.put(node, 0.0);
            onQueue.put(node, Boolean.FALSE);
            routeList.put(node, new ArrayList<>());
        }

        distancesTo.put(source, (double) 0);

        onQueue.put(source, Boolean.TRUE);
        queue.add(source);

        while (queue.size() > 0 && !hasNegativeCycle()) {
            ByteBuffer v = queue.remove(0);
            onQueue.put(v, Boolean.FALSE);
            relax(v);
        }
    }

    private void relax (ByteBuffer node) {
        List<ChannelStatusObject> connectedNodes = network.get(node);
        for (ChannelStatusObject channel : connectedNodes) {
            ByteBuffer otherNode = getOtherNode(channel, node);

            //Only use a node once for each route (against negative cycling)..
            if (routeList.get(node).contains(channel)) {
                continue;
            }

            double balance = amount - costTo.get(node);

            double distanceT = distancesTo.get(otherNode);
//            double distanceC = distancesTo.get(node) + channel.getWeight(node, otherNode, (long) balance, weightPrivacy, weightLatency, weightCost);
            double distanceC = distancesTo.get(node) + channel.getWeight(otherNode.array(), weightPrivacy, weightLatency, weightCost);

            if (distanceT > distanceC) {
                distancesTo.put(otherNode, distanceC);
                costTo.put(otherNode, costTo.get(node) + channel.getFee(otherNode.array()));
                latencyTo.put(otherNode, latencyTo.get(node) + channel.latency);

                List<ChannelStatusObject> currentRoute = new ArrayList<>(routeList.get(node));
                currentRoute.add(channel);
                routeList.put(otherNode, currentRoute);

//				edgesTo.put(e, network.getEdge(node, e));
//				routeList.put(e, this.pathToAsNodeList(e));

                if (!onQueue.get(otherNode)) {
                    queue.add(otherNode);
                    onQueue.put(otherNode, Boolean.TRUE);
                }
            }
            //			if(cost++ % )
            //TODO: Cycle detection..

        }

    }

    public boolean hasNegativeCycle () {
        return cycle != null;
    }

    public Iterable<Edge> negativeCycle () {
        return cycle;
    }

    public boolean hasPathTo (ByteBuffer v) {

        return distancesTo.get(v) < Double.POSITIVE_INFINITY;
    }

    public double distTo (byte[] v) {
        if (hasNegativeCycle()) {
            throw new UnsupportedOperationException("Negative cost cycle exists");
        }
        return distancesTo.get(ByteBuffer.wrap(v));
    }

    public List<byte[]> pathTo (ByteBuffer v) {
        if (hasNegativeCycle()) {
            throw new UnsupportedOperationException("Negative cost cycle exists");
        }
        if (!hasPathTo(v)) {
            return null;
        }

//        for(ByteBuffer b : nodes) {
//            System.out.println(Tools.bytesToHex(b.array()) + routeList.get(b));
//        }

        List<byte[]> path = new ArrayList<>();
        path.add(source.array());
        ByteBuffer lastNode = source;
        List<ChannelStatusObject> channelStatusObjects = routeList.get(v);
        for (ChannelStatusObject channel : routeList.get(v)) {
            lastNode = ByteBuffer.wrap(channel.getOtherNode(lastNode.array()));
            path.add(lastNode.array());
        }

        return path;
    }

    public ByteBuffer getOtherNode (ChannelStatusObject channelStatusObject, ByteBuffer node) {
        if (ByteBuffer.wrap(channelStatusObject.pubkeyA).equals(node)) {
            return ByteBuffer.wrap(channelStatusObject.pubkeyB);
        } else {
            return ByteBuffer.wrap(channelStatusObject.pubkeyA);
        }
    }
}
