package network.thunder.core.etc;

import org.bitcoinj.core.Block;

public class BlockWrapper {
    public BlockWrapper (Block block, int height) {
        this.block = block;
        this.height = height;
    }

    public Block block;
    public int height;
}
