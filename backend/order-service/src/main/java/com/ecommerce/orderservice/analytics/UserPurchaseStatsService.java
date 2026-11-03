package com.ecommerce.orderservice.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;


@Service
public class UserPurchaseStatsService {
    static final String INDEX_NAME = "user_created_at_stats";
    private static final String ORDERS_COLLECTION = "orders";

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
                decimalValue(totals.get("totalSpent")),
                numberValue(totals.get("ordersCount")),
                result.getList("mostBought", Document.class, List.of()).stream()
                        .map(this::toMostBought)
                        .toList(),
                result.getList("topCategories", Document.class, List.of()).stream()
                        .map(this::toTopCategory)
                        .toList());
    }

    List<Document> buildPipeline(String userId, LocalDate from, LocalDate to) {
        Document match = new Document("userId", userId)
                .append("status", new Document("$ne", "CANCELLED"));

        if (from != null || to != null) {
            Document dateRange = new Document();
            if (from != null) {
                dateRange.append("$gte", Date.from(from.atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
            if (to != null) {
                dateRange.append("$lt",
                        Date.from(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
            match.append("createdAt", dateRange);
        }

        Document amount = new Document("$multiply", List.of("$items.unitPrice", "$items.quantity"));
        List<Document> summary = List.of(
                new Document("$group", new Document("_id", null)
                        .append("totalSpent", new Document("$sum", amount))
                        .append("orderIds", new Document("$addToSet", "$_id"))),
                new Document("$project", new Document("_id", 0)
                        .append("totalSpent", 1)
                        .append("ordersCount", new Document("$size", "$orderIds"))));

        List<Document> mostBought = List.of(
                new Document("$group", new Document("_id",
                        new Document("productId", "$items.productId").append("name", "$items.name"))
                        .append("units", new Document("$sum", "$items.quantity"))
                        .append("amount", new Document("$sum", amount))),
                new Document("$sort", new Document("units", -1).append("amount", -1).append("_id", 1)),
                new Document("$limit", 5),
                new Document("$project", new Document("_id", 0)
                        .append("productId", "$_id.productId")
                        .append("name", "$_id.name")
                        .append("units", 1)
                        .append("amount", 1)));

        List<Document> topCategories = List.of(
                new Document("$match",
                        new Document("items.category", new Document("$exists", true)
                                .append("$nin", Arrays.asList(null, "")))),
                new Document("$group", new Document("_id", "$items.category")
                        .append("units", new Document("$sum", "$items.quantity"))
                        .append("amount", new Document("$sum", amount))),
                new Document("$sort", new Document("units", -1).append("amount", -1).append("_id", 1)),
                new Document("$limit", 5),
                new Document("$project", new Document("_id", 0)
                        .append("category", "$_id")
                        .append("units", 1)
                        .append("amount", 1)));

        return List.of(
                new Document("$match", match),
                new Document("$unwind", "$items"),
                new Document("$facet", new Document("summary", summary)
                        .append("mostBought", mostBought)
                        .append("topCategories", topCategories)));
    }

    private UserPurchaseStats.MostBoughtProduct toMostBought(Document document) {
        return new UserPurchaseStats.MostBoughtProduct(
                document.getString("productId"),
                document.getString("name"),
                numberValue(document.get("units")),
                decimalValue(document.get("amount")));
    }

    private UserPurchaseStats.TopCategory toTopCategory(Document document) {
        return new UserPurchaseStats.TopCategory(
                document.getString("category"),
                numberValue(document.get("units")),
                decimalValue(document.get("amount")));
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Unexpected MongoDB numeric value type");
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Decimal128 decimal128) {
            return decimal128.bigDecimalValue();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        throw new IllegalStateException("Unexpected MongoDB amount value type");
    }
}
