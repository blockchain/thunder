package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.processor.exceptions.LNRoutingException;
import network.thunder.core.database.DBHandler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LNRoutingHelperImpl implements LNRoutingHelper {

    DBHandler dbHandler;

    List<ByteBuffer> nodes = new ArrayList<>();
    Map<ByteBuffer, List<ChannelStatusObject>> network = new HashMap<>();

    Map<ByteBuffer, Double> latencyTo = new HashMap<>();
    Map<ByteBuffer, Double> costTo = new HashMap<>();
    Map<ByteBuffer, Double> distancesTo = new HashMap<>();

    Map<ByteBuffer, Boolean> onQueue = new HashMap<>();
    List<ByteBuffer> queue = new ArrayList<>();

    Map<ByteBuffer, List<ChannelStatusObject>> routeList = new HashMap<>();

    public LNRoutingHelperImpl (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    int cost;
    
    ByteBuffer source;

    private float weightPrivacy;
    private float weightLatency;
    private float weightCost;

    private long amount;

    @Override
    public List<ChannelStatusObject> getRoute (byte[] source, byte[] destination, long amount, float weightPrivacy, float weightCost, float weightLatency) {

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
            double distanceC = distancesTo.get(node) + channel.getWeight(otherNode.array(), weightPrivacy, weightLatency, weightCost);

            if (distanceT > distanceC) {
                distancesTo.put(otherNode, distanceC);
                costTo.put(otherNode, costTo.get(node) + channel.getFee(otherNode.array()).fix); //TODO use percentage fee as well
                latencyTo.put(otherNode, latencyTo.get(node) + channel.latency);

                List<ChannelStatusObject> currentRoute = new ArrayList<>(routeList.get(node));
                currentRoute.add(channel);
                routeList.put(otherNode, currentRoute);
                if (!onQueue.get(otherNode)) {
                    queue.add(otherNode);
                    onQueue.put(otherNode, Boolean.TRUE);
                }
            }
            //TODO: Cycle detection..

        }

    }

    public boolean hasNegativeCycle () {
        return false; //TODO negative cycle detection not yet implemented
    }

    public boolean hasPathTo (ByteBuffer v) {
        if (!distancesTo.containsKey(v)) {
            throw new LNRoutingException("No route found..");
        }

        return distancesTo.get(v) < Double.POSITIVE_INFINITY;
    }

    public double distTo (byte[] v) {
        if (hasNegativeCycle()) {
            throw new UnsupportedOperationException("Negative cost cycle exists");
        }
        return distancesTo.get(ByteBuffer.wrap(v));
    }

    public List<ChannelStatusObject> pathTo (ByteBuffer v) {
        if (hasNegativeCycle()) {
            throw new UnsupportedOperationException("Negative cost cycle exists");
        }
        if (!hasPathTo(v)) {
            return null;
        }

        List<byte[]> path = new ArrayList<>();
        path.add(source.array());
        return routeList.get(v);
    }

    public ByteBuffer getOtherNode (ChannelStatusObject channelStatusObject, ByteBuffer node) {
        if (ByteBuffer.wrap(channelStatusObject.pubkeyA).equals(node)) {
            return ByteBuffer.wrap(channelStatusObject.pubkeyB);
        } else {
            return ByteBuffer.wrap(channelStatusObject.pubkeyA);
        }
    }
}
