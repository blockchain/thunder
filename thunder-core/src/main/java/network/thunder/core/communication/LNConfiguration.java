package network.thunder.core.communication;

import network.thunder.core.etc.Tools;

public class LNConfiguration {
    //Class for holding all the different settings with sane defaults set

    public int MIN_FEE_PER_BYTE = 1;
    public int DEFAULT_FEE_PER_BYTE = 5;
    public int MAX_FEE_PER_BYTE = 30;

    public int MIN_REVOCATION_DELAY = 7 * 24 * 60 * 60;
    public int DEFAULT_REVOCATION_DELAY = 2 * 7 * 24 * 60 * 60;
    public int MAX_REVOCATION_DELAY = 4 * 7 * 24 * 60 * 60;

    public int MIN_REFUND_DELAY = 1 * 60 * 60;
    public int DEFAULT_REFUND_DELAY = 3 * 60 * 60;
    public int MAX_REFUND_DELAY = 12 * 60 * 60;

    public int MIN_OVERLAY_REFUND = 2;
    public int DEFAULT_OVERLAY_REFUND = 3;
    public int MAX_OVERLAY_REFUND = 4;

    public int MAX_DIFF_TIMESTAMPS = 60;

    public int getTimeToReduceWhenRelayingPayment() {
        return Tools.getRandom(DEFAULT_REFUND_DELAY * DEFAULT_OVERLAY_REFUND, MAX_REFUND_DELAY * MAX_OVERLAY_REFUND);
    }
}
