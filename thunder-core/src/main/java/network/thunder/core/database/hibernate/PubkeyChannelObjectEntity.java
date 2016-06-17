package network.thunder.core.database.hibernate;

import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Jean-Pierre Rupp on 08/06/16.
 */

@Entity(name = "PubkeyChannelObject")
public class PubkeyChannelObjectEntity {
    private Integer id;
    private byte[] pubkeyB;
    private byte[] pubkeyB1;
    private byte[] pubkeyA;
    private byte[] pubkeyA1;
    private byte[] txidAnchor;
    private byte[] signatureA;
    private byte[] signatureB;
    private int timestamp;

    public PubkeyChannelObjectEntity () {}

    public PubkeyChannelObjectEntity (PubkeyChannelObject pubKeyChannelObject) {
        pubkeyB = pubKeyChannelObject.pubkeyB;
        pubkeyB1 = pubKeyChannelObject.pubkeyB1;
        pubkeyA = pubKeyChannelObject.pubkeyA;
        pubkeyA1 = pubKeyChannelObject.pubkeyA1;
        txidAnchor = pubKeyChannelObject.txidAnchor;
        signatureA = pubKeyChannelObject.signatureA;
        signatureB = pubKeyChannelObject.signatureB;
        timestamp = pubKeyChannelObject.timestamp;
    }

    public PubkeyChannelObject toPubkeyChannelObject() {
        PubkeyChannelObject pubkeyChannelObject = new PubkeyChannelObject();
        pubkeyChannelObject.pubkeyB = pubkeyB;
        pubkeyChannelObject.pubkeyB1 = pubkeyB1;
        pubkeyChannelObject.pubkeyA = pubkeyA;
        pubkeyChannelObject.pubkeyA1 = pubkeyA1;
        pubkeyChannelObject.txidAnchor = txidAnchor;
        pubkeyChannelObject.signatureA = signatureA;
        pubkeyChannelObject.signatureB = signatureB;
        pubkeyChannelObject.timestamp = timestamp;
        return pubkeyChannelObject;
    }

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    public byte[] getPubkeyB () {
        return pubkeyB;
    }

    public void setPubkeyB (byte[] pubkeyB) {
        this.pubkeyB = pubkeyB;
    }

    public byte[] getPubkeyB1 () {
        return pubkeyB1;
    }

    public void setPubkeyB1 (byte[] pubkeyB1) {
        this.pubkeyB1 = pubkeyB1;
    }

    public byte[] getPubkeyA () {
        return pubkeyA;
    }

    public void setPubkeyA (byte[] pubkeyA) {
        this.pubkeyA = pubkeyA;
    }

    public byte[] getPubkeyA1 () {
        return pubkeyA1;
    }

    public void setPubkeyA1 (byte[] pubkeyA1) {
        this.pubkeyA1 = pubkeyA1;
    }

    public byte[] getTxidAnchor () {
        return txidAnchor;
    }

    public void setTxidAnchor (byte[] txidAnchor) {
        this.txidAnchor = txidAnchor;
    }

    public byte[] getSignatureA () {
        return signatureA;
    }

    public void setSignatureA (byte[] signatureA) {
        this.signatureA = signatureA;
    }

    public byte[] getSignatureB () {
        return signatureB;
    }

    public void setSignatureB (byte[] signatureB) {
        this.signatureB = signatureB;
    }

    public int getTimestamp () {
        return timestamp;
    }

    public void setTimestamp (int timestamp) {
        this.timestamp = timestamp;
    }
}
