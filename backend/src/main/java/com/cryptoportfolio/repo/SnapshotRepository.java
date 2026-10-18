package com.cryptoportfolio.repo;

import com.cryptoportfolio.model.Snapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SnapshotRepository extends MongoRepository<Snapshot, String> {
    Optional<Snapshot> findFirstByOrderByTimestampDesc();

    List<Snapshot> findTop200ByOrderByTimestampDesc();
}
