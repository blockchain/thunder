package network.thunder.core.communication.objects.messages.interfaces.helper.etc;

import org.bitcoinj.core.Block;

public interface OnBlockCommand {
    boolean execute (Block block);
}
