package com.cryptoportfolio.web;

import com.cryptoportfolio.service.InsightService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final MongoTemplate mongo;
    private final InsightService insights;

    public HealthController(MongoTemplate mongo, InsightService insights) {
        this.mongo = mongo;
        this.insights = insights;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        boolean dbUp;
        try {
            mongo.getDb().runCommand(new Document("ping", 1));
            dbUp = true;
        } catch (Exception e) {
            dbUp = false;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", dbUp ? "UP" : "DEGRADED");
        m.put("database", dbUp ? "UP" : "DOWN");
        m.put("aiConfigured", insights.aiConfigured());
        m.put("time", Instant.now());
        return m;
    }
}
