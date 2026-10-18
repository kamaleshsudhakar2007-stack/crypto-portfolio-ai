package com.cryptoportfolio.web;

import com.cryptoportfolio.dto.Dtos.*;
import com.cryptoportfolio.model.Transaction;
import com.cryptoportfolio.service.InsightService;
import com.cryptoportfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PortfolioController {
    private final PortfolioService service;
    private final InsightService insights;

    public PortfolioController(PortfolioService service, InsightService insights) {
        this.service = service;
        this.insights = insights;
    }

    @GetMapping("/portfolio")
    public Portfolio portfolio() {
        return service.portfolio();
    }

    @GetMapping("/portfolio/summary")
    public PortfolioSummary summary() {
        return service.portfolio().summary();
    }

    /** Adds a holding by recording an initial BUY transaction. */
    @PostMapping("/holdings")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction addHolding(@Valid @RequestBody HoldingRequest r) {
        return service.addHolding(r);
    }

    /** Removes the whole position (all transactions of this coin). Use DELETE /api/transactions/{id} for one record. */
    @DeleteMapping("/holdings/{coinId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHolding(@PathVariable String coinId) {
        service.deleteHolding(coinId);
    }

    @GetMapping("/analytics")
    public Analytics analytics() {
        return service.analytics();
    }

    @GetMapping("/insights")
    public InsightResponse insights(@RequestParam(defaultValue = "false") boolean ai) {
        return insights.insights(ai);
    }
}
