package com.cryptoportfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.util.*;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * One-time, explicit, reversible conversion of legacy USD records to INR.
 * - On first start of this version we store "inrSwitchAt". Only records created BEFORE it are legacy USD,
 *   so anything you enter in INR afterwards is never touched.
 * - Transactions and snapshots use the USD->INR rate of their own date; alerts use today's rate.
 * - dryRun (default) writes nothing. A real run first copies each collection to <name>_usd_backup.
 * - All rates are fetched BEFORE any write; if one fails nothing changes. Converted docs get fxConverted=true,
 *   so running twice never double-converts.
 */
@Service
public class CurrencyMigrationService {
    private static final Logger log = LoggerFactory.getLogger(CurrencyMigrationService.class);
    private static final String META = "app_meta";

    private final MongoTemplate mongo;
    private final FxRateService fx;

    public CurrencyMigrationService(MongoTemplate mongo, FxRateService fx) {
        this.mongo = mongo;
        this.fx = fx;
    }

    private record Plan(String coll, Object id, Map<String, Object> set, BigDecimal rate) {}

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            if (mongo.findById("currency", Document.class, META) == null) {
                mongo.save(new Document("_id", "currency").append("inrSwitchAt", new Date()), META);
            }
            Date cutoff = cutoff();
            long pending = mongo.count(new Query(legacy("createdAt", cutoff)), "transactions")
                    + mongo.count(new Query(legacy("createdAt", cutoff)), "alerts")
                    + mongo.count(new Query(legacy("timestamp", cutoff)), "snapshots");
            if (pending > 0) {
                log.warn("{} stored record(s) are still in USD. Preview: POST /api/dev/migrate-to-inr?dryRun=true  Apply: ...dryRun=false", pending);
            }
        } catch (Exception e) {
            log.warn("Currency check skipped: {}", e.getMessage());
        }
    }

    public Map<String, Object> migrate(boolean dryRun) {
        Date cutoff = cutoff();
        BigDecimal latest = fx.usdToInr().rate();
        List<Plan> plans = new ArrayList<>();
        plan(plans, "transactions", "createdAt", cutoff, d -> rateFor(d.getDate("timestamp"), latest), "price", "fee");
        plan(plans, "alerts", "createdAt", cutoff, d -> latest, "targetPrice", "triggeredPrice");
        plan(plans, "snapshots", "timestamp", cutoff, d -> rateFor(d.getDate("timestamp"), latest), "totalValue", "totalInvested");

        Map<String, Integer> counts = new LinkedHashMap<>();
        List<Map<String, Object>> samples = new ArrayList<>();
        for (Plan p : plans) {
            counts.merge(p.coll(), 1, Integer::sum);
            if (counts.get(p.coll()) <= 2) samples.add(Map.of("collection", p.coll(), "rate", p.rate(), "newValues", p.set()));
        }
        if (!dryRun) {
            for (String c : counts.keySet()) {
                String backup = c + "_usd_backup";
                if (!mongo.collectionExists(backup)) {
                    List<Document> all = mongo.findAll(Document.class, c);
                    if (!all.isEmpty()) mongo.insert(all, backup);
                }
            }
            for (Plan p : plans) {
                Update u = new Update().set("fxConverted", true).set("fxRate", p.rate().toPlainString()).set("fxFrom", "USD");
                p.set().forEach(u::set);
                mongo.updateFirst(Query.query(Criteria.where("_id").is(p.id())), u, p.coll());
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dryRun", dryRun);
        out.put("recordsToConvert", counts);
        out.put("latestUsdInr", latest);
        out.put("samples", samples);
        out.put("note", dryRun ? "Nothing was written. Re-run with dryRun=false to apply." : "Done. Originals are kept in *_usd_backup collections.");
        return out;
    }

    private interface RateFn { BigDecimal of(Document d); }

    private void plan(List<Plan> out, String coll, String dateField, Date cutoff, RateFn rateFn, String... fields) {
        for (Document d : mongo.find(new Query(legacy(dateField, cutoff)), Document.class, coll)) {
            BigDecimal rate = rateFn.of(d);
            Map<String, Object> set = new LinkedHashMap<>();
            for (String f : fields) {
                Object v = d.get(f);
                if (v != null) set.put(f, convert(v, rate));
            }
            out.add(new Plan(coll, d.get("_id"), set, rate));
        }
    }

    private BigDecimal rateFor(Date when, BigDecimal fallback) {
        return when == null ? fallback : fx.rateOn(when.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
    }

    /** Keeps the stored type (String / Decimal128 / double) so existing reads keep working. */
    private static Object convert(Object orig, BigDecimal rate) {
        BigDecimal out = new BigDecimal(orig.toString()).multiply(rate).setScale(8, RoundingMode.HALF_UP);
        if (orig instanceof String) return out.toPlainString();
        if (orig instanceof Decimal128) return new Decimal128(out);
        return out.doubleValue();
    }

    private Date cutoff() {
        Document m = mongo.findById("currency", Document.class, META);
        if (m == null || m.getDate("inrSwitchAt") == null) throw new ApiException(409, "Restart the backend once, then retry.");
        return m.getDate("inrSwitchAt");
    }

    private static Criteria legacy(String dateField, Date cutoff) {
        return new Criteria().andOperator(
                new Criteria().orOperator(Criteria.where(dateField).lt(cutoff), Criteria.where(dateField).is(null)),
                Criteria.where("fxConverted").ne(true));
    }
}
