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
import network.thunder.core.communication.objects.p2p.gossip.GetGossipDataObject;
import network.thunder.core.communication.objects.p2p.gossip.InvObject;
import network.thunder.core.communication.objects.p2p.gossip.SendGossipDataObject;
import network.thunder.core.communication.objects.p2p.sync.PubkeyIPObject;
import network.thunder.core.database.DatabaseHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.mesh.Node;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.util.ArrayList;

/* This layer is for coordinating gossip messages we received.
 * Messages are sent using the Node.class.
 */
public class GossipHandler extends ChannelInboundHandlerAdapter {

    private Node node;
    private boolean isServer = false;

    private P2PContext context;

    public GossipHandler (boolean isServer, Node node, P2PContext context) {
        this.isServer = isServer;
        this.node = node;
        this.context = context;
    }

    @Override
    public void channelActive (final ChannelHandlerContext ctx) throws SQLException {
        System.out.println("CHANNEL ACTIVE GOSSIP");
        if (node.getNettyContext() == null) {
            node.setNettyContext(ctx);
        }

        node.conn = context.dataSource.getConnection();

        try {
            if (!isServer) {
                //The newly connected node will broadcast it's existence to all other nodes once the connection has been established.
                sendOwnIPAddress(ctx);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendOwnIPAddress (ChannelHandlerContext ctx) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException {
        PubkeyIPObject pubkeyIPObject = new PubkeyIPObject();
        pubkeyIPObject.pubkey = this.context.nodeKey.getPubKey();
        pubkeyIPObject.port = this.context.port;
        pubkeyIPObject.IP = "127.0.0.1"; //TODO...
        pubkeyIPObject.timestamp = Tools.currentTime();
        pubkeyIPObject.sign(context.nodeKey);

        sendIPAddress(ctx, pubkeyIPObject);
    }

    public void sendIPAddress (ChannelHandlerContext ctx, PubkeyIPObject pubkeyIPObject) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchProviderException {
        ctx.writeAndFlush(new Message(pubkeyIPObject, Type.GOSSIP_SEND_IP_OBJECT));
    }

    public void sendGetData (ChannelHandlerContext ctx, ArrayList<byte[]> inventory) {
        GetGossipDataObject object = new GetGossipDataObject();
        object.inventoryList = inventory;
        ctx.writeAndFlush(new Message(object, Type.GOSSIP_GET));
    }

    public void sendData (ChannelHandlerContext ctx, ArrayList<DataObject> dataList) {
        SendGossipDataObject object = new SendGossipDataObject();
        object.dataObjects = dataList;
        ctx.writeAndFlush(new Message(object, Type.GOSSIP_SEND));
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) throws Exception {
        try {

            //Check authentication first before doing anything else with the messages.
            Message message = (Message) msg;

//			System.out.println(message.type);

            if (message.type >= 1200 && message.type <= 1299) {

                if (message.type == Type.GOSSIP_SEND_IP_OBJECT) {
                    //Other node sent us a new IP object. We check whether we know it already and if we don't, we send it to all other nodes...
                    PubkeyIPObject pubkeyIPObject = new Gson().fromJson(message.data, PubkeyIPObject.class);
                    pubkeyIPObject.verifySignature();

                    //Check if it is new to us
                    if (DatabaseHandler.newIPObject(node.conn, pubkeyIPObject)) {
                        //Object is new to us, broadcast to all other connected peers..
                        for (Node n : context.connectedNodes) {
                            sendIPAddress(n.getNettyContext(), pubkeyIPObject);
                        }
                    }
                }

                if (message.type == Type.GOSSIP_INV) {
                    //The other node is sending us his inventory of new data
                    InvObject inventory = new Gson().fromJson(message.data, InvObject.class);
                    ArrayList<byte[]> checkedInventory = DatabaseHandler.checkInv(node.conn, inventory.inventoryList);
                    if (checkedInventory.size() > 0) {
                        sendGetData(ctx, checkedInventory);
                    }
                }

                if (message.type == Type.GOSSIP_GET) {
                    //The other node asks for specific data
                    GetGossipDataObject getGossipDataObject = new Gson().fromJson(message.data, GetGossipDataObject.class);
                    ArrayList<DataObject> dataList = DatabaseHandler.getDataObjectByHash(node.conn, getGossipDataObject.inventoryList);
                    if (dataList.size() > 0) {
                        sendData(ctx, dataList);
                    }
                }

                if (message.type == Type.GOSSIP_SEND) {
                    //The other node sent us a list of new data
                    SendGossipDataObject sendGossipDataObject = new Gson().fromJson(message.data, SendGossipDataObject.class);
                    for (DataObject o : sendGossipDataObject.dataObjects) {
                        DatabaseHandler.newGossipData(node.conn, o);
                    }
                    for(Node n : context.connectedNodes) {
                        n.newInventoryList(sendGossipDataObject.dataObjects);
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
