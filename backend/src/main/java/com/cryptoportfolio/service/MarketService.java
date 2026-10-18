package com.cryptoportfolio.service;

import com.cryptoportfolio.dto.Dtos.Coin;
import com.cryptoportfolio.dto.Dtos.CoinList;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Market data from CoinGecko, requested directly in INR (vs_currency=inr), so prices arrive as INR and are
 * NEVER converted again anywhere in this app. Cached for 60s with a 60s cache, a 30s back-off after failures
 * (rate-limit friendly), stale-data fallback and a clearly labelled demo fallback.
 */
@Service
public class MarketService {
    private static final Logger log = LoggerFactory.getLogger(MarketService.class);
    private static final String URL = "https://api.coingecko.com/api/v3/coins/markets"
            + "?vs_currency=inr&order=market_cap_desc&per_page=50&page=1&sparkline=false&price_change_percentage=24h";
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final Duration BACKOFF = Duration.ofSeconds(30);

    private final RestClient http;
    private final String apiKey;

    private CoinList lastLive;
    private Instant lastFetch = Instant.EPOCH;
    private Instant lastFailure = Instant.EPOCH;

    public MarketService(@Value("${app.coingecko.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(8000);
        this.http = RestClient.builder().requestFactory(f).build();
        this.apiKey = apiKey;
    }

    public synchronized CoinList getCoins() {
        Instant now = Instant.now();
        if (lastLive != null && Duration.between(lastFetch, now).compareTo(TTL) < 0) {
            return lastLive;
        }
        boolean backingOff = Duration.between(lastFailure, now).compareTo(BACKOFF) < 0;
        if (!backingOff) {
            try {
                lastLive = new CoinList(fetchLive(), "live", now, null);
                lastFetch = now;
                return lastLive;
            } catch (Exception e) {
                lastFailure = now;
                log.warn("CoinGecko request failed: {}", e.getMessage());
            }
        }
        if (lastLive != null) {
            return new CoinList(lastLive.coins(), "stale", lastLive.updatedAt(),
                    "Live market API is unavailable (rate limit or network). Showing the last successful live data.");
        }
        return new CoinList(demoCoins(), "demo", now,
                "DEMO DATA: live market API is unavailable. Prices below are static sample values, not real market prices.");
    }

    public Optional<Coin> findCoin(String coinId) {
        return getCoins().coins().stream().filter(c -> c.id().equalsIgnoreCase(coinId)).findFirst();
    }

    private List<Coin> fetchLive() {
        var req = http.get().uri(URL).header("User-Agent", "CryptoPortfolioAI/1.0").header("Accept", "application/json");
        if (apiKey != null && !apiKey.isBlank()) {
            req = req.header("x-cg-demo-api-key", apiKey);
        }
        JsonNode arr = req.retrieve().body(JsonNode.class);
        if (arr == null || !arr.isArray() || arr.isEmpty()) {
            throw new IllegalStateException("Empty response from market API");
        }
        List<Coin> out = new ArrayList<>();
        for (JsonNode n : arr) {
            out.add(new Coin(n.path("id").asText(), n.path("symbol").asText().toUpperCase(), n.path("name").asText(),
                    dec(n, "current_price"), dec(n, "price_change_percentage_24h"), dec(n, "market_cap"),
                    n.path("image").isNull() ? null : n.path("image").asText(null)));
        }
        return out;
    }

    private static BigDecimal dec(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return (v.isMissingNode() || v.isNull()) ? null : v.decimalValue();
    }

    /** Static sample data in INR (rough values x83), only used when no live data has ever been fetched. */
    private static List<Coin> demoCoins() {
        return List.of(
                demo("bitcoin", "BTC", "Bitcoin", "5395000", "1.2", "106240000000000"),
                demo("ethereum", "ETH", "Ethereum", "265600", "-0.8", "31955000000000"),
                demo("tether", "USDT", "Tether", "83", "0.0", "9130000000000"),
                demo("binancecoin", "BNB", "BNB", "48970", "0.5", "7138000000000"),
                demo("solana", "SOL", "Solana", "12450", "2.1", "5810000000000"),
                demo("ripple", "XRP", "XRP", "45.65", "-1.4", "2573000000000"),
                demo("cardano", "ADA", "Cardano", "37.35", "0.3", "1328000000000"),
                demo("dogecoin", "DOGE", "Dogecoin", "9.96", "3.0", "1411000000000"),
                demo("polkadot", "DOT", "Polkadot", "539.5", "-0.2", "747000000000"),
                demo("chainlink", "LINK", "Chainlink", "1162", "1.0", "705500000000"));
    }

    private static Coin demo(String id, String sym, String name, String price, String chg, String cap) {
        return new Coin(id, sym, name, new BigDecimal(price), new BigDecimal(chg), new BigDecimal(cap), null);
    }
}
