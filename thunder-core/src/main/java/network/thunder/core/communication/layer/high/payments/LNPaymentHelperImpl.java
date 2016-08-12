package network.thunder.core.communication.layer.high.payments;

import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.ServerObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.communication.processor.exceptions.LNPaymentException;
import network.thunder.core.database.DBHandler;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.events.LNEventHelper;
import network.thunder.core.helper.events.LNEventListener;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LNPaymentHelperImpl implements LNPaymentHelper {
    private static final Logger log = Tools.getLogger();

    LNOnionHelper onionHelper;
    DBHandler dbHandler;
    LNEventHelper eventHelper;
    ServerObject serverObject;
    LNConfiguration configuration;

    Map<NodeKey, LNPaymentProcessor> processorList = new ConcurrentHashMap<>();

    public LNPaymentHelperImpl (ContextFactory contextFactory, DBHandler dbHandler) {
        this.onionHelper = contextFactory.getOnionHelper();
        this.dbHandler = dbHandler;
        this.eventHelper = contextFactory.getEventHelper();
        this.serverObject = contextFactory.getServerSettings();
        configuration = serverObject.configuration;

        this.eventHelper.addListener(new LNEventListener() {
            @Override
            public void onPaymentAdded (NodeKey nodeKey, PaymentData payment) {
                relayPayment(nodeKey, payment);
            }

            @Override
            public void onPaymentCompleted (PaymentData payment) {
                NodeKey senderOfPayment = dbHandler.getSenderOfPayment(payment.secret);
                if (senderOfPayment != null) {
                    pingProcessor(senderOfPayment);
                }
            }

            @Override
            public void onPaymentRefunded (PaymentData payment) {
                NodeKey senderOfPayment = dbHandler.getSenderOfPayment(payment.secret);
                if (senderOfPayment != null) {
                    pingProcessor(senderOfPayment);
                } else {
                    log.debug("LNPaymentHelperImpl.onPaymentRefunded - we were the sender?");
                }
            }
        });
    }

    @Override
    public void addProcessor (NodeKey nodeKey, LNPaymentProcessor processor) {
        processorList.put(nodeKey, processor);
    }

    @Override
    public void removeProcessor (NodeKey nodeKey) {
        processorList.remove(nodeKey);
    }

    private boolean pingProcessor (NodeKey nodeKey) {
        LNPaymentProcessor processor = processorList.get(nodeKey);
        if (processor != null) {
            processor.ping();
            return true;
        }
        return false;
    }

    public void relayPayment (NodeKey sender, PaymentData paymentData) {
        try {
            PeeledOnion peeledOnion = getPeeledOnion(paymentData);
            if (peeledOnion.isLastHop) {
                pingProcessor(sender);
            } else {
                paymentData.onionObject = peeledOnion.onionObject;
                NodeKey receiver = peeledOnion.nextHop;
                if (!pingProcessor(receiver)) {
                    if (dbHandler.getOpenChannel(receiver).size() == 0) {
                        //No payment channel with next hop, will just send back
                        pingProcessor(sender);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("", e);
            LNPaymentProcessor senderProcessor = processorList.get(sender);
            if (senderProcessor != null) {
                senderProcessor.ping();
            }
        }
    }

    @Override
    public void makePayment (PaymentData paymentData) {
        try {
            PeeledOnion peeledOnion = getPeeledOnion(paymentData);
            paymentData.onionObject = peeledOnion.onionObject;
            NodeKey nextHop = peeledOnion.nextHop;

            if (processorList.containsKey(nextHop)) {
                dbHandler.insertPayment(nextHop, paymentData);
                pingProcessor(nextHop);
            } else {
                throw new LNPaymentException("Not connected to next hop " + nextHop);
            }

        } catch (Exception e) {
            log.warn("", e);
            throw new LNPaymentException(e);
        }
    }

    private PeeledOnion getPeeledOnion (PaymentData paymentData) {
        return onionHelper.loadMessage(serverObject.pubKeyServer, paymentData.onionObject);
    }
}
