package com.cryptoportfolio.repo;

import com.cryptoportfolio.model.Alert;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AlertRepository extends MongoRepository<Alert, String> {
}
