package com.cryptoportfolio.repo;

import com.cryptoportfolio.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    long deleteByCoinId(String coinId);
}
