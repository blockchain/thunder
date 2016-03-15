package network.thunder.core.communication.objects.messages.interfaces.helper.etc;

import network.thunder.core.communication.objects.messages.interfaces.helper.BlockchainHelper;
import network.thunder.core.communication.objects.messages.interfaces.helper.ChannelManager;

public interface ChannelFailureAction {
    void execute (ChannelManager manager, BlockchainHelper blockchainHelper);
}
