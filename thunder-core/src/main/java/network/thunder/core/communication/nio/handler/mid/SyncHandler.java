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
import network.thunder.core.mesh.Node;
import network.thunder.core.communication.Type;
import network.thunder.core.communication.nio.P2PContext;
import network.thunder.core.communication.objects.p2p.DataObject;
import network.thunder.core.communication.objects.p2p.GetDataObject;
import network.thunder.core.communication.objects.p2p.PubkeyChannelObject;
import network.thunder.core.communication.objects.p2p.PubkeyIPObject;
import network.thunder.core.database.DatabaseHandler;

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

        if (!isServer) {
            if (node.justFetchNewIpAddresses) {
                sendGetIPs(ctx);
            } else if (node.justDownloadSyncData) {
                sendGetSyncData(ctx, context.syncDatastructure.getNextFragmentIndexToSynchronize());
            }
        }

    }

    public void sendIPaddresses (ChannelHandlerContext ctx) {
        System.out.println("IPs");
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
        ctx.writeAndFlush(new Message(new GetDataObject(index), Type.SYNC_GET_FRAGMENT));
    }

    public void sendSyncData (ChannelHandlerContext ctx, ArrayList<DataObject> dataList) {
        ctx.writeAndFlush(new Message(dataList, Type.SYNC_SEND_FRAGMENT));
    }

    public void sendData (ChannelHandlerContext ctx, ArrayList<DataObject> dataList) {
        try {
            ctx.writeAndFlush(new Message(dataList, Type.GOSSIP_SEND)).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendFailure (ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new Message(null, Type.FAILURE));
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {

            //Check authentication first before doing anything else with the messages.
            Message message = (Message) msg;

//			System.out.println(message.type);

            if (message.type >= 1300 && message.type <= 1399) {

                if (message.type == Type.SYNC_GET_IPS) {
                    sendIPaddresses(ctx);
                }

                if (message.type == Type.SYNC_SEND_IPS) {
                    //DataObject can hold lots of data. We only want to have IPs here
                    DataObject[] dataList = new Gson().fromJson(message.data, DataObject[].class);
                    for (DataObject o : dataList) {
                        if (o.type == DataObject.TYPE_IP_PUBKEY) {
                            PubkeyIPObject ipObject = o.getPubkeyIPObject();
                            ipObject.verify();
                            context.newIPList(ipObject);
                        } else {
                            throw new RuntimeException("Wrong Datatype when expecting IPs");
                        }
                    }
                    if (node.justFetchNewIpAddresses) {
                        ctx.close();
                    }
                }

                if (message.type == Type.SYNC_GET_FRAGMENT) {
                    GetDataObject object = new Gson().fromJson(message.data, GetDataObject.class);
                    ArrayList<PubkeyChannelObject> tempList = DatabaseHandler.getPubkeyChannelObjectsByFragmentIndex(conn, object.index);
                    ArrayList<DataObject> dataList = new ArrayList<>();
                    for (PubkeyChannelObject o : tempList) {
                        dataList.add(new DataObject(o));
                    }
                    sendSyncData(ctx, dataList);
                }

                if (message.type == Type.SYNC_SEND_FRAGMENT) {
                    DataObject[] dataList = new Gson().fromJson(message.data, DataObject[].class);
                    System.out.println(dataList.length);

                    int nextIndex = context.syncDatastructure.getNextFragmentIndexToSynchronize();
                    if (nextIndex > 0) {
                        sendGetSyncData(ctx, nextIndex);
                    } else {
                        ctx.close();
                    }
                }

            } else if (message.type == 0) {
                System.out.println("Got Failure:");
                System.out.println(message);
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
