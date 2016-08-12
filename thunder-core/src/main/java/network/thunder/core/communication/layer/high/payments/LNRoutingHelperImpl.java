package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.processor.exceptions.LNRoutingException;
import network.thunder.core.database.DBHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LNRoutingHelperImpl implements LNRoutingHelper {

    DBHandler dbHandler;

    List<NodeKey> nodes = new ArrayList<>();
    Map<NodeKey, List<ChannelStatusObject>> network = new HashMap<>();

    Map<NodeKey, Double> latencyTo = new HashMap<>();
    Map<NodeKey, Double> costTo = new HashMap<>();
    Map<NodeKey, Double> distancesTo = new HashMap<>();

    Map<NodeKey, Boolean> onQueue = new HashMap<>();
    List<NodeKey> queue = new ArrayList<>();

    Map<NodeKey, List<ChannelStatusObject>> routeList = new HashMap<>();

    public LNRoutingHelperImpl (DBHandler dbHandler) {
        this.dbHandler = dbHandler;
    }

    int cost;
    
    NodeKey source;

    private float weightPrivacy;
    private float weightLatency;
    private float weightCost;

    private long amount;

    @Override
    public List<ChannelStatusObject> getRoute (byte[] source, byte[] destination, long amount, float weightPrivacy, float weightCost, float weightLatency) {
        List<ChannelStatusObject> channelList = dbHandler.getTopology();
        for (ChannelStatusObject channel : channelList) {
            NodeKey nodeA = new NodeKey(channel.pubkeyA);
            NodeKey nodeB = new NodeKey(channel.pubkeyB);

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
        this.source = new NodeKey(source);
        execute();
        return pathTo(new NodeKey(destination));
    }



    private void execute () {
        for (NodeKey node : nodes) {

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
            NodeKey v = queue.remove(0);
            onQueue.put(v, Boolean.FALSE);
            relax(v);
        }
    }

    private void relax (NodeKey node) {
        List<ChannelStatusObject> connectedNodes = network.get(node);
        for (ChannelStatusObject channel : connectedNodes) {
            NodeKey otherNode = getOtherNode(channel, node);

            //Only use a node once for each route (against negative cycling)..
            if (routeList.get(node).contains(channel)) {
                continue;
            }

            double balance = amount - costTo.get(node);

            double distanceT = distancesTo.get(otherNode);
            double distanceC = distancesTo.get(node) + channel.getWeight(otherNode.getPubKey(), weightPrivacy, weightLatency, weightCost);

            if (distanceT > distanceC) {
                distancesTo.put(otherNode, distanceC);
                costTo.put(otherNode, costTo.get(node) + channel.getFee(otherNode.getPubKey()).fix); //TODO use percentage fee as well
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

    public boolean hasPathTo (NodeKey v) {
        if (!distancesTo.containsKey(v)) {
            throw new LNRoutingException("No route found..");
        }

        return distancesTo.get(v) < Double.POSITIVE_INFINITY;
    }

    public double distTo (byte[] v) {
        if (hasNegativeCycle()) {
            throw new UnsupportedOperationException("Negative cost cycle exists");
        }
        return distancesTo.get(new NodeKey(v));
    }

    public List<ChannelStatusObject> pathTo (NodeKey v) {
        if (hasNegativeCycle()) {
            throw new UnsupportedOperationException("Negative cost cycle exists");
        }
        if (!hasPathTo(v)) {
            return null;
        }

        List<byte[]> path = new ArrayList<>();
        path.add(source.getPubKey());
        return routeList.get(v);
    }

    public NodeKey getOtherNode (ChannelStatusObject channelStatusObject, NodeKey node) {
        if (new NodeKey(channelStatusObject.pubkeyA).equals(node)) {
            return new NodeKey(channelStatusObject.pubkeyB);
        } else {
            return new NodeKey(channelStatusObject.pubkeyA);
        }
    }
}
