package network.thunder.core.communication.processor.implementations;

import network.thunder.core.communication.Message;
import network.thunder.core.communication.objects.OnionObject;
import network.thunder.core.communication.objects.lightning.subobjects.PaymentData;
import network.thunder.core.communication.objects.messages.MessageExecutor;
import network.thunder.core.communication.objects.messages.interfaces.factories.LNPaymentMessageFactory;
import network.thunder.core.communication.objects.messages.interfaces.helper.LNPaymentHelper;
import network.thunder.core.communication.processor.interfaces.LNPaymentProcessor;
import network.thunder.core.database.objects.Channel;
import network.thunder.core.mesh.Node;

/**
 * Created by matsjerratsch on 04/01/2016.
 */
public class LNPaymentProcessorImpl implements LNPaymentProcessor {
    LNPaymentMessageFactory messageFactory;
    Node node;
    MessageExecutor messageExecutor;

    LNPaymentHelper paymentHelper;

    Channel channel;

    int status = 0;

    @Override
    public boolean connectsToNodeId (byte[] nodeId) {
        return false;
    }

    @Override
    public void makePayment (PaymentData paymentData, OnionObject onionObject) {

    }

    @Override
    public void redeemPayment (PaymentData paymentData) {

    }

    @Override
    public void refundPayment (PaymentData paymentData) {

    }

    @Override
    public void onInboundMessage (Message message) {

    }

    @Override
    public void onOutboundMessage (Message message) {

    }

    @Override
    public void onLayerActive (MessageExecutor messageExecutor) {

    }

    private void sendMessageA () {

    }

    private void sendMessageB () {

    }

    private void sendMessageC () {

    }

    private void sendMessageD () {

    }
}
