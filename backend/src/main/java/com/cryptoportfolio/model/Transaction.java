package com.cryptoportfolio.model;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** A single BUY or SELL. Holdings are always derived from these records. */
@Document("transactions")
public record Transaction(
        @Id String id,
        String coinId,
        String symbol,
        String name,
        Type type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        Instant timestamp,
        Instant createdAt) {

    public enum Type { BUY, SELL }
}
