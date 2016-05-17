package network.thunder.core.database;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.*;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;
import java.util.Properties;

public class HibernateDBHandler implements DBHandler {

    SessionFactory sessionFactory;

    public HibernateDBHandler () {

        Properties properties = new Properties();

//        properties.put("hibernate.archive.autodetection", "class,hbm");
//        properties.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        properties.put("hibernate.show_sql", "true");
//        properties.put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
//        properties.put("hibernate.connection.username", "sa");
//        properties.put("hibernate.connection.password", "1");
        properties.put("hibernate.connection.url", "jdbc:hsqldb:mem:howtodoinjava");
//        properties.put("hibernate.hbm2ddl.auto", "create");

        Configuration configuration = new Configuration();

//        configuration.addAttributeConverter(AddressAttributeConverter.class, true);
//        configuration.addAttributeConverter(TransactionAttributeConverter.class, true);
//        configuration.addAttributeConverter(RevocationHashAttributeConverter.class, true);

        configuration
//                .addAnnotatedClass(P2PDataObject.class)
//                .addAnnotatedClass(PubkeyIPObject.class)
//                .addAnnotatedClass(PubkeyChannelObject.class)
//                .addAnnotatedClass(ChannelStatusObject.class)
//                .addAnnotatedClass(PaymentWrapper.class)

                .addAnnotatedClass(RevocationHashAttributeConverter.class)
                .addAnnotatedClass(TransactionAttributeConverter.class)
                .addAnnotatedClass(AddressAttributeConverter.class)

                .addAnnotatedClass(AddressWrapper.class)
                .addAnnotatedClass(RevocationHashWrapper.class)
                .addAnnotatedClass(TransactionWrapper.class)


                .addAnnotatedClass(Channel.class)

//                .addAnnotatedClass(RevocationHash.class)
//                .addAnnotatedClass(ChannelStatus.class)
//                .addAnnotatedClass(ChannelSignatures.class)
                .addProperties(properties);



        this.sessionFactory = configuration.buildSessionFactory();
    }

    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
        try (Session session = sessionFactory.openSession()) {
            Query q;
//            q = session.createQuery("from channelstatusobject where fragmentindex = :i");
//            q.setInteger("i", fragmentIndex);
//            List channelStatusList = q.list();
//
//            q = session.createQuery("from pubkeychannelobject where fragmentindex = :i");
//            q.setInteger("i", fragmentIndex);
//            List pubkeychannelList = q.list();
//
//            q = session.createQuery("from pubkeyipobject where fragmentindex = :i");
//            q.setInteger("i", fragmentIndex);
//            List pubkeyIPList = q.list();

            q = session.createQuery("from p2pdataobject where fragmentindex = :i");
            q.setInteger("i", fragmentIndex);
            List pubkeyIPList = q.list();
            return pubkeyIPList;
        }
    }

    @Override
    public List<P2PDataObject> getSyncDataIPObjects () {
        try (Session session = sessionFactory.openSession()) {
            Query q;
            q = session.createQuery("from p2pdataobject p where p.class = PubkeyIPObject");
            List pubkeyIPList = q.list();
            return pubkeyIPList;
        }
    }

    @Override
    public void insertIPObjects (List<P2PDataObject> ipList) {
        try (Session session = sessionFactory.openSession()) {
            ipList.forEach(session::save);
        }
    }

    @Override
    public List<PubkeyIPObject> getIPObjects () {
        try (Session session = sessionFactory.openSession()) {
            Query q;
            q = session.createQuery("from p2pdataobject p where p.class = PubkeyIPObject");
            List pubkeyIPList = q.list();
            return pubkeyIPList;
        }
    }

    @Override
    public P2PDataObject getP2PDataObjectByHash (byte[] hash) {
        try (Session session = sessionFactory.openSession()) {
            Query q;
            q = session.createQuery("from p2pdataobject where hash = :h");
            q.setBinary("i", hash);
            return (P2PDataObject) q.list().get(0);
        }
    }

    @Override
    public PubkeyIPObject getIPObject (byte[] nodeKey) {
        try (Session session = sessionFactory.openSession()) {
            Query q;
            q = session.createQuery("from p2pdataobject p where p.class = PubkeyIPObject AND pubkey = :h");
            q.setBinary("i", nodeKey);
            return (PubkeyIPObject) q.list().get(0);
        }
    }

    @Override
    public void invalidateP2PObject (P2PDataObject ipObject) {

    }

    @Override
    public void syncDatalist (List<P2PDataObject> dataList) {
        try (Session session = sessionFactory.openSession()) {
            session.save(dataList);
        }
    }

    @Override
    public void insertRevocationHash (RevocationHash hash) {
        try (Session session = sessionFactory.openSession()) {
            session.save(hash);
        }
    }

    @Override
    public RevocationHash createRevocationHash (Channel channel) {
        RevocationHash revocationHash = new RevocationHash(1, 1, Tools.getRandomByte(20));
        try (Session session = sessionFactory.openSession()) {
            session.save(revocationHash);
        }
        return revocationHash;
    }

    @Override
    public List<RevocationHash> getOldRevocationHashes (Channel channel) {
        return null;
    }

    @Override
    public boolean checkOldRevocationHashes (List<RevocationHash> revocationHashList) {
        try (Session session = sessionFactory.openSession()) {
            session.save(revocationHashList);
        }
        return true;
    }

    @Override
    public Channel getChannel (Sha256Hash hash) {
        return null;
    }

    @Override
    public List<Channel> getChannel (ECKey nodeKey) {
        return null;
    }

    @Override
    public List<Channel> getOpenChannel (ECKey nodeKey) {
        return null;
    }

    @Override
    public void saveChannel (Channel channel) {
    }

    @Override
    public void updateChannel (Channel channel) {

    }

    @Override
    public List<Channel> getOpenChannel () {
        return null;
    }

    @Override
    public List<PubkeyIPObject> getIPObjectsWithActiveChannel () {
        return null;
    }

    @Override
    public List<ChannelStatusObject> getTopology () {
        return null;
    }

    @Override
    public byte[] getSenderOfPayment (PaymentSecret paymentSecret) {
        return new byte[0];
    }

    @Override
    public byte[] getReceiverOfPayment (PaymentSecret paymentSecret) {
        return new byte[0];
    }

    @Override
    public void addPayment (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePayment (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePaymentSender (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePaymentReceiver (PaymentWrapper paymentWrapper) {

    }

    @Override
    public void updatePaymentAddReceiverAddress (PaymentSecret secret, byte[] receiver) {

    }

    @Override
    public PaymentWrapper getPayment (PaymentSecret paymentSecret) {
        return null;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {

    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        return null;
    }

    @Override
    public List<PaymentWrapper> getAllPayments () {
        return null;
    }

    @Override
    public List<PaymentWrapper> getOpenPayments () {
        return null;
    }

    @Override
    public List<PaymentWrapper> getRefundedPayments () {
        return null;
    }

    @Override
    public List<PaymentWrapper> getRedeemedPayments () {
        return null;
    }
}
