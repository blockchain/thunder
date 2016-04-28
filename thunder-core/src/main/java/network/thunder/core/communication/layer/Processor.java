package network.thunder.core.communication.layer;

public abstract class Processor {
    public void onInboundMessage (Message message) {

    }

    public void onOutboundMessage (Message message) {

    }

    public abstract void onLayerActive (MessageExecutor messageExecutor);

    public void onLayerClose () {

    }

    public boolean consumesInboundMessage (Object object) {
        return true;
    }

    public boolean consumesOutboundMessage (Object object) {
        return true;
    }

}
