package network.thunder.core.helper.blockchain;

import network.thunder.core.etc.BlockWrapper;

public interface OnBlockCommand {
    void execute (BlockWrapper blockWrapper);
}
