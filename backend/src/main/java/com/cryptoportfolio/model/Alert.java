package com.cryptoportfolio.model;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("alerts")
public record Alert(
        @Id String id,
        String coinId,
        String symbol,
        Direction direction,
        BigDecimal targetPrice,
        Instant createdAt,
        Instant triggeredAt,
        BigDecimal triggeredPrice) {

    public enum Direction { ABOVE, BELOW }
}
