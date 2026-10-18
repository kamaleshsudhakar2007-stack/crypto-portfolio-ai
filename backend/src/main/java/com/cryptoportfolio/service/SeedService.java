package com.cryptoportfolio.service;

import com.cryptoportfolio.model.Transaction;
import com.cryptoportfolio.repo.AlertRepository;
import com.cryptoportfolio.repo.SnapshotRepository;
import com.cryptoportfolio.repo.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;

/** Demo helpers: sample transactions (clearly sample data, prices in INR) and full reset. */
@Service
public class SeedService {
    private final TransactionRepository txRepo;
    private final AlertRepository alertRepo;
    private final SnapshotRepository snapRepo;

    public SeedService(TransactionRepository txRepo, AlertRepository alertRepo, SnapshotRepository snapRepo) {
        this.txRepo = txRepo;
        this.alertRepo = alertRepo;
        this.snapRepo = snapRepo;
    }

    public int seed() {
        if (txRepo.count() > 0) throw new ApiException(409, "Transactions already exist. Reset first to load the sample data.");
        Instant now = Instant.now();
        txRepo.save(tx("bitcoin", "BTC", "Bitcoin", Transaction.Type.BUY, "0.5", "3486000", "830", now.minus(90, ChronoUnit.DAYS)));
        txRepo.save(tx("ethereum", "ETH", "Ethereum", Transaction.Type.BUY, "4", "182600", "415", now.minus(80, ChronoUnit.DAYS)));
        txRepo.save(tx("solana", "SOL", "Solana", Transaction.Type.BUY, "30", "7885", "166", now.minus(60, ChronoUnit.DAYS)));
        txRepo.save(tx("bitcoin", "BTC", "Bitcoin", Transaction.Type.BUY, "0.25", "4814000", "664", now.minus(40, ChronoUnit.DAYS)));
        txRepo.save(tx("ethereum", "ETH", "Ethereum", Transaction.Type.SELL, "1", "257300", "415", now.minus(20, ChronoUnit.DAYS)));
        return 5;
    }

    public void reset() {
        txRepo.deleteAll();
        alertRepo.deleteAll();
        snapRepo.deleteAll();
    }

    private static Transaction tx(String id, String sym, String name, Transaction.Type t, String q, String p, String fee, Instant ts) {
        return new Transaction(null, id, sym, name, t, new BigDecimal(q), new BigDecimal(p), new BigDecimal(fee), ts, Instant.now());
    }
}
