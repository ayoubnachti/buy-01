package com.ecommerce.orderservice.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.Decimal128;

final class AnalyticsMongoSupport {
    static final String ITEMS_QUANTITY = "$items.quantity";
    static final String GROUP_STAGE = "$group";
    static final String PROJECT_STAGE = "$project";
    static final String PRODUCT_ID = "productId";
    static final String UNITS = "units";
    static final String AMOUNT = "amount";

    private AnalyticsMongoSupport() {}

    static Document orderMatch(String ownerField, String ownerId, LocalDate from, LocalDate to) {
        Document match = new Document(ownerField, ownerId)
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
        return match;
    }

    static Document itemAmount() {
        return new Document("$multiply", List.of("$items.unitPrice", ITEMS_QUANTITY));
    }

    static List<Document> orderSummary(String totalField, Document amount, boolean includeUnits) {
        Document groupedTotals = new Document(totalField, new Document("$sum", amount));
        if (includeUnits) {
            groupedTotals.append(UNITS, new Document("$sum", ITEMS_QUANTITY));
        }
        groupedTotals.append("orderIds", new Document("$addToSet", "$_id"));

        Document projectedTotals = new Document(totalField, 1)
                .append("ordersCount", new Document("$size", "$orderIds"));
        if (includeUnits) {
            projectedTotals.append(UNITS, 1);
        }

        Document group = new Document("_id", null);
        group.putAll(groupedTotals);
        Document project = new Document("_id", 0);
        project.putAll(projectedTotals);
        return List.of(new Document(GROUP_STAGE, group), new Document(PROJECT_STAGE, project));
    }

    static List<Document> bestProducts(String monetaryField, Document amount) {
        return List.of(
                new Document(GROUP_STAGE, new Document("_id",
                        new Document(PRODUCT_ID, "$items.productId").append("name", "$items.name"))
                        .append(UNITS, new Document("$sum", ITEMS_QUANTITY))
                        .append(monetaryField, new Document("$sum", amount))),
                new Document("$sort",
                        new Document(UNITS, -1).append(monetaryField, -1).append("_id", 1)),
                new Document("$limit", 5),
                new Document(PROJECT_STAGE, new Document("_id", 0)
                        .append(PRODUCT_ID, "$_id.productId")
                        .append("name", "$_id.name")
                        .append(UNITS, 1)
                        .append(monetaryField, 1)));
    }

    static long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Unexpected MongoDB numeric value type");
    }

    static BigDecimal decimalValue(Object value) {
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

    static List<String> nonEmptyValues() {
        return Arrays.asList(null, "");
    }
}
