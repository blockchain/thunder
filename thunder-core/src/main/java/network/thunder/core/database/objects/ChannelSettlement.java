package network.thunder.core.database.objects;

import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

public class ChannelSettlement {
    public int settlementId;
    public Sha256Hash channelHash;

    public SettlementPhase phase;
    public int timeToSettle;

    //If the channelTx is from us, we have to move funds out with the double-tx approach
    //If it's not, we just can claim after timeout/with secret
    public boolean ourChannelTx;

    //If it's our channel and we cheated, there's nothing we can do (technically we can claim after the timeout, but thats not realistic to happen for now)
    public boolean cheated;

    //Cache these, so we don't have to brute force them every time
    //It may not contain the secret
    public RevocationHash revocationHash;

    public boolean payment;
    public PaymentData paymentData;

    //First and second tx denominate the channel tx where it could get claimed originally and the redeem/refund tx according to the new design
    //We need to watch out for both, as we can claim both
    public Transaction channelTx;
    public int channelTxHeight;
    public TransactionOutput channelOutput;
    public Transaction secondTx;
    public int secondTxHeight;
    public TransactionOutput secondOutput;
    public Transaction thirdTx;
    public int thirdTxHeight;
    public TransactionOutput thirdOutput;


    //This is the hash/index of the channeltransaction this settlement is about

    public enum SettlementPhase {
        UNSETTLED,
        SETTLED
    }
}
