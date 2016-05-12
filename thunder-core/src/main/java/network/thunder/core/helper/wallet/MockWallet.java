package network.thunder.core.helper.wallet;

import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.*;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.WalletTransaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MockWallet extends Wallet {

    List<ECKey> keyList = new ArrayList<>();
    List<TransactionOutput> outputs = new ArrayList<>();

    public MockWallet (NetworkParameters params) {
        this(params, 100);
    }

    public MockWallet (NetworkParameters params, int totalOutputs) {
        super(params);
        //

        Random random = new Random();

        for (int i = 1; i < 101; i++) {
            Transaction transaction = new Transaction(Constants.getNetwork());

            byte[] h = new byte[32];
            random.nextBytes(h);

            ECKey k = new ECKey(new BigInteger(h), null, true);
            keyList.add(k);

            transaction.addInput(Sha256Hash.wrap(h), 0, Tools.getDummyScript());

            TransactionOutput a = new TransactionOutput(Constants.getNetwork(), transaction, Coin.valueOf(1000000), k.toAddress(Constants.getNetwork()));
            transaction.addOutput(a);

            outputs.add(a);

        }

    }

    public MockWallet (Context context) {
        super(context);
    }

    public MockWallet (NetworkParameters params, KeyChainGroup keyChainGroup) {
        super(params, keyChainGroup);
    }

    public MockWallet (Context context, KeyChainGroup keyChainGroup) {
        super(context, keyChainGroup);
    }

    @Override
    public List<TransactionOutput> calculateAllSpendCandidates (boolean excludeImmatureCoinbases, boolean excludeUnsignable) {
        return outputs;
    }

    @Override
    public Coin getBalance () {
        final long[] value = {0};
        this.outputs.stream().forEach(transactionOutput -> value[0] += transactionOutput.getValue().value);
        return Coin.valueOf(value[0]);
    }

    @Override
    public void addWalletTransaction (WalletTransaction wtx) {
        wtx.getTransaction().getInputs().stream().forEach(new Consumer<TransactionInput>() {
            @Override
            public void accept (TransactionInput transactionInput) {
                outputs = outputs.stream().filter(new Predicate<TransactionOutput>() {
                    @Override
                    public boolean test (TransactionOutput transactionOutput) {
                        return !transactionInput.getOutpoint().equals(transactionOutput.getOutPointFor());
                    }
                }).collect(Collectors.toList());
            }
        });
        wtx.getTransaction().getOutputs().forEach(transactionOutput ->
        {
            Address address = transactionOutput.getAddressFromP2PKHScript(Constants.getNetwork());
            if (address != null) {
                if (keyList.stream().anyMatch(
                        ecKey -> ecKey.toAddress(Constants.getNetwork()).equals(address))) {
                    outputs.add(transactionOutput);
                }
            } else {

            }

        });

    }

    @Override
    public ECKey findKeyFromPubHash (byte[] pubkeyHash) {
        for (ECKey k : keyList) {
            if (Arrays.equals(pubkeyHash, k.toAddress(Constants.getNetwork()).getHash160())) {
                return k;
            }
        }
        return null;
    }

    @Override
    public boolean addKey (ECKey key) {
        keyList.add(key);
        return true;
    }

    @Override
    public Address freshReceiveAddress () {
        return keyList.get(0).toAddress(Constants.getNetwork());
    }
}
