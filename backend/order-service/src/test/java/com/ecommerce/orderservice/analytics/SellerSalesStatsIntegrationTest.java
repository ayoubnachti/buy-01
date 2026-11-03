package com.ecommerce.orderservice.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "eureka.client.fetch-registry=false",
        "eureka.client.register-with-eureka=false"
})
class SellerSalesStatsIntegrationTest {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SellerSalesStatsService statsService;

    private LocalDate today;
    private LocalDate yesterday;

    @BeforeEach
    void clearOrders() {
        mongoTemplate.remove(new Query(), "orders");
        today = LocalDate.now(ZoneOffset.UTC);
        yesterday = today.minusDays(1);
        insertSampleOrders();
    }

    @Test
    void returnsExactSellerTotalsExcludesCancelledAndOnlyCountsThisSellersOrders() throws Exception {
        mockMvc.perform(get("/profile/seller/stats")
                        .header("X-User-Id", "seller-a")
                        .header("X-User-Role", "SELLER")
                        .param("sellerId", "seller-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue").value(54.49))
                .andExpect(jsonPath("$.data.unitsSold").value(6))
                .andExpect(jsonPath("$.data.ordersCount").value(3))
                .andExpect(jsonPath("$.data.bestSellers[0].productId").value("p2"))
                .andExpect(jsonPath("$.data.bestSellers[0].name").value("Notebook"))
                .andExpect(jsonPath("$.data.bestSellers[0].units").value(3))
                .andExpect(jsonPath("$.data.bestSellers[0].revenue").value(13.50))
                .andExpect(jsonPath("$.data.revenueByDay.length()").value(30))
                .andExpect(jsonPath("$.data.revenueByDay[28].date").value(DATE_FORMAT.format(yesterday)))
                .andExpect(jsonPath("$.data.revenueByDay[28].revenue").value(13.50))
                .andExpect(jsonPath("$.data.revenueByDay[29].date").value(DATE_FORMAT.format(today)))
                .andExpect(jsonPath("$.data.revenueByDay[29].revenue").value(39.98));

        mockMvc.perform(get("/profile/seller/stats")
                        .header("X-User-Id", "seller-b")
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue").value(500))
                .andExpect(jsonPath("$.data.unitsSold").value(1))
                .andExpect(jsonPath("$.data.ordersCount").value(1));
    }

    @Test
    void clientCannotReadSellerStatsAndSellerWithNoSalesGetsEmptyTotalsAndZeroDays() throws Exception {
        mockMvc.perform(get("/profile/seller/stats")
                        .header("X-User-Id", "client-a")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/profile/seller/stats")
                        .header("X-User-Id", "seller-no-sales")
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue").value(0))
                .andExpect(jsonPath("$.data.unitsSold").value(0))
                .andExpect(jsonPath("$.data.ordersCount").value(0))
                .andExpect(jsonPath("$.data.bestSellers.length()").value(0))
                .andExpect(jsonPath("$.data.revenueByDay.length()").value(30))
                .andExpect(jsonPath("$.data.revenueByDay[0].revenue").value(0));
    }

    @Test
    void optionalDateRangeFiltersTotalsAndRejectsReversedDates() throws Exception {
        mockMvc.perform(get("/profile/seller/stats")
                        .header("X-User-Id", "seller-a")
                        .header("X-User-Role", "SELLER")
                        .param("from", DATE_FORMAT.format(yesterday))
                        .param("to", DATE_FORMAT.format(today)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue").value(53.48))
                .andExpect(jsonPath("$.data.unitsSold").value(5))
                .andExpect(jsonPath("$.data.ordersCount").value(2));

        mockMvc.perform(get("/profile/seller/stats")
                        .header("X-User-Id", "seller-a")
                        .header("X-User-Role", "SELLER")
                        .param("from", DATE_FORMAT.format(today))
                        .param("to", DATE_FORMAT.format(yesterday)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void explainUsesSellerAndDateIndexForTheInitialMatch() {
        LocalDate firstDailyDate = today.minusDays(29);
        Document explain = mongoTemplate.getCollection("orders")
                .aggregate(statsService.buildPipeline("seller-a", null, null, firstDailyDate, today))
                .hintString(SellerSalesStatsService.INDEX_NAME)
                .explain();

        assertThat(explain.toJson()).contains("IXSCAN", SellerSalesStatsService.INDEX_NAME);
    }

    private void insertSampleOrders() {
        insertOrder("seller-a-today", "seller-a", "PENDING", today, item("p1", "Desk lamp", "19.99", 2));
        insertOrder("seller-a-yesterday", "seller-a", "DELIVERED", yesterday,
                item("p2", "Notebook", "4.50", 3));
        insertOrder("seller-a-old", "seller-a", "SHIPPED", today.minusDays(31),
                item("p3", "Old item", "1.01", 1));
        insertOrder("seller-a-cancelled", "seller-a", "CANCELLED", today,
                item("cancelled", "Cancelled item", "99.99", 5));
        insertOrder("seller-b-today", "seller-b", "DELIVERED", today,
                item("other-seller-item", "Other seller item", "500.00", 1));
    }

    private void insertOrder(String id, String sellerId, String status, LocalDate createdDate, Document... items) {
        Instant createdAt = createdDate.atTime(12, 0).toInstant(ZoneOffset.UTC);
        mongoTemplate.getCollection("orders").insertOne(new Document("_id", id)
                .append("sellerId", sellerId)
                .append("userId", "client-" + id)
                .append("status", status)
                .append("createdAt", Date.from(createdAt))
                .append("items", List.of(items)));
    }

    private Document item(String productId, String name, String price, int quantity) {
        return new Document("productId", productId)
                .append("name", name)
                .append("unitPrice", new Decimal128(new java.math.BigDecimal(price)))
                .append("quantity", quantity);
    }
}
