package network.thunder.core.database.hibernate;

import org.bitcoinj.crypto.TransactionSignature;

import javax.persistence.*;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

@Entity(name = "Signature")
public class HibernateSignature {
    private Integer id;
    private TransactionSignature transactionSignature;
    private Type type;
    private ChannelEntity channel;

    public enum Type {
        CHANNEL, PAYMENT, CLOSING
    }

    public HibernateSignature () {
    }

    public HibernateSignature (TransactionSignature transactionSignature, Type type) {
        this.transactionSignature = transactionSignature;
        this.type = type;
    }

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    @Column
    @Convert(converter = TransactionSignatureConverter.class)
    public TransactionSignature getTransactionSignature () {
        return transactionSignature;
    }

    public void setTransactionSignature (TransactionSignature transactionSignature) {
        this.transactionSignature = transactionSignature;
    }

    public Type getType () {
        return type;
    }

    public void setType (Type type) {
        this.type = type;
    }

    @ManyToOne
    public ChannelEntity getChannel () {
        return channel;
    }

    public void setChannel (ChannelEntity channel) {
        this.channel = channel;
    }
}
