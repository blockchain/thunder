package network.thunder.core.etc;

import network.thunder.core.communication.objects.lightning.subobjects.ChannelStatus;
import network.thunder.core.communication.objects.messages.interfaces.message.lnpayment.LNPayment;
import network.thunder.core.communication.processor.interfaces.lnpayment.LNPaymentLogic;
import network.thunder.core.lightning.RevocationHash;
import org.bitcoinj.core.Transaction;

/**
 * Created by matsjerratsch on 13/01/2016.
 */
public class MockLNPaymentLogic implements LNPaymentLogic {
    @Override
    public Transaction getClientTransaction () {
        return null;
    }

    @Override
    public Transaction getServerTransaction () {
        return null;
    }

    @Override
    public void checkMessage (LNPayment message) {

    }

    @Override
    public ChannelStatus getTemporaryChannelStatus () {
        return null;
    }

    @Override
    public void putCurrentRevocationHashServer (RevocationHash revocationHash) {

    }

    @Override
    public void putNewChannelStatus (ChannelStatus channelStatus) {

    }
}
