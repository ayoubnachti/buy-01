package com.ecommerce.orderservice.analytics;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AnalyticsDateRange {
    private AnalyticsDateRange() {}

    static void validate(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be on or before 'to'");
        }
    }
}
