package com.cryptoportfolio.web;

import com.cryptoportfolio.service.CurrencyMigrationService;
import com.cryptoportfolio.service.SeedService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/** Demo helpers. Disable with app.dev-endpoints=false. */
@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "app.dev-endpoints", havingValue = "true", matchIfMissing = true)
public class DevController {
    private final SeedService seed;
    private final CurrencyMigrationService migration;

    public DevController(SeedService seed, CurrencyMigrationService migration) {
        this.seed = seed;
        this.migration = migration;
    }

    /** Converts legacy USD records to INR. Default is a dry run that writes nothing. */
    @PostMapping("/migrate-to-inr")
    public Map<String, Object> migrate(@RequestParam(defaultValue = "true") boolean dryRun) {
        return migration.migrate(dryRun);
    }

    @PostMapping("/seed")
    public Map<String, Object> seed() {
        return Map.of("inserted", seed.seed());
    }

    @DeleteMapping("/reset")
    public Map<String, Object> reset() {
        seed.reset();
        return Map.of("reset", true);
    }
}
