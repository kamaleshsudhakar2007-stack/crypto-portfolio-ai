package com.cryptoportfolio.web;

import com.cryptoportfolio.dto.Dtos.TransactionRequest;
import com.cryptoportfolio.model.Transaction;
import com.cryptoportfolio.service.PortfolioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final PortfolioService service;

    public TransactionController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transaction> list() {
        return service.listTransactions();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction create(@Valid @RequestBody TransactionRequest r) {
        return service.addTransaction(r);
    }

    @PutMapping("/{id}")
    public Transaction update(@PathVariable String id, @Valid @RequestBody TransactionRequest r) {
        return service.updateTransaction(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.deleteTransaction(id);
    }
}
