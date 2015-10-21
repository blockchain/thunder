package network.thunder.core.communication.objects.p2p;

import com.google.gson.Gson;

/**
 * Created by matsjerratsch on 19/10/2015.
 */
public class DataObject {

	public final static int TYPE_IP_PUBKEY = 1;
	public final static int TYPE_CHANNEL_PUBKEY = 2;
	public final static int TYPE_CHANNEL_STATUS = 3;

	public int type = 0;

	public String data = "";

	public PubkeyChannelObject getPubkeyChannelObject () {
		if (type == TYPE_CHANNEL_PUBKEY) {
			return new Gson().fromJson(data, PubkeyChannelObject.class);
		}
		throw new RuntimeException("Not correct object..");
	}

	public PubkeyIPObject getPubkeyIPObject () {
		if (type == TYPE_IP_PUBKEY) {
			return new Gson().fromJson(data, PubkeyIPObject.class);
		}
		throw new RuntimeException("Not correct object..");
	}

	public DataObject (Object object) {
		if (object instanceof PubkeyIPObject) {
			type = TYPE_IP_PUBKEY;
		} else if (object instanceof PubkeyChannelObject) {
			type = TYPE_CHANNEL_PUBKEY;
		} else {
			throw new RuntimeException("Object not supported currently");
		}
		data = new Gson().toJson(object);

	}

}
