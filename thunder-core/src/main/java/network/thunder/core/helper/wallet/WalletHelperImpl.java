package network.thunder.core.helper.wallet;

import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.ScriptBuilder;

import java.util.*;

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
    public Transaction addInputs (Transaction transaction, long value, float feePerByte) {
        return addOutAndInputs(transaction, value, feePerByte);
    }

    @Override
    public Transaction signTransaction (Transaction transaction) {
        return signTransaction(transaction, wallet);
    }

    @Override
    public Address fetchAddress () {
        return wallet.freshReceiveAddress();
    }

    private Transaction addOutAndInputs (Transaction transaction, long value, float feePerByte) {

        long totalInput = 0;
        long neededAmount = value + Tools.getTransactionFees(20, 20, feePerByte); //TODO obviously a hack, either use bitcoinj or make it smarter

        List<TransactionOutput> spendable = getUnlockedOutputs();
        List<TransactionOutput> outputList = addInputs(totalInput, neededAmount, spendable);
        totalInput = Tools.getCoinValueFromOutput(outputList);

        if (totalInput < neededAmount) {
            /*
             * Not enough outputs in total to pay for the channel..
             */
            throw new RuntimeException("Wallet Balance not sufficient. " + totalInput + "<" + neededAmount); //TODO
        } else {

            //Fee calculation still not perfect, since both nodes that sign it run it, so fees for inputs are paid twice
            long actualFee = Tools.getTransactionFees(outputList.size(), 3, feePerByte);
            transaction.addOutput(Coin.valueOf(totalInput - value - actualFee), wallet.freshReceiveAddress());

            for (TransactionOutput o : outputList) {
                transaction.addInput(o);
            }

            for (TransactionOutput output : outputList) {
                lockOutput(output);
            }
        }

        return transaction;
    }

    private static List<TransactionOutput> addInputs (long totalInput, long neededAmount, List<TransactionOutput> spendable) {
        List<TransactionOutput> outputList = new ArrayList<>();
        for (TransactionOutput o : spendable) {
            if (o.getValue().value > neededAmount) {
                /*
                 * Ok, found a suitable output, need to split the change
                 * TODO: Change (a few things), such that there will be no output < 500...
                 */
                outputList.add(o);
                totalInput += o.getValue().value;
                break;

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
            throw new RuntimeException("Wallet Balance not sufficient. " + totalInput + "<" + neededAmount); //TODO
        }
        return outputList;
    }

    private static Transaction signTransaction (Transaction transaction, Wallet wallet) {
        //TODO: Currently only working if we have P2PKH outputs in our wallet
        int j = 0;
        for (int i = 0; i < transaction.getInputs().size(); ++i) {
            TransactionInput input = transaction.getInput(i);
            Optional<TransactionOutput> optional =
                    wallet.calculateAllSpendCandidates().stream().filter(out -> input.getOutpoint().equals(out.getOutPointFor())).findAny();
            if (optional.isPresent()) {
                TransactionOutput output = optional.get();
                Address address = output.getAddressFromP2PKHScript(Constants.getNetwork());

                //Only sign P2PKH and only those that we possess the key for..
                if (address != null) {
                    ECKey key = wallet.findKeyFromPubHash(address.getHash160());
                    if (key != null) {
                        TransactionSignature sig = Tools.getSignature(transaction, i, output, key);
                        byte[] s = sig.encodeToBitcoin();
                        ScriptBuilder builder = new ScriptBuilder();
                        builder.data(s);
                        builder.data(key.getPubKey());
                        transaction.getInput(i).setScriptSig(builder.build());
                    }
                }
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
