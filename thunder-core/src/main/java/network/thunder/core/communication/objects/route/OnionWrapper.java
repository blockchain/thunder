package network.thunder.core.communication.objects.route;

import network.thunder.core.communication.objects.OnionObject;

/**
 * Created by matsjerratsch on 07/10/2015.
 */
public class OnionWrapper {

    private OnionObject onionObject;

    private boolean isEncrypted = true;
    private boolean isPrepared = false;
    private boolean isHandled = false;

    private byte[] publicKey;
    private byte[] privateKey;

    public OnionWrapper (OnionObject onionObject) {
        this.onionObject = onionObject;
    }

    public boolean decrypt () {

        if (!isEncrypted) {
            return false;
        }

        return true;
    }

}
