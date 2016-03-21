package network.thunder.core.helper.blockchain.bciapi.exchangerates;

import java.math.BigDecimal;

/**
 * Used in response to the `getTicker` method in the `ExchangeRates` class.
 */
public class Currency {
    private BigDecimal buy;
    private BigDecimal sell;
    private BigDecimal last;
    private BigDecimal price15m;
    private String symbol;

    public Currency (double buy, double sell, double last, double price15m, String symbol) {
        this.buy = BigDecimal.valueOf(buy);
        this.sell = BigDecimal.valueOf(sell);
        this.last = BigDecimal.valueOf(last);
        this.price15m = BigDecimal.valueOf(price15m);
        this.symbol = symbol;
    }

    /**
     * @return Current buy price
     */
    public BigDecimal getBuy () {
        return buy;
    }

    /**
     * @return Current sell price
     */
    public BigDecimal getSell () {
        return sell;
    }

    /**
     * @return Most recent market price
     */
    public BigDecimal getLast () {
        return last;
    }

    /**
     * @return 15 minutes delayed market price
     */
    public BigDecimal getPrice15m () {
        return price15m;
    }

    /**
     * @return Currency symbol
     */
    public String getSymbol () {
        return symbol;
    }
}
