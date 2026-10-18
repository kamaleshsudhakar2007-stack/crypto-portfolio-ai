package com.cryptoportfolio.web;

import com.cryptoportfolio.dto.Dtos.AlertList;
import com.cryptoportfolio.dto.Dtos.AlertRequest;
import com.cryptoportfolio.model.Alert;
import com.cryptoportfolio.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public AlertList list() {
        return service.listAndEvaluate();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Alert create(@Valid @RequestBody AlertRequest r) {
        return service.create(r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
