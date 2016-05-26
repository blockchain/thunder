package network.thunder.core.communication.layer.high.channel.establish.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.processor.exceptions.LNEstablishException;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;

public class LNEstablishAMessage implements LNEstablish {
    //Data about the anchor
    public byte[] channelKeyServer;
    public long amountClient;
    public long amountServer;
    public byte[] anchorTransaction;
    public byte[] addressBytes;
    public int minConfirmationAnchor;

    //Data about the first commitment to be able to refund if necessary
    public RevocationHash revocationHash;
    public RevocationHash revocationHashNext;
    public int feePerByte;
    public int csvDelay;

    public LNEstablishAMessage (ECKey channelKeyServer, Transaction anchor, RevocationHash revocationHash, RevocationHash revocationHashNext, long clientAmount,
                                long serverAmount, int minConfirmationAnchor, Address address, int feePerByte, int csvDelay) {
        this.channelKeyServer = channelKeyServer.getPubKey();
        this.minConfirmationAnchor = minConfirmationAnchor;
        this.anchorTransaction = anchor.bitcoinSerialize();
        this.revocationHash = revocationHash;
        this.revocationHashNext = revocationHashNext;
        this.amountClient = clientAmount;
        this.amountServer = serverAmount;
        this.addressBytes = address.getHash160();
        this.feePerByte = feePerByte;
        this.csvDelay = csvDelay;

        this.revocationHash.secret = null;
        this.revocationHashNext.secret = null;
    }

    @Override
    public Channel saveToChannel (Channel channel) {
        try {
            Transaction newAnchorTx = new Transaction(Constants.getNetwork(), anchorTransaction);

            if (channel.anchorTx != null) {
                if (Tools.checkIfExistingInOutPutsAreEqual(channel.anchorTx, newAnchorTx)) {
                    throw new LNEstablishException("Our in/outputs of the anchor has been changed..");
                }
            }

            //Don't allow changing the values for now, symmetric messages are easy to implement though
            if (channel.channelStatus.amountServer == 0 || channel.channelStatus.amountClient == 0) {
                channel.channelStatus.amountClient = amountClient;
                channel.channelStatus.amountServer = amountServer;
            }

            channel.keyClient = ECKey.fromPublicOnly(channelKeyServer);
            channel.anchorTx = newAnchorTx;
            channel.channelStatus.addressClient = new Address(Constants.getNetwork(), addressBytes);
            channel.channelStatus.revoHashClientCurrent = revocationHash;
            channel.channelStatus.revoHashClientNext = revocationHashNext;
            channel.channelStatus.csvDelay = csvDelay;
            channel.channelStatus.feePerByte = feePerByte;
            channel.minConfirmationAnchor = minConfirmationAnchor;
            channel.shaChainDepth = 0;
            return channel;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(addressBytes);
        Preconditions.checkNotNull(anchorTransaction);
        Preconditions.checkNotNull(revocationHash);
    }

    @Override
    public String toString () {
        return "LNEstablishAMessage{" +
                "amountServer=" + amountServer +
                ", amountClient=" + amountClient +
                '}';
    }
}
