package network.thunder.core.communication.layer.middle.broadcasting.sync.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;

import java.util.ArrayList;
import java.util.List;

public class SyncSendMessage implements Sync {
    public List<PubkeyIPObject> pubkeyIPList;
    public List<PubkeyChannelObject> pubkeyChannelList;
    public List<ChannelStatusObject> channelStatusList;

    public SyncSendMessage (List<P2PDataObject> dataObjects) {
        pubkeyIPList = new ArrayList<>();
        pubkeyChannelList = new ArrayList<>();
        channelStatusList = new ArrayList<>();

        for (P2PDataObject obj : dataObjects) {
            if (obj instanceof PubkeyIPObject) {
                pubkeyIPList.add((PubkeyIPObject) obj);

            } else if (obj instanceof PubkeyChannelObject) {
                pubkeyChannelList.add((PubkeyChannelObject) obj);

            } else if (obj instanceof ChannelStatusObject) {
                channelStatusList.add((ChannelStatusObject) obj);

            }
        }
    }

    public List<P2PDataObject> getDataList () {
        List<P2PDataObject> dataList = new ArrayList<>();
        dataList.addAll(pubkeyChannelList);
        dataList.addAll(pubkeyIPList);
        dataList.addAll(channelStatusList);
        return dataList;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(pubkeyIPList);
        Preconditions.checkNotNull(pubkeyChannelList);
        Preconditions.checkNotNull(channelStatusList);
    }

    @Override
    public String toString () {
        return "SyncSendMessage{" +
                "pubkeyIPList=" + pubkeyIPList.size() +
                ", pubkeyChannelList=" + pubkeyChannelList.size() +
                ", channelStatusList=" + channelStatusList.size() +
                '}';
    }
}
