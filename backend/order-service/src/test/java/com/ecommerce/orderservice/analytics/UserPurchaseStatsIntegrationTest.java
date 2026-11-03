package com.ecommerce.orderservice.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

import com.ecommerce.orderservice.order.models.OrderStatus;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "eureka.client.fetch-registry=false",
        "eureka.client.register-with-eureka=false"
})
class UserPurchaseStatsIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserPurchaseStatsService statsService;

    @BeforeEach
    void clearOrders() {
        mongoTemplate.remove(new Query(), "orders");
        insertSampleOrders();
    }

    @Test
    void returnsExactTotalsExcludesCancelledIncludesSoftDeletedAndIsolatesUsers() throws Exception {
        mockMvc.perform(get("/profile/user/stats")
                        .header("X-User-Id", "client-a")
                        .header("X-User-Role", "CLIENT")
                        .param("userId", "client-b")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSpent").value(72.22))
                .andExpect(jsonPath("$.data.ordersCount").value(3))
                .andExpect(jsonPath("$.data.mostBought.length()").value(3))
                .andExpect(jsonPath("$.data.mostBought[0].productId").value("p1"))
                .andExpect(jsonPath("$.data.mostBought[0].name").value("Desk lamp"))
                .andExpect(jsonPath("$.data.mostBought[0].units").value(3))
                .andExpect(jsonPath("$.data.mostBought[0].amount").value(59.97))
                .andExpect(jsonPath("$.data.topCategories[0].category").value("HOME"))
                .andExpect(jsonPath("$.data.topCategories[0].units").value(4))
                .andExpect(jsonPath("$.data.topCategories[0].amount").value(67.22));

        mockMvc.perform(get("/profile/user/stats")
                        .header("X-User-Id", "client-b")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSpent").value(999))
                .andExpect(jsonPath("$.data.ordersCount").value(1));
    }

    @Test
    void limitsMostBoughtProductsAndCategoriesToFive() throws Exception {
        insertOrder("order-c1", "client-c", OrderStatus.PENDING, "2026-03-20T12:00:00Z", false,
                item("p1", "Product 1", "HOME", "2.00", 6),
                item("p2", "Product 2", "CLOTHING", "2.00", 5),
                item("p3", "Product 3", "BOOKS", "2.00", 4),
                item("p4", "Product 4", "SPORTS", "2.00", 3),
                item("p5", "Product 5", "ELECTRONICS", "2.00", 2),
                item("p6", "Product 6", "OTHER", "2.00", 1));

        mockMvc.perform(get("/profile/user/stats")
                        .header("X-User-Id", "client-c")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mostBought.length()").value(5))
                .andExpect(jsonPath("$.data.mostBought[0].productId").value("p1"))
                .andExpect(jsonPath("$.data.topCategories.length()").value(5))
                .andExpect(jsonPath("$.data.topCategories[0].category").value("HOME"));
    }

    @Test
    void appliesInclusiveDateRangeAndReturnsEmptyStatsForUserWithoutOrders() throws Exception {
        mockMvc.perform(get("/profile/user/stats")
                        .header("X-User-Id", "client-a")
                        .header("X-User-Role", "CLIENT")
                        .param("from", "2026-03-16")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSpent").value(24.99))
                .andExpect(jsonPath("$.data.ordersCount").value(1));

        mockMvc.perform(get("/profile/user/stats")
                        .header("X-User-Id", "client-no-orders")
                        .header("X-User-Role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSpent").value(0))
                .andExpect(jsonPath("$.data.ordersCount").value(0))
                .andExpect(jsonPath("$.data.mostBought.length()").value(0))
                .andExpect(jsonPath("$.data.topCategories.length()").value(0));
    }

    @Test
    void rejectsStatsRequestsFromNonClientsAndInvalidDateRanges() throws Exception {
        mockMvc.perform(get("/profile/user/stats")
                        .header("X-User-Id", "seller-a")
                        .header("X-User-Role", "SELLER"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/profile/user/stats")
                        .header("X-User-Id", "client-a")
                        .header("X-User-Role", "CLIENT")
                        .param("from", "2026-04-01")
                        .param("to", "2026-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void explainUsesTheUserAndDateIndexForTheInitialMatch() {
        Document explain = mongoTemplate.getCollection("orders")
                .aggregate(statsService.buildPipeline("client-a", null, null))
                .hintString(UserPurchaseStatsService.INDEX_NAME)
                .explain();
        String explainPlan = explain.toJson();

        assertThat(explainPlan).contains("IXSCAN", UserPurchaseStatsService.INDEX_NAME);
    }

    private void insertSampleOrders() {
        insertOrder("order-a1", "client-a", OrderStatus.PENDING, "2026-03-15T10:00:00Z", false,
                item("p1", "Desk lamp", "HOME", "19.99", 2));
        insertOrder("order-a2", "client-a", OrderStatus.DELIVERED, "2026-03-31T23:59:59Z", true,
                item("p1", "Desk lamp", "HOME", "19.99", 1),
                item("p2", "Notebook", "BOOKS", "5.00", 1));
        insertOrder("order-a3", "client-a", OrderStatus.CANCELLED, "2026-03-20T12:00:00Z", false,
                item("p4", "Cancelled item", "OTHER", "100.00", 2));
        insertOrder("order-a4", "client-a", OrderStatus.PENDING, "2026-04-01T00:00:00Z", false,
                item("p3", "Reading lamp", "HOME", "7.25", 1));
        insertOrder("order-b1", "client-b", OrderStatus.DELIVERED, "2026-03-20T12:00:00Z", false,
                item("p5", "Other user's item", "SPORTS", "999.00", 1));
    }

    private void insertOrder(
            String id,
            String userId,
            OrderStatus status,
            String createdAt,
            boolean deletedByUser,
            Document... items) {
        mongoTemplate.getCollection("orders").insertOne(new Document("_id", id)
                .append("userId", userId)
                .append("status", status.name())
                .append("createdAt", Date.from(Instant.parse(createdAt)))
                .append("deletedByUser", deletedByUser)
                .append("items", List.of(items)));
    }

    private Document item(String id, String name, String category, String price, int quantity) {
        return new Document("productId", id)
                .append("name", name)
                .append("category", category)
                .append("unitPrice", new Decimal128(new java.math.BigDecimal(price)))
                .append("quantity", quantity)
                .append("imageUrl", null);
    }
}
