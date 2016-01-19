package network.thunder.core.communication.objects.messages.impl;

import network.thunder.core.communication.objects.messages.interfaces.helper.WalletHelper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.ScriptBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by matsjerratsch on 19/01/2016.
 */
public class WalletHelperImpl implements WalletHelper {
    final static int TIME_LOCK_IN_SECONDS = 60;

    Wallet wallet;
    Map<TransactionOutput, Integer> lockedOutputs = new HashMap<>();

    public WalletHelperImpl (Wallet wallet) {
        this.wallet = wallet;
    }

    @Override
    public long getSpendableAmount () {
        return Tools.getCoinValueFromOutput(getUnlockedOutputs());
    }

    @Override
    public Transaction completeInputs (Transaction transaction) {
        return addOutAndInputs(transaction);
    }

    private Transaction addOutAndInputs (Transaction transaction) {

        long totalInput = 0;
        long value = Tools.getCoinValueFromOutput(transaction.getOutputs());
        long neededAmount = value + Tools.getTransactionFees(20, 2);

        List<TransactionOutput> outputList = new ArrayList<>();
        List<TransactionOutput> spendable = getUnlockedOutputs();

        for (TransactionOutput o : spendable) {
            if (o.getValue().value > neededAmount) {
                /*
                 * Ok, found a suitable output, need to split the change
                 * TODO: Change (a few things), such that there will be no output < 500...
                 */
                outputList.add(o);
                totalInput += o.getValue().value;

            }
        }
        if (totalInput == 0) {
            /*
             * None of our outputs alone is sufficient, have to add multiples..
             */
            for (TransactionOutput o : spendable) {
                if (totalInput >= neededAmount) {
                    continue;
                }
                totalInput += o.getValue().value;
                outputList.add(o);
            }
        }

        if (totalInput < neededAmount) {
            /*
             * Not enough outputs in total to pay for the channel..
             */
            throw new RuntimeException("Wallet Balance not sufficient"); //TODO
        } else {

            transaction.addOutput(Coin.valueOf(totalInput - value - Tools.getTransactionFees(2, 2)), wallet.freshReceiveAddress());

            for (TransactionOutput o : outputList) {
                transaction.addInput(o);
            }

            /*
             * Sign all of our inputs..
             */
            int j = 0;
            for (int i = 0; i < outputList.size(); i++) {
                TransactionOutput o = outputList.get(i);
                ECKey key = wallet.findKeyFromPubHash(o.getAddressFromP2PKHScript(Constants.getNetwork()).getHash160());
                System.out.println(key.toAddress(Constants.getNetwork()));
                TransactionSignature sig = Tools.getSignature(transaction, i, o, key);
                byte[] s = sig.encodeToBitcoin();
                ScriptBuilder builder = new ScriptBuilder();
                builder.data(s);
                builder.data(key.getPubKey());
                transaction.getInput(i).setScriptSig(builder.build());
                //TODO: Currently only working if we have P2PKH outputs in our wallet
            }

            for (TransactionOutput output : outputList) {
                lockOutput(output);
            }
        }

        return transaction;
    }

    private List<TransactionOutput> getUnlockedOutputs () {
        List<TransactionOutput> spendable = new ArrayList<>(wallet.calculateAllSpendCandidates());
        List<TransactionOutput> tempList = new ArrayList<>(spendable);
        for (TransactionOutput o : tempList) {
            Integer timeLock = lockedOutputs.get(o);
            if (timeLock != null && timeLock > Tools.currentTime()) {
                spendable.remove(o);
            }
        }
        return spendable;
    }

    private void lockOutput (TransactionOutput output) {
        lockedOutputs.put(output, Tools.currentTime() + TIME_LOCK_IN_SECONDS);
    }

}
