package com.cryptoportfolio.service;

import com.cryptoportfolio.dto.Dtos.AlertList;
import com.cryptoportfolio.dto.Dtos.Coin;
import com.cryptoportfolio.dto.Dtos.CoinList;
import com.cryptoportfolio.dto.Dtos.AlertRequest;
import com.cryptoportfolio.model.Alert;
import com.cryptoportfolio.repo.AlertRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Alerts are evaluated whenever the list is requested (the UI polls it while open).
 * There is no background scheduler, email or push delivery: that would need extra infrastructure.
 * Only LIVE prices are used for evaluation; demo/stale data never triggers an alert.
 */
@Service
public class AlertService {
    private final AlertRepository repo;
    private final MarketService market;

    public AlertService(AlertRepository repo, MarketService market) {
        this.repo = repo;
        this.market = market;
    }

    public Alert create(AlertRequest r) {
        Coin c = market.findCoin(r.coinId()).orElseThrow(() -> new ApiException(400, "Unknown or unsupported coin: " + r.coinId()));
        return repo.save(new Alert(null, c.id(), c.symbol(), r.direction(), r.targetPrice(), Instant.now(), null, null));
    }

    public void delete(String id) {
        if (!repo.existsById(id)) throw new ApiException(404, "Alert not found.");
        repo.deleteById(id);
    }

    public AlertList listAndEvaluate() {
        CoinList cl = market.getCoins();
        boolean live = "live".equals(cl.source());
        List<Alert> out = new ArrayList<>();
        for (Alert a : repo.findAll()) {
            if (live && a.triggeredAt() == null) {
            String coinId = a.coinId();
Coin c = cl.coins().stream().filter(x -> x.id().equals(coinId)).findFirst().orElse(null);
                if (c != null && c.price() != null) {
                    int cmp = c.price().compareTo(a.targetPrice());
                    boolean hit = a.direction() == Alert.Direction.ABOVE ? cmp >= 0 : cmp <= 0;
                    if (hit) {
                        a = repo.save(new Alert(a.id(), a.coinId(), a.symbol(), a.direction(), a.targetPrice(),
                                a.createdAt(), Instant.now(), c.price()));
                    }
                }
            }
            out.add(a);
        }
        out.sort(Comparator.comparing(Alert::createdAt, Comparator.nullsLast(Comparator.<Instant>naturalOrder())).reversed());
        String notice = live ? null : "Alerts are only evaluated against live prices; current data is " + cl.source() + ".";
        return new AlertList(out, cl.source(), cl.updatedAt(), notice);
    }
}
