package com.cryptoportfolio.service;

import com.cryptoportfolio.dto.Dtos.*;
import com.cryptoportfolio.model.Snapshot;
import com.cryptoportfolio.model.Transaction;
import com.cryptoportfolio.repo.SnapshotRepository;
import com.cryptoportfolio.repo.TransactionRepository;
import com.cryptoportfolio.service.PortfolioCalculator.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Duration SNAPSHOT_EVERY = Duration.ofMinutes(15);

    private final TransactionRepository txRepo;
    private final SnapshotRepository snapRepo;
    private final MarketService market;

    public PortfolioService(TransactionRepository txRepo, SnapshotRepository snapRepo, MarketService market) {
        this.txRepo = txRepo;
        this.snapRepo = snapRepo;
        this.market = market;
    }

    // ------------------------------------------------------------ transactions

    public List<Transaction> listTransactions() {
        List<Transaction> all = new ArrayList<>(txRepo.findAll());
        all.sort(Comparator.comparing(Transaction::timestamp, Comparator.nullsLast(Comparator.<Instant>naturalOrder())).reversed());
        return all;
    }

    public Transaction addTransaction(TransactionRequest r) {
        Transaction tx = build(null, r.coinId(), r.type(), r.quantity(), r.price(), r.fee(), r.timestamp(), null, null);
        List<Transaction> all = new ArrayList<>(txRepo.findAll());
        all.add(tx);
        PortfolioCalculator.compute(all); // throws if any chronological sell oversells
        return txRepo.save(tx);
    }

    public Transaction addHolding(HoldingRequest r) {
        return addTransaction(new TransactionRequest(r.coinId(), Transaction.Type.BUY, r.quantity(), r.price(), r.fee(), r.timestamp()));
    }

    public Transaction updateTransaction(String id, TransactionRequest r) {
        Transaction existing = txRepo.findById(id).orElseThrow(() -> new ApiException(404, "Transaction not found."));
        Transaction tx = build(id, r.coinId(), r.type(), r.quantity(), r.price(), r.fee(), r.timestamp(), existing, existing.createdAt());
        List<Transaction> all = txRepo.findAll().stream().filter(t -> !t.id().equals(id)).collect(Collectors.toCollection(ArrayList::new));
        all.add(tx);
        PortfolioCalculator.compute(all);
        return txRepo.save(tx);
    }

    public void deleteTransaction(String id) {
        if (!txRepo.existsById(id)) throw new ApiException(404, "Transaction not found.");
        List<Transaction> rest = txRepo.findAll().stream().filter(t -> !t.id().equals(id)).toList();
        try {
            PortfolioCalculator.compute(rest);
        } catch (ApiException e) {
            throw new ApiException(409, "This transaction cannot be deleted because a later sell depends on it. " + e.getMessage());
        }
        txRepo.deleteById(id);
    }

    public void deleteHolding(String coinId) {
        if (txRepo.deleteByCoinId(coinId) == 0) throw new ApiException(404, "No holding found for " + coinId + ".");
    }

    private Transaction build(String id, String coinId, Transaction.Type type, BigDecimal q, BigDecimal p,
                              BigDecimal fee, Instant ts, Transaction existing, Instant createdAt) {
        Instant now = Instant.now();
        Instant when = ts == null ? now : ts;
        if (when.isAfter(now.plus(Duration.ofMinutes(5)))) throw new ApiException(400, "Transaction date cannot be in the future.");
        String symbol, name, cid;
        var coin = market.findCoin(coinId);
        if (coin.isPresent()) {
            cid = coin.get().id(); symbol = coin.get().symbol(); name = coin.get().name();
        } else if (existing != null && existing.coinId().equalsIgnoreCase(coinId)) {
            cid = existing.coinId(); symbol = existing.symbol(); name = existing.name();
        } else {
            throw new ApiException(400, "Unknown or unsupported coin: " + coinId);
        }
        return new Transaction(id, cid, symbol, name, type, q, p, fee == null ? BigDecimal.ZERO : fee, when, createdAt == null ? now : createdAt);
    }

    // ------------------------------------------------------------ portfolio

    public Portfolio portfolio() {
        CoinList cl = market.getCoins();
        Map<String, Coin> byId = cl.coins().stream().collect(Collectors.toMap(Coin::id, Function.identity(), (a, b) -> a));
        Map<String, Position> positions = PortfolioCalculator.compute(txRepo.findAll());

        BigDecimal realizedTotal = positions.values().stream().map(p -> p.realizedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Position> open = positions.values().stream().filter(p -> p.quantity.signum() > 0).toList();

        BigDecimal totalValue = BigDecimal.ZERO, totalInvested = BigDecimal.ZERO, pricedCost = BigDecimal.ZERO;
        int missing = 0;
        for (Position p : open) {
            totalInvested = totalInvested.add(p.costBasis);
            Coin c = byId.get(p.coinId);
            if (c != null && c.price() != null) {
                totalValue = totalValue.add(p.quantity.multiply(c.price()));
                pricedCost = pricedCost.add(p.costBasis);
            } else {
                missing++;
            }
        }
        BigDecimal unrealized = totalValue.subtract(pricedCost);

        List<Holding> holdings = new ArrayList<>();
        for (Position p : open) {
            Coin c = byId.get(p.coinId);
            boolean priced = c != null && c.price() != null;
            BigDecimal value = priced ? p.quantity.multiply(c.price()) : null;
            BigDecimal pnl = priced ? value.subtract(p.costBasis) : null;
            holdings.add(new Holding(p.coinId, p.symbol, p.name, r8(p.quantity), r8(p.avgCost()), r8(p.costBasis),
                    priced ? c.price() : null, r8(value), r8(pnl), pct(pnl, p.costBasis), r8(p.realizedPnl),
                    priced && totalValue.signum() > 0 ? pct(value, totalValue) : null, priced));
        }
        holdings.sort(Comparator.comparing(Holding::currentValue, Comparator.nullsLast(Comparator.<BigDecimal>naturalOrder())).reversed());

        String notice = cl.notice();
        if (missing > 0) {
            String m = missing + " holding(s) have no available price and are excluded from value and profit/loss.";
            notice = notice == null ? m : notice + " " + m;
        }
        PortfolioSummary s = new PortfolioSummary(r8(totalValue), r8(totalInvested), r8(unrealized), pct(unrealized, pricedCost),
                r8(realizedTotal), open.size(), cl.source(), cl.updatedAt(), notice);

        snapshotIfDue(s, missing);
        return new Portfolio(holdings, s);
    }

    private void snapshotIfDue(PortfolioSummary s, int missing) {
        if (!"live".equals(s.source()) || s.holdingsCount() == 0 || missing > 0) return;
        Instant now = Instant.now();
        var last = snapRepo.findFirstByOrderByTimestampDesc();
        if (last.isEmpty() || Duration.between(last.get().timestamp(), now).compareTo(SNAPSHOT_EVERY) >= 0) {
            snapRepo.save(new Snapshot(null, now, s.totalValue(), s.totalInvested()));
        }
    }

    // ------------------------------------------------------------ analytics

    public Analytics analytics() {
        Portfolio pf = portfolio();
        List<Holding> priced = pf.holdings().stream().filter(Holding::priceAvailable).toList();

        List<AllocationItem> allocation = priced.stream()
                .map(h -> new AllocationItem(h.coinId(), h.symbol(), h.currentValue(), h.allocationPct())).toList();
        List<AssetPnl> pnl = pf.holdings().stream()
                .map(h -> new AssetPnl(h.symbol(), h.unrealizedPnl(), h.realizedPnl(), h.returnPct())).toList();

        List<Holding> ranked = priced.stream().filter(h -> h.returnPct() != null)
                .sorted(Comparator.comparing(Holding::returnPct)).toList();
        Performer best = ranked.isEmpty() ? null : new Performer(ranked.get(ranked.size() - 1).symbol(), ranked.get(ranked.size() - 1).returnPct());
        Performer worst = ranked.isEmpty() ? null : new Performer(ranked.get(0).symbol(), ranked.get(0).returnPct());

        Holding top = priced.stream().max(Comparator.comparing(Holding::currentValue)).orElse(null);

        List<Snapshot> snaps = new ArrayList<>(snapRepo.findTop200ByOrderByTimestampDesc());
        snaps.sort(Comparator.comparing(Snapshot::timestamp));
        String note = snaps.size() >= 2 ? null
                : "Not enough history yet. Portfolio value snapshots are recorded from live prices (at most every 15 minutes) while the app is in use. Real history will appear here over time; none is invented.";

        return new Analytics(allocation, pnl, best, worst, top == null ? null : top.symbol(), top == null ? null : top.allocationPct(),
                snaps.size() >= 2 ? snaps : List.of(), note, pf.summary().source(), pf.summary().updatedAt());
    }

    // ------------------------------------------------------------ helpers

    private static BigDecimal r8(BigDecimal v) {
        return v == null ? null : v.setScale(8, RoundingMode.HALF_UP);
    }

    /** part/whole*100 with 4 decimals; null when whole is zero (never divides by zero). */
    private static BigDecimal pct(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() <= 0) return null;
        return part.multiply(HUNDRED).divide(whole, 4, RoundingMode.HALF_UP);
    }
}
