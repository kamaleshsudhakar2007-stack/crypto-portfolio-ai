package com.cryptoportfolio.model;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Portfolio value recorded from LIVE prices only. Used for the performance chart. */
@Document("snapshots")
public record Snapshot(
        @Id String id,
        Instant timestamp,
        BigDecimal totalValue,
        BigDecimal totalInvested) {
}
