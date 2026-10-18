package com.cryptoportfolio.service;

import com.cryptoportfolio.dto.Dtos.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Rule-based insights are always produced and labelled as such.
 * If GEMINI_API_KEY is set and ai=true, an additional AI-written summary is requested and
 * shown in a separate, clearly labelled field. Insights are educational, not financial advice.
 */
@Service
public class InsightService {
    private static final String DISCLAIMER = "Educational information only, not financial advice. Crypto assets are volatile and "
            + "past performance does not predict future results. Nothing here is a prediction or a promise of returns.";

    private final PortfolioService portfolioService;
    private final String geminiKey;
    private final String geminiModel;
    private final RestClient http;

    public InsightService(PortfolioService portfolioService,
                          @Value("${app.gemini.api-key:}") String geminiKey,
                          @Value("${app.gemini.model:gemini-2.5-flash}") String geminiModel) {
        this.portfolioService = portfolioService;
        this.geminiKey = geminiKey;
        this.geminiModel = geminiModel;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(20000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public boolean aiConfigured() {
        return geminiKey != null && !geminiKey.isBlank();
    }

    public InsightResponse insights(boolean useAi) {
        Portfolio pf = portfolioService.portfolio();
        Analytics an = portfolioService.analytics();
        List<Insight> list = ruleBased(pf, an);

        String aiSummary = null, provider = null, aiNotice = null;
        String mode = "rule-based";
        if (useAi) {
            if (!aiConfigured()) {
                aiNotice = "No AI key configured on the server, showing rule-based insights only.";
            } else if (pf.holdings().isEmpty()) {
                aiNotice = "Add holdings first to generate an AI summary.";
            } else {
                try {
                    aiSummary = callGemini(pf);
                    provider = "Google Gemini (" + geminiModel + ")";
                    mode = "rule-based + AI summary";
                } catch (Exception e) {
                    aiNotice = "AI request failed (" + e.getClass().getSimpleName() + "). Showing rule-based insights only.";
                }
            }
        }
        return new InsightResponse(mode, list, aiSummary, provider, aiNotice, DISCLAIMER,
                pf.summary().source(), Instant.now());
    }

    private List<Insight> ruleBased(Portfolio pf, Analytics an) {
        List<Insight> out = new ArrayList<>();
        PortfolioSummary s = pf.summary();
        if (!"live".equals(s.source())) {
            out.add(new Insight("warning", "Prices are not live",
                    "Current data source is '" + s.source() + "'. Treat all values below as illustrative until live prices return."));
        }
        if (pf.holdings().isEmpty()) {
            out.add(new Insight("info", "No holdings yet",
                    "Record a buy transaction or add a holding to see allocation, concentration and performance insights."));
            return out;
        }
        if (an.topHoldingPct() != null) {
            BigDecimal p = an.topHoldingPct();
            if (p.compareTo(BigDecimal.valueOf(70)) > 0) {
                out.add(new Insight("warning", "Very high concentration",
                        an.topHoldingSymbol() + " is " + p.setScale(1, java.math.RoundingMode.HALF_UP) + "% of your portfolio value. A large move in one asset would dominate your results."));
            } else if (p.compareTo(BigDecimal.valueOf(50)) > 0) {
                out.add(new Insight("warning", "High concentration",
                        an.topHoldingSymbol() + " makes up " + p.setScale(1, java.math.RoundingMode.HALF_UP) + "% of your portfolio. Consider whether that matches your risk tolerance."));
            } else {
                out.add(new Insight("positive", "Allocation looks spread out",
                        "Your largest position (" + an.topHoldingSymbol() + ") is " + p.setScale(1, java.math.RoundingMode.HALF_UP) + "% of value."));
            }
        }
        if (pf.holdings().size() < 3) {
            out.add(new Insight("info", "Few assets held",
                    "You hold " + pf.holdings().size() + " asset(s). Holding more assets can reduce dependence on any single one, although crypto assets often move together."));
        }
        if (an.best() != null && an.worst() != null && !an.best().symbol().equals(an.worst().symbol())) {
            out.add(new Insight("info", "Best and worst performers",
                    "Best: " + an.best().symbol() + " (" + fmt(an.best().returnPct()) + "%). Worst: " + an.worst().symbol() + " (" + fmt(an.worst().returnPct()) + "%), measured against your average cost."));
        }
        if (s.returnPct() != null) {
            String sign = s.returnPct().signum() >= 0 ? "above" : "below";
            out.add(new Insight("info", "Unrealized result",
                    "Your open positions are " + fmt(s.returnPct().abs()) + "% " + sign + " their cost basis. This is unrealized: it only becomes a realized gain or loss if you sell."));
        }
        if (s.realizedPnl() != null && s.realizedPnl().signum() != 0) {
            out.add(new Insight("info", "Realized result",
                    "Closed sales so far have realized " + s.realizedPnl().setScale(2, java.math.RoundingMode.HALF_UP) + " INR (after fees, weighted-average cost)."));
        }
        if (an.performanceNote() != null) {
            out.add(new Insight("info", "Historical performance", an.performanceNote()));
        }
        return out;
    }

    private static String fmt(BigDecimal v) {
        return v == null ? "n/a" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String callGemini(Portfolio pf) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an educational assistant inside a student portfolio-tracker app. ")
          .append("Using ONLY the data below, write a short plain-language summary (max 150 words) covering allocation, ")
          .append("concentration risk and performance versus cost. Do not predict prices, do not promise returns, ")
          .append("do not give buy/sell advice. End with one sentence on risk awareness.\n\nHoldings (INR):\n");
        for (Holding h : pf.holdings()) {
            sb.append("- ").append(h.symbol()).append(": allocation ").append(fmt(h.allocationPct())).append("%, return vs cost ")
              .append(fmt(h.returnPct())).append("%\n");
        }
        sb.append("Total unrealized return: ").append(fmt(pf.summary().returnPct())).append("%\n");
        sb.append("Data source: ").append(pf.summary().source()).append("\n");

        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", sb.toString())))));
        JsonNode res = http.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent")
                .header("x-goog-api-key", geminiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        String text = res == null ? "" : res.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        if (text.isBlank()) throw new IllegalStateException("Empty AI response");
        return text.trim();
    }
}
