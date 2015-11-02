/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package network.thunder.core.communication.nio.handler.mid;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.thunder.core.communication.Message;
import network.thunder.core.communication.Type;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.objects.p2p.DataObject;
import network.thunder.core.communication.objects.p2p.GetP2PDataObject;
import network.thunder.core.communication.objects.p2p.P2PDataObject;
import network.thunder.core.communication.objects.p2p.gossip.SendDataObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyIPObject;
import network.thunder.core.database.DatabaseHandler;
import network.thunder.core.mesh.Node;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/*
 * Handling the data transfer to new nodes.
 *
 * New nodes will first connect to a random node and just ask for more IP addresses.
 * As soon as they got new IP addresses, they will close the first connection and connect to
 * some of the new ones to ask them for fragments of the PubkeyChannelMap.
 */
public class SyncHandler extends ChannelInboundHandlerAdapter {

    private Node node;
    private boolean isServer = false;

    private P2PContext context;

    private Connection conn;

    private int lastIndex = 0;

    public SyncHandler (boolean isServer, Node node, P2PContext context) {
        this.isServer = isServer;
        this.node = node;
        this.context = context;
        try {
            this.conn = context.dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) {
        System.out.println("CHANNEL ACTIVE SYNC");
        if (node.getNettyContext() == null) {
            node.setNettyContext(ctx);
        }

        if (!isServer) {
            if (node.justFetchNewIpAddresses) {
                sendGetIPs(ctx);
            } else if (context.needsInitialSyncing) {
                sendGetSyncData(ctx, context.syncDatastructure.getNextFragmentIndexToSynchronize());

            }
        }

    }

    @Override
    public void channelUnregistered (ChannelHandlerContext ctx) {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ctx.fireChannelUnregistered();
    }

    public void sendIP (ChannelHandlerContext ctx) {
        ArrayList<DataObject> dataList = new ArrayList<>();
        for (PubkeyIPObject o : context.getIPList()) {
            dataList.add(new DataObject(o));
        }
        ctx.writeAndFlush(new Message(dataList, Type.SYNC_SEND_IPS));
    }

    public void sendGetIPs (ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new Message(null, Type.SYNC_GET_IPS));
    }

    public void sendGetSyncData (ChannelHandlerContext ctx, int index) {
        lastIndex = index;
        ctx.writeAndFlush(new Message(new GetP2PDataObject(index), Type.SYNC_GET_FRAGMENT));
    }

    public void sendSyncData (ChannelHandlerContext ctx, ArrayList<DataObject> dataList) {
        SendDataObject sendDataObject = new SendDataObject();
        sendDataObject.dataObjects = dataList;
        ctx.writeAndFlush(new Message(sendDataObject, Type.SYNC_SEND_FRAGMENT));
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {

            Message message = (Message) msg;

            if (message.type >= 1300 && message.type <= 1399) {

                if (message.type == Type.SYNC_GET_IPS) {
                    sendIP(ctx);
                }

                if (message.type == Type.SYNC_SEND_IPS) {
                    DataObject[] dataList = new Gson().fromJson(message.data, DataObject[].class);
                    for (DataObject o : dataList) {
                        if (o.type == DataObject.TYPE_IP_PUBKEY) {
                            PubkeyIPObject p2PDataObject = o.getPubkeyIPObject();
                            context.newIP(p2PDataObject);
                        } else {
                            throw new RuntimeException("Wrong Datatype when expecting IPs");
                        }
                    }
                    if (node.justFetchNewIpAddresses) {
                        ctx.close();
                    }
                }

                if (message.type == Type.SYNC_GET_FRAGMENT) {
                    //We got a GET request for a specific fragment.
                    GetP2PDataObject object = new Gson().fromJson(message.data, GetP2PDataObject.class);
                    ArrayList<DataObject> dataList = DatabaseHandler.getSyncDataByFragmentIndex(conn, object.index);
                    sendSyncData(ctx, dataList);
                }

                if (message.type == Type.SYNC_SEND_FRAGMENT) {
                    //Other node sent us all data with a specific fragment index.
                    SendDataObject dataList = new Gson().fromJson(message.data, SendDataObject.class);

                    ArrayList<P2PDataObject> list = new ArrayList<>();
                    for (DataObject obj : dataList.dataObjects) {
                        list.add(obj.getObject());
                    }

                    DatabaseHandler.syncDatalist(node.conn, list);
                    context.syncDatastructure.newFragment(lastIndex, dataList.dataObjects);

                    //Add the inventory to all other nodes
                    for (Node node : context.connectedNodes) {
                        if (!node.equals(this.node)) {
                            node.newInventoryList(dataList.dataObjects);
                        }
                    }
                    int nextIndex = context.syncDatastructure.getNextFragmentIndexToSynchronize();
                    if(nextIndex > 0) {
                        this.sendGetSyncData(ctx, nextIndex);
                    }
                    //TODO: A bit messy with DataObject and P2PDataObject here..
                }

            } else {
                //Pass it further to the next handler
                ctx.fireChannelRead(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
