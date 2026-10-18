package com.cryptoportfolio.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cryptoportfolio.model.Transaction;
import com.cryptoportfolio.model.Transaction.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortfolioCalculatorTest {

    private static Transaction tx(Type type, String q, String p, String fee, long day) {
        Instant ts = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(day * 86400);
        return new Transaction("id" + day, "bitcoin", "BTC", "Bitcoin", type,
                new BigDecimal(q), new BigDecimal(p), new BigDecimal(fee), ts, ts);
    }

    private static void eq(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.setScale(8, java.math.RoundingMode.HALF_UP)),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void weightedAverageCostWithFees() {
        // buy 1 @100 (fee 1) -> cost 101 ; buy 1 @200 (fee 1) -> cost 302, qty 2, avg 151
        var p = PortfolioCalculator.compute(List.of(
                tx(Type.BUY, "1", "100", "1", 0),
                tx(Type.BUY, "1", "200", "1", 1))).get("bitcoin");
        eq("2", p.quantity);
        eq("302", p.costBasis);
        eq("151", p.avgCost());
        eq("0", p.realizedPnl);
    }

    @Test
    void sellRealizesProfitAndKeepsAverageCost() {
        // after the two buys above, sell 1 @300 fee 2 -> realized = 300 - 2 - 151 = 147 ; remaining qty 1 cost 151
        var p = PortfolioCalculator.compute(List.of(
                tx(Type.BUY, "1", "100", "1", 0),
                tx(Type.BUY, "1", "200", "1", 1),
                tx(Type.SELL, "1", "300", "2", 2))).get("bitcoin");
        eq("1", p.quantity);
        eq("151", p.costBasis);
        eq("151", p.avgCost());
        eq("147", p.realizedPnl);
    }

    @Test
    void sellingEverythingZeroesCostBasis() {
        var p = PortfolioCalculator.compute(List.of(
                tx(Type.BUY, "3", "10", "0", 0),
                tx(Type.SELL, "3", "12", "0", 1))).get("bitcoin");
        eq("0", p.quantity);
        eq("0", p.costBasis);
        eq("6", p.realizedPnl);
    }

    @Test
    void overSellIsRejected() {
        var ex = assertThrows(ApiException.class, () -> PortfolioCalculator.compute(List.of(
                tx(Type.BUY, "1", "10", "0", 0),
                tx(Type.SELL, "2", "12", "0", 1))));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void backdatedSellBeforeBuyIsRejected() {
        assertThrows(ApiException.class, () -> PortfolioCalculator.compute(List.of(
                tx(Type.BUY, "1", "10", "0", 5),
                tx(Type.SELL, "1", "12", "0", 1))));
    }
}
