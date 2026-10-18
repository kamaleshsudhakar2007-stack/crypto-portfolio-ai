package com.cryptoportfolio.web;

import com.cryptoportfolio.dto.Dtos.Coin;
import com.cryptoportfolio.dto.Dtos.CoinList;
import com.cryptoportfolio.service.ApiException;
import com.cryptoportfolio.service.MarketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketController {
    private final MarketService market;

    public MarketController(MarketService market) {
        this.market = market;
    }

    @GetMapping("/api/coins")
    public CoinList coins() {
        return market.getCoins();
    }

    @GetMapping("/api/coins/{coinId}")
    public Coin coin(@PathVariable String coinId) {
        return market.findCoin(coinId).orElseThrow(() -> new ApiException(404, "Coin not found: " + coinId));
    }
}
