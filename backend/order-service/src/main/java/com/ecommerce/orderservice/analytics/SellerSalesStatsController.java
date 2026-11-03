package com.ecommerce.orderservice.analytics;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ecommerce.orderservice.common.ResponseData;

@RestController
@RequestMapping("/profile/seller")
public class SellerSalesStatsController {
    private final SellerSalesStatsService statsService;

    public SellerSalesStatsController(SellerSalesStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseData<SellerSalesStats> getStats(
            @AuthenticationPrincipal String sellerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'from' must be on or before 'to'");
        }
        return ResponseData.success("Seller sales statistics retrieved", statsService.getStats(sellerId, from, to));
    }
}
