package network.thunder.core.etc;

import org.bitcoinj.core.TransactionWitness;

public class TransactionWitnessMutable extends TransactionWitness {

    public TransactionWitnessMutable (int pushCount) {
        super(pushCount);
        pushes = new byte[pushCount][];
    }

    byte[][] pushes;

    @Override
    public byte[] getPush (int i) {
        return pushes[i];
    }

    @Override
    public int getPushCount () {
        return pushes.length;
    }

    public void set (int i, byte[] data) {
        pushes[i] = data;
    }
}
