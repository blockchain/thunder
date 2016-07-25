package network.thunder.core.etc;

import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;

import java.util.ArrayList;
import java.util.List;

public class SeedNodes {
    public static List<PubkeyIPObject> ipList = new ArrayList<>();
    public static List<ClientObject> nodeList = new ArrayList<>();

    static {
        ipList.add(createIPObject("thunder-1.blockchain.info", "0361da8688bf21d605407c1843b4661e0802c6134a44883c496609e8163fd6df67"));
//        ipList.add(createIPObject("thunder-2.blockchain.info", "02ec5bc887f5317602525ebd8a4706acef18c1f1aadc9caeb81a1d82fc6d190c0e"));
//        ipList.add(createIPObject("thunder-3.blockchain.info", "02c6c23eb7a3212eae69726891dfd3845dc48f503b5f2ec2a2166f8613c6aeb134"));
//        ipList.add(createIPObject("thunder-4.blockchain.info", "03f8ffdae2b884baf0f0166afa510ef30766a0629e762f266f32953fb658dfe845"));
//        ipList.add(createIPObject("thunder-5.blockchain.info", "02a0b848a9cebe8f7caeceffbe0b5ad7734d7f8f59e5f7e3da6c4040774c333797"));
//        ipList.add(createIPObject("thunder-6.blockchain.info", "02678c66d4244a2df885aa7e7636c3ac105d9bd271d8aac2e13c6c292ffaf3493f"));
//        ipList.add(createIPObject("thunder-7.blockchain.info", "02a9a9cd8c420234f7945c558c13843b0e0a14cc1cd0314c6dcb073d564295540b"));
//        ipList.add(createIPObject("thunder-8.blockchain.info", "0274a0705e85ebce156da4d0658f76f8d08edc809561ae7046311f2f9704012486"));
//        ipList.add(createIPObject("thunder-9.blockchain.info", "03413a55bbfea121c7ddcd54cd9e52eed86d7b0252ef02dc0182dceef8c6c812e9"));
    }

    public static PubkeyIPObject createIPObject (String hostname, String pubkey, int port) {
        PubkeyIPObject seed1 = new PubkeyIPObject();
        seed1.hostname = hostname;
        seed1.pubkey = Tools.hexStringToByteArray(pubkey);
        seed1.signature = new byte[1];
        seed1.port = port;
        return seed1;
    }

    public static PubkeyIPObject createIPObject (String hostname, String pubkey) {
        return createIPObject(hostname, pubkey, Constants.STANDARD_PORT);
    }

    public static List<PubkeyIPObject> getSeedNodes () {
        return ipList;
    }

    public static void setToTestValues () {
        ClientObject node = new ClientObject();
        node.init();
        node.host = "localhost";
        node.port = Constants.STANDARD_PORT;

        PubkeyIPObject seed1 = new PubkeyIPObject();
        seed1.hostname = "localhost";
        seed1.port = Constants.STANDARD_PORT;
        seed1.pubkey = node.nodeKey.getPubKey();

        ipList.clear();

        nodeList.add(node);
        ipList.add(seed1);
    }
}
