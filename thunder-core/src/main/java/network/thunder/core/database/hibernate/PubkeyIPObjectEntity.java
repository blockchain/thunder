package network.thunder.core.database.hibernate;

import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Jean-Pierre Rupp on 08/06/16.
 */

@Entity(name = "PubkeyIPObject")
public class PubkeyIPObjectEntity {
    private Integer id;
    private String hostname;
    private int port;
    private byte[] pubkey;
    private byte[] signature;
    private int timestamp;

    public PubkeyIPObjectEntity () {}

    public PubkeyIPObjectEntity (PubkeyIPObject pubkeyIPObject) {
        hostname = pubkeyIPObject.hostname;
        port = pubkeyIPObject.port;
        pubkey = pubkeyIPObject.pubkey;
        signature = pubkeyIPObject.signature;
        timestamp = pubkeyIPObject.timestamp;
    }

    public PubkeyIPObject toPubkeyIPObject() {
        PubkeyIPObject pubkeyIPObject = new PubkeyIPObject();
        pubkeyIPObject.hostname = hostname;
        pubkeyIPObject.port = port;
        pubkeyIPObject.pubkey = pubkey;
        pubkeyIPObject.signature = signature;
        pubkeyIPObject.timestamp = timestamp;
        return pubkeyIPObject;
    }

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    public String getHostname () {
        return hostname;
    }

    public void setHostname (String hostname) {
        this.hostname = hostname;
    }

    public int getPort () {
        return port;
    }

    public void setPort (int port) {
        this.port = port;
    }

    public byte[] getPubkey () {
        return pubkey;
    }

    public void setPubkey (byte[] pubkey) {
        this.pubkey = pubkey;
    }

    public byte[] getSignature () {
        return signature;
    }

    public void setSignature (byte[] signature) {
        this.signature = signature;
    }

    public int getTimestamp () {
        return timestamp;
    }

    public void setTimestamp (int timestamp) {
        this.timestamp = timestamp;
    }
}
