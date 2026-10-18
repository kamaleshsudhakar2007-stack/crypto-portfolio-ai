package com.cryptoportfolio.service;

import com.cryptoportfolio.model.Transaction;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Weighted-average cost basis engine (pure logic, no Spring).
 *
 * Conventions:
 *  - BUY : quantity += q ; costBasis += q*price + fee      (fees are capitalised into cost)
 *  - SELL: avgCost = costBasis/quantity (before the sale)
 *          costRemoved = avgCost*q ; realized += q*price - fee - costRemoved
 *          quantity -= q ; costBasis -= costRemoved        (average cost per unit is unchanged)
 *  - Transactions are processed in chronological order, so a back-dated SELL that would
 *    oversell at that point in time is rejected.
 */
public final class PortfolioCalculator {
    public static final MathContext MC = new MathContext(28, RoundingMode.HALF_UP);

    private PortfolioCalculator() {}

    public static final class Position {
        public final String coinId;
        public String symbol;
        public String name;
        public BigDecimal quantity = BigDecimal.ZERO;
        public BigDecimal costBasis = BigDecimal.ZERO;
        public BigDecimal realizedPnl = BigDecimal.ZERO;

        Position(String coinId, String symbol, String name) {
            this.coinId = coinId;
            this.symbol = symbol;
            this.name = name;
        }

        public BigDecimal avgCost() {
            return quantity.signum() > 0 ? costBasis.divide(quantity, MC) : BigDecimal.ZERO;
        }
    }

    public static Map<String, Position> compute(List<Transaction> transactions) {
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator
                .comparing(Transaction::timestamp, Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
                .thenComparing(Transaction::createdAt, Comparator.nullsLast(Comparator.<Instant>naturalOrder())));

        Map<String, Position> positions = new LinkedHashMap<>();
        for (Transaction t : sorted) {
            Position p = positions.computeIfAbsent(t.coinId(), k -> new Position(t.coinId(), t.symbol(), t.name()));
            BigDecimal fee = t.fee() == null ? BigDecimal.ZERO : t.fee();
            BigDecimal gross = t.quantity().multiply(t.price(), MC);

            if (t.type() == Transaction.Type.BUY) {
                p.quantity = p.quantity.add(t.quantity());
                p.costBasis = p.costBasis.add(gross).add(fee);
            } else {
                if (t.quantity().compareTo(p.quantity) > 0) {
                    throw new ApiException(400, "Cannot sell " + t.quantity().stripTrailingZeros().toPlainString()
                            + " " + t.symbol() + " on " + t.timestamp() + ": only "
                            + p.quantity.stripTrailingZeros().toPlainString() + " available at that time.");
                }
                BigDecimal removed = t.quantity().compareTo(p.quantity) == 0
                        ? p.costBasis
                        : p.costBasis.multiply(t.quantity(), MC).divide(p.quantity, MC);
                p.realizedPnl = p.realizedPnl.add(gross.subtract(fee).subtract(removed));
                p.quantity = p.quantity.subtract(t.quantity());
                p.costBasis = p.costBasis.subtract(removed);
            }
        }
        return positions;
    }
}
