package network.thunder.core.communication.processor.interfaces;

import network.thunder.core.communication.processor.Processor;

/**
 * Created by matsjerratsch on 03/12/2015.
 */
public abstract class LNEstablishProcessor extends Processor {
    public static int MIN_CONFIRMATIONS = 0;
    public static int MAX_WAIT_FOR_OTHER_TX = 6;
    public static int MAX_WAIT_FOR_OTHER_TX_IF_SEEN = 12;
}
