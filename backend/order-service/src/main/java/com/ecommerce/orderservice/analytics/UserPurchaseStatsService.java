package com.ecommerce.orderservice.analytics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.AMOUNT;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.GROUP_STAGE;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.ITEMS_QUANTITY;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.PRODUCT_ID;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.PROJECT_STAGE;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.UNITS;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.bestProducts;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.decimalValue;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.itemAmount;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.numberValue;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.orderSummary;
import static com.ecommerce.orderservice.analytics.AnalyticsMongoSupport.orderMatch;


@Service
public class UserPurchaseStatsService {
    static final String INDEX_NAME = "user_created_at_stats";
    private static final String ORDERS_COLLECTION = "orders";
    private static final String TOTAL_SPENT = "totalSpent";

    private final MongoTemplate mongoTemplate;

    public UserPurchaseStatsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public UserPurchaseStats getStats(String userId, LocalDate from, LocalDate to) {
        List<Document> results = mongoTemplate.getCollection(ORDERS_COLLECTION)
                .aggregate(buildPipeline(userId, from, to))
                .hintString(INDEX_NAME)
                .into(new ArrayList<>());

        if (results.isEmpty()) {
            return UserPurchaseStats.empty();
        }

        Document result = results.get(0);
        List<Document> summary = result.getList("summary", Document.class, List.of());
        if (summary.isEmpty()) {
            return UserPurchaseStats.empty();
        }

        Document totals = summary.get(0);
        return new UserPurchaseStats(
                decimalValue(totals.get(TOTAL_SPENT)),
                numberValue(totals.get("ordersCount")),
                result.getList("mostBought", Document.class, List.of()).stream()
                        .map(this::toMostBought)
                        .toList(),
                result.getList("topCategories", Document.class, List.of()).stream()
                        .map(this::toTopCategory)
                        .toList());
    }

    List<Document> buildPipeline(String userId, LocalDate from, LocalDate to) {
        Document match = orderMatch("userId", userId, from, to);
        Document amount = itemAmount();
        List<Document> summary = orderSummary(TOTAL_SPENT, amount, false);
        List<Document> mostBought = bestProducts(AMOUNT, amount);

        List<Document> topCategories = List.of(
                new Document("$match",
                        new Document("items.category", new Document("$exists", true)
                                .append("$nin", AnalyticsMongoSupport.nonEmptyValues()))),
                new Document(GROUP_STAGE, new Document("_id", "$items.category")
                        .append(UNITS, new Document("$sum", ITEMS_QUANTITY))
                        .append(AMOUNT, new Document("$sum", amount))),
                new Document("$sort", new Document(UNITS, -1).append(AMOUNT, -1).append("_id", 1)),
                new Document("$limit", 5),
                new Document(PROJECT_STAGE, new Document("_id", 0)
                        .append("category", "$_id")
                        .append(UNITS, 1)
                        .append(AMOUNT, 1)));

        return List.of(
                new Document("$match", match),
                new Document("$unwind", "$items"),
                new Document("$facet", new Document("summary", summary)
                        .append("mostBought", mostBought)
                        .append("topCategories", topCategories)));
    }

    private UserPurchaseStats.MostBoughtProduct toMostBought(Document document) {
        return new UserPurchaseStats.MostBoughtProduct(
                document.getString(PRODUCT_ID),
                document.getString("name"),
                numberValue(document.get(UNITS)),
                decimalValue(document.get(AMOUNT)));
    }

    private UserPurchaseStats.TopCategory toTopCategory(Document document) {
        return new UserPurchaseStats.TopCategory(
                document.getString("category"),
                numberValue(document.get(UNITS)),
                decimalValue(document.get(AMOUNT)));
    }

}
