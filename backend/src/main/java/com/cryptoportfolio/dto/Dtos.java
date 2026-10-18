package com.cryptoportfolio.dto;

import com.cryptoportfolio.model.Alert;
import com.cryptoportfolio.model.Snapshot;
import com.cryptoportfolio.model.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** All money fields in this API are INR (Indian rupees). Market prices come from CoinGecko already in INR. */
public final class Dtos {
    private Dtos() {}

    // ---------- requests ----------
    public record TransactionRequest(
            @NotBlank String coinId,
            @NotNull Transaction.Type type,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
            @DecimalMin("0") BigDecimal fee,
            Instant timestamp) {}

    /** Adds a holding = records an initial BUY transaction. */
    public record HoldingRequest(
            @NotBlank String coinId,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
            @DecimalMin("0") BigDecimal fee,
            Instant timestamp) {}

    public record AlertRequest(
            @NotBlank String coinId,
            @NotNull Alert.Direction direction,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal targetPrice) {}

    // ---------- market ----------
    public record Coin(String id, String symbol, String name, BigDecimal price,
                       BigDecimal change24h, BigDecimal marketCap, String image) {}

    /** source: "live" | "stale" (last successful live data) | "demo" (static sample data). */
    public record CoinList(List<Coin> coins, String source, Instant updatedAt, String notice) {}

    // ---------- portfolio ----------
    public record Holding(String coinId, String symbol, String name, BigDecimal quantity,
                          BigDecimal avgCost, BigDecimal costBasis, BigDecimal currentPrice,
                          BigDecimal currentValue, BigDecimal unrealizedPnl, BigDecimal returnPct,
                          BigDecimal realizedPnl, BigDecimal allocationPct, boolean priceAvailable) {}

    public record PortfolioSummary(BigDecimal totalValue, BigDecimal totalInvested,
                                   BigDecimal unrealizedPnl, BigDecimal returnPct,
                                   BigDecimal realizedPnl, int holdingsCount,
                                   String source, Instant updatedAt, String notice) {}

    public record Portfolio(List<Holding> holdings, PortfolioSummary summary) {}

    // ---------- analytics ----------
    public record AllocationItem(String coinId, String symbol, BigDecimal value, BigDecimal pct) {}

    public record AssetPnl(String symbol, BigDecimal unrealizedPnl, BigDecimal realizedPnl, BigDecimal returnPct) {}

    public record Performer(String symbol, BigDecimal returnPct) {}

    public record Analytics(List<AllocationItem> allocation, List<AssetPnl> pnlByAsset,
                            Performer best, Performer worst,
                            String topHoldingSymbol, BigDecimal topHoldingPct,
                            List<Snapshot> performance, String performanceNote,
                            String source, Instant updatedAt) {}

    // ---------- alerts ----------
    public record AlertList(List<Alert> alerts, String source, Instant updatedAt, String notice) {}

    // ---------- insights ----------
    public record Insight(String severity, String title, String text) {}

    public record InsightResponse(String mode, List<Insight> insights, String aiSummary,
                                  String aiProvider, String aiNotice, String disclaimer,
                                  String source, Instant generatedAt) {}

    // ---------- errors ----------
    public record ErrorResponse(Instant timestamp, int status, String error, String message,
                                Map<String, String> details) {}
}
