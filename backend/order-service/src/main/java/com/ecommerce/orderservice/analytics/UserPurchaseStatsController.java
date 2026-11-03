package com.ecommerce.orderservice.analytics;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.orderservice.common.ResponseData;


@RestController
@RequestMapping("/profile/user")
public class UserPurchaseStatsController {
    private final UserPurchaseStatsService statsService;

    public UserPurchaseStatsController(UserPurchaseStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseData<UserPurchaseStats> getStats(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AnalyticsDateRange.validate(from, to);
        return ResponseData.success("User purchase statistics retrieved", statsService.getStats(userId, from, to));
    }
}
