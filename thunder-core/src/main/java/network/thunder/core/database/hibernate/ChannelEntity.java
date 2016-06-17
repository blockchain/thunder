package network.thunder.core.database.hibernate;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.hibernate.Session;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jean-Pierre Rupp on 05/06/16.
 */

@Entity(name = "Channel")
public class ChannelEntity {
    private int id;
    private Sha256Hash hash;
    private NodeKey nodeKeyClient;
    private ECKey keyClient;
    private ECKey keyServer;
    private byte[] masterPrivateKeyClient;
    private byte[] masterPrivateKeyServer;
    private Integer shaChainDepthCurrent;
    private Integer timestampOpen;
    private Integer timestampForceClose;
    private Transaction anchorTx;
    private Sha256Hash anchorTxHash;
    private Integer minConfirmationAnchor;
    private Channel.Phase phase;
    private List<HibernateSignature> signatures = new ArrayList<>();

    // ChannelStatus object
    private long amountClient;
    private long amountServer;
    private List<PaymentDataEntity> paymentList = new ArrayList<>();
    private int feePerByte;
    private int csvDelay;
    private RevocationHashEmbedded revoHashClientCurrent;
    private RevocationHashEmbedded revoHashServerCurrent;
    private RevocationHashEmbedded revoHashClientNext;
    private RevocationHashEmbedded revoHashServerNext;
    private Address addressClient;
    private Address addressServer;

    public Channel toChannel () {
        List<TransactionSignature> channelSignatures = new ArrayList<>();
        List<TransactionSignature> paymentSignatures = new ArrayList<>();
        List<TransactionSignature> closingSignatures = new ArrayList<>();
        ChannelSignatures localChannelSignatures =
                new ChannelSignatures(channelSignatures, paymentSignatures);

        List<HibernateSignature> signatures = getSignatures();
        signatures.forEach(signature -> {
            switch (signature.getType()) {
                case CHANNEL:
                    channelSignatures.add(signature.getTransactionSignature());
                    break;
                case PAYMENT:
                    paymentSignatures.add(signature.getTransactionSignature());
                    break;
                case CLOSING:
                    paymentSignatures.add(signature.getTransactionSignature());
                    break;
                default:
                    throw new RuntimeException("Unrecognized signature type");
            }
        });

        Channel channel = new Channel();
        channel.id = id;
        channel.nodeKeyClient = nodeKeyClient;
        channel.keyClient = keyClient;
        channel.keyServer = keyServer;
        channel.masterPrivateKeyClient = masterPrivateKeyClient;
        channel.masterPrivateKeyServer = masterPrivateKeyServer;
        channel.shaChainDepthCurrent = shaChainDepthCurrent;
        channel.timestampOpen = timestampOpen;
        channel.timestampForceClose = timestampForceClose;
        channel.anchorTxHash = anchorTxHash;
        channel.anchorTx = anchorTx;
        channel.minConfirmationAnchor = minConfirmationAnchor;
        channel.channelStatus = toChannelStatus();
        channel.channelSignatures = localChannelSignatures;
        channel.phase = phase;
        channel.closingSignatures = closingSignatures;
        return channel;
    }

    public ChannelEntity () {
    }

    public ChannelEntity (Channel channel) {
        id = channel.id;
        hash = channel.getHash();
        nodeKeyClient = channel.nodeKeyClient;
        keyClient = channel.keyClient;
        keyServer = channel.keyServer;
        masterPrivateKeyClient = channel.masterPrivateKeyClient;
        masterPrivateKeyServer = channel.masterPrivateKeyServer;
        shaChainDepthCurrent = channel.shaChainDepthCurrent;
        timestampOpen = channel.timestampOpen;
        timestampForceClose = channel.timestampForceClose;
        anchorTx = channel.anchorTx;
        anchorTxHash = channel.anchorTxHash;
        minConfirmationAnchor = channel.minConfirmationAnchor;
        if (channel.channelStatus != null) {
            fromChannelStatus(channel.channelStatus);
        }
        phase = channel.phase;
    }

    public void saveChannelSignatures (Session session, Channel channel) {
        if (channel.closingSignatures != null) {
            channel.closingSignatures.forEach(signature -> {
                HibernateSignature closingSignature =
                        new HibernateSignature(signature, HibernateSignature.Type.CLOSING);
                addSignature(closingSignature);
            });
        }

        if (channel.channelSignatures != null) {
            if (channel.channelSignatures.paymentSignatures != null) {
                channel.channelSignatures.paymentSignatures.forEach(signature -> {
                    HibernateSignature paymentSignature = new HibernateSignature(signature, HibernateSignature.Type.CLOSING);
                    addSignature(paymentSignature);
                });
            }
            if (channel.channelSignatures.channelSignatures != null) {
                channel.channelSignatures.channelSignatures.forEach(signature -> {
                    HibernateSignature channelSignature = new HibernateSignature(signature, HibernateSignature.Type.CLOSING);
                    addSignature(channelSignature);
                });
            }
        }
    }

    @Id
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

    public Sha256Hash getHash () {
        return hash;
    }

    public void setHash (Sha256Hash hash) {
        this.hash = hash;
    }

    @Column
    @Convert(converter = NodeKeyConverter.class)
    public NodeKey getNodeKeyClient () {
        return nodeKeyClient;
    }

    public void setNodeKeyClient (NodeKey nodeKeyClient) {
        this.nodeKeyClient = nodeKeyClient;
    }

    @Column
    @Convert(converter = ECKeyConverter.class)
    public ECKey getKeyClient () {
        return keyClient;
    }

    public void setKeyClient (ECKey keyClient) {
        this.keyClient = keyClient;
    }

    @Column
    @Convert(converter = ECKeyConverter.class)
    public ECKey getKeyServer () {
        return keyServer;
    }

    public void setKeyServer (ECKey keyServer) {
        this.keyServer = keyServer;
    }

    public byte[] getMasterPrivateKeyClient () {
        return masterPrivateKeyClient;
    }

    public void setMasterPrivateKeyClient (byte[] masterPrivateKeyClient) {
        this.masterPrivateKeyClient = masterPrivateKeyClient;
    }

    public byte[] getMasterPrivateKeyServer () {
        return masterPrivateKeyServer;
    }

    public void setMasterPrivateKeyServer (byte[] masterPrivateKeyServer) {
        this.masterPrivateKeyServer = masterPrivateKeyServer;
    }

    public Integer getShaChainDepthCurrent () {
        return shaChainDepthCurrent;
    }

    public void setShaChainDepthCurrent (Integer shaChainDepthCurrent) {
        this.shaChainDepthCurrent = shaChainDepthCurrent;
    }

    public Integer getTimestampOpen () {
        return timestampOpen;
    }

    public void setTimestampOpen (Integer timestampOpen) {
        this.timestampOpen = timestampOpen;
    }

    public Integer getTimestampForceClose () {
        return timestampForceClose;
    }

    public void setTimestampForceClose (Integer timestampForceClose) {
        this.timestampForceClose = timestampForceClose;
    }

    @Column
    @Convert(converter = TransactionConverter.class)
    public Transaction getAnchorTx () {
        return anchorTx;
    }

    public void setAnchorTx (Transaction anchorTx) {
        this.anchorTx = anchorTx;
    }

    public Integer getMinConfirmationAnchor () {
        return minConfirmationAnchor;
    }

    public void setMinConfirmationAnchor (Integer minConfirmationAnchor) {
        this.minConfirmationAnchor = minConfirmationAnchor;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "channel")
    @OrderColumn
    public List<HibernateSignature> getSignatures () {
        return signatures;
    }

    public void setSignatures (List<HibernateSignature> signatures) {
        this.signatures = signatures;
    }

    public void addSignature(HibernateSignature signature) {
        signatures.add(signature);
        signature.setChannel(this);
    }

    public void clearSignatures() {
        signatures.forEach(signature -> {
            signatures.remove(signature);
            signature.setChannel(null);
        });
    }

    public Channel.Phase getPhase () {
        return phase;
    }

    public void setPhase (Channel.Phase phase) {
        this.phase = phase;
    }

    public Sha256Hash getAnchorTxHash () {
        return anchorTxHash;
    }

    public void setAnchorTxHash (Sha256Hash anchorTxHash) {
        this.anchorTxHash = anchorTxHash;
    }

    private ChannelStatus toChannelStatus() {
        ChannelStatus channelStatus = new ChannelStatus();
        channelStatus.amountClient = amountClient;
        channelStatus.amountServer = amountServer;
        channelStatus.paymentList = paymentList.stream()
                .map(PaymentDataEntity::toPaymentData)
                .collect(Collectors.toList());
        channelStatus.feePerByte = feePerByte;
        channelStatus.csvDelay = csvDelay;
        channelStatus.revoHashClientCurrent = revoHashClientCurrent == null ? null
                : revoHashClientCurrent.toRevocationHash();
        channelStatus.revoHashServerCurrent = revoHashServerCurrent == null ? null
                : revoHashServerCurrent.toRevocationHash();
        channelStatus.revoHashClientNext = revoHashClientNext == null ? null
                : revoHashClientNext.toRevocationHash();
        channelStatus.revoHashServerNext = revoHashServerNext == null ? null
                : revoHashServerNext.toRevocationHash();
        channelStatus.addressClient = addressClient;
        channelStatus.addressServer = addressServer;
        return channelStatus;
    }

    private void fromChannelStatus (ChannelStatus channelStatus) {
        amountClient = channelStatus.amountClient;
        amountServer = channelStatus.amountServer;
        feePerByte = channelStatus.feePerByte;
        csvDelay = channelStatus.csvDelay;
        if (channelStatus.revoHashClientCurrent != null) {
            revoHashClientCurrent = new RevocationHashEmbedded(channelStatus.revoHashClientCurrent);
        }
        if (channelStatus.revoHashServerCurrent != null) {
            revoHashServerCurrent = new RevocationHashEmbedded(channelStatus.revoHashServerCurrent);
        }
        if (channelStatus.revoHashClientNext != null) {
            revoHashClientNext = new RevocationHashEmbedded(channelStatus.revoHashClientNext);
        }
        if (channelStatus.revoHashServerNext != null) {
            revoHashServerNext = new RevocationHashEmbedded(channelStatus.revoHashServerNext);
        }
        addressClient = channelStatus.addressClient;
        addressServer = channelStatus.addressServer;
        if (channelStatus.paymentList != null) {
            channelStatus.paymentList.forEach(payment -> addPayment(new PaymentDataEntity(payment)));
        }
    }

    public long getAmountClient () {
        return amountClient;
    }

    public void setAmountClient (long amountClient) {
        this.amountClient = amountClient;
    }

    public long getAmountServer () {
        return amountServer;
    }

    public void setAmountServer (long amountServer) {
        this.amountServer = amountServer;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "channel")
    @OrderColumn
    public List<PaymentDataEntity> getPaymentList () {
        return paymentList;
    }

    public void setPaymentList (List<PaymentDataEntity> paymentList) {
        this.paymentList = paymentList;
    }

    public void addPayment (PaymentDataEntity payment) {
        paymentList.add(payment);
        payment.setChannel(this);
    }

    public void clearPayments () {
        paymentList.forEach(payment -> {
            paymentList.remove(payment);
            payment.setChannel(null);
        });
    }

    public int getFeePerByte () {
        return feePerByte;
    }

    public void setFeePerByte (int feePerByte) {
        this.feePerByte = feePerByte;
    }

    public int getCsvDelay () {
        return csvDelay;
    }

    public void setCsvDelay (int csvDelay) {
        this.csvDelay = csvDelay;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "index",
                    column = @Column(name = "client_revocation_index")
            ),
            @AttributeOverride(
                    name = "secret",
                    column = @Column(name = "client_revocation_secret")
            ),
            @AttributeOverride(
                    name = "secretHash",
                    column = @Column(name = "client_revocation_hash")
            )
    })
    public RevocationHashEmbedded getRevoHashClientCurrent () {
        return revoHashClientCurrent;
    }

    public void setRevoHashClientCurrent (RevocationHashEmbedded revoHashClientCurrent) {
        this.revoHashClientCurrent = revoHashClientCurrent;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "index",
                    column = @Column(name = "server_revocation_index")
            ),
            @AttributeOverride(
                    name = "secret",
                    column = @Column(name = "server_revocation_secret")
            ),
            @AttributeOverride(
                    name = "secretHash",
                    column = @Column(name = "server_revocation_hash")
            )
    })
    public RevocationHashEmbedded getRevoHashServerCurrent () {
        return revoHashServerCurrent;
    }

    public void setRevoHashServerCurrent (RevocationHashEmbedded revoHashServerCurrent) {
        this.revoHashServerCurrent = revoHashServerCurrent;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "index",
                    column = @Column(name = "next_client_revocation_index")
            ),
            @AttributeOverride(
                    name = "secret",
                    column = @Column(name = "next_client_revocation_secret")
            ),
            @AttributeOverride(
                    name = "secretHash",
                    column = @Column(name = "next_client_revocation_hash")
            )
    })
    public RevocationHashEmbedded getRevoHashClientNext () {
        return revoHashClientNext;
    }

    public void setRevoHashClientNext (RevocationHashEmbedded revoHashClientNext) {
        this.revoHashClientNext = revoHashClientNext;
    }

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "index",
                    column = @Column(name = "next_server_revocation_index")
            ),
            @AttributeOverride(
                    name = "secret",
                    column = @Column(name = "next_server_revocation_secret")
            ),
            @AttributeOverride(
                    name = "secretHash",
                    column = @Column(name = "next_server_revocation_hash")
            )
    })
    public RevocationHashEmbedded getRevoHashServerNext () {
        return revoHashServerNext;
    }

    public void setRevoHashServerNext (RevocationHashEmbedded revoHashServerNext) {
        this.revoHashServerNext = revoHashServerNext;
    }

    public Address getAddressClient () {
        return addressClient;
    }

    public void setAddressClient (Address addressClient) {
        this.addressClient = addressClient;
    }

    public Address getAddressServer () {
        return addressServer;
    }

    public void setAddressServer (Address addressServer) {
        this.addressServer = addressServer;
    }
}
