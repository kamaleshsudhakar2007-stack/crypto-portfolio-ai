package com.cryptoportfolio.web;

import com.cryptoportfolio.dto.Dtos.ErrorResponse;
import com.cryptoportfolio.service.ApiException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Every error leaves the API in the same JSON shape: {timestamp,status,error,message,details}. */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> api(ApiException e) {
        return build(e.getStatus(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        Map<String, String> details = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> details.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return build(400, "Validation failed. Check the highlighted fields.", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        return build(400, "Malformed request body (check numbers, dates and enum values).", Map.of());
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErrorResponse> db(DataAccessException e) {
        log.error("Database error", e);
        return build(503, "Database is unavailable. Make sure MongoDB is running and MONGODB_URI is correct.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> other(Exception e) {
        log.error("Unhandled error", e);
        return build(500, "Unexpected server error.", Map.of());
    }

    private ResponseEntity<ErrorResponse> build(int status, String message, Map<String, String> details) {
        String reason = HttpStatus.valueOf(status).getReasonPhrase();
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status, reason, message, details));
    }
}
