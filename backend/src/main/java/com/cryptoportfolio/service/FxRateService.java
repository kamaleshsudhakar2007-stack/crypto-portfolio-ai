package com.cryptoportfolio.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * USD->INR rates (Frankfurter, ECB reference rates, no key). Used ONLY by the one-time migration of
 * legacy USD records. Live market prices are requested in INR directly and are never converted.
 * Latest rate is cached 1h; if the API fails the last good rate is returned flagged stale; if none, 503.
 */
@Service
public class FxRateService {
    public record FxRate(BigDecimal rate, Instant updatedAt, boolean stale) {}

    private static final String BASE = "https://api.frankfurter.app/";
    private static final Duration TTL = Duration.ofHours(1);
    private final RestClient http;
    private final Map<LocalDate, BigDecimal> history = new ConcurrentHashMap<>();
    private FxRate cached;

    public FxRateService() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(8000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public synchronized FxRate usdToInr() {
        if (cached != null && Duration.between(cached.updatedAt(), Instant.now()).compareTo(TTL) < 0) return cached;
        try {
            cached = new FxRate(fetch(BASE + "latest?from=USD&to=INR"), Instant.now(), false);
            return cached;
        } catch (Exception e) {
            if (cached != null) return new FxRate(cached.rate(), cached.updatedAt(), true);
            throw new ApiException(503, "INR exchange rate is unavailable right now. Nothing was changed.");
        }
    }

    /** Rate for a calendar day (weekends/holidays resolve to the previous published rate). */
    public BigDecimal rateOn(LocalDate day) {
        BigDecimal r = history.get(day);
        if (r == null) {
            try {
                r = fetch(BASE + day + "?from=USD&to=INR");
            } catch (Exception e) {
                throw new ApiException(503, "Historical INR rate for " + day + " is unavailable. Nothing was changed.");
            }
            history.put(day, r);
        }
        return r;
    }

    private BigDecimal fetch(String url) {
        JsonNode n = http.get().uri(url).header("Accept", "application/json").retrieve().body(JsonNode.class);
        JsonNode inr = n == null ? null : n.path("rates").path("INR");
        if (inr == null || !inr.isNumber() || inr.decimalValue().signum() <= 0) throw new IllegalStateException("bad FX payload");
        return inr.decimalValue();
    }
}
