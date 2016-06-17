package network.thunder.core.database.hibernate;

import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Jean-Pierre Rupp on 08/06/16.
 */

@Entity(name = "ChannelStatusObject")
public class ChannelStatusObjectEntity {
    private Integer id;
    private byte[] pubkeyA;
    private byte[] pubkeyB;
    private byte[] infoA;
    private byte[] infoB;
    private int latency;
    private int feeA;
    private int feeB;
    private byte[] signatureA;
    private byte[] signatureB;
    private int timestamp;

    public ChannelStatusObjectEntity () {}

    public ChannelStatusObjectEntity (ChannelStatusObject channelStatusObject) {
        pubkeyA = channelStatusObject.pubkeyA;
        pubkeyB = channelStatusObject.pubkeyB;
        infoA = channelStatusObject.infoA;
        infoB = channelStatusObject.infoB;
        latency = channelStatusObject.latency;
        feeA = channelStatusObject.feeA;
        feeB = channelStatusObject.feeB;
        signatureA = channelStatusObject.signatureA;
        signatureB = channelStatusObject.signatureB;
        timestamp = channelStatusObject.timestamp;
    }

    public ChannelStatusObject toChannelStatusObject() {
        ChannelStatusObject channelStatusObject = new ChannelStatusObject();
        channelStatusObject.pubkeyA = pubkeyA;
        channelStatusObject.pubkeyB = pubkeyB;
        channelStatusObject.infoA = infoA;
        channelStatusObject.infoB = infoB;
        channelStatusObject.latency = latency;
        channelStatusObject.feeA = feeA;
        channelStatusObject.feeB = feeB;
        channelStatusObject.signatureA = signatureA;
        channelStatusObject.signatureB = signatureB;
        channelStatusObject.timestamp = timestamp;
        return channelStatusObject;
    }

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    public byte[] getPubkeyA () {
        return pubkeyA;
    }

    public void setPubkeyA (byte[] pubkeyA) {
        this.pubkeyA = pubkeyA;
    }

    public byte[] getPubkeyB () {
        return pubkeyB;
    }

    public void setPubkeyB (byte[] pubkeyB) {
        this.pubkeyB = pubkeyB;
    }

    public byte[] getInfoA () {
        return infoA;
    }

    public void setInfoA (byte[] infoA) {
        this.infoA = infoA;
    }

    public byte[] getInfoB () {
        return infoB;
    }

    public void setInfoB (byte[] infoB) {
        this.infoB = infoB;
    }

    public int getLatency () {
        return latency;
    }

    public void setLatency (int latency) {
        this.latency = latency;
    }

    public int getFeeA () {
        return feeA;
    }

    public void setFeeA (int feeA) {
        this.feeA = feeA;
    }

    public int getFeeB () {
        return feeB;
    }

    public void setFeeB (int feeB) {
        this.feeB = feeB;
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
