package network.thunder.core.helper.blockchain;

import org.bitcoinj.core.Block;

public interface OnBlockCommand {
    boolean execute (Block block);
}
