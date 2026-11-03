package com.ecommerce.orderservice.analytics;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class SellerSalesStatsService {
    static final String INDEX_NAME = "seller_created_at_stats";
    private static final String ORDERS_COLLECTION = "orders";
    private static final String ITEMS_QUANTITY = "$items.quantity";
    private static final String GROUP_STAGE = "$group";
    private static final String PROJECT_STAGE = "$project";
    private static final String REVENUE = "revenue";
    private static final String UNITS = "units";
    private static final String PRODUCT_ID = "productId";
    private static final int DAILY_WINDOW_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MongoTemplate mongoTemplate;
    private final Clock clock;

    @Autowired
    public SellerSalesStatsService(MongoTemplate mongoTemplate) {
        this(mongoTemplate, Clock.systemUTC());
    }

    SellerSalesStatsService(MongoTemplate mongoTemplate, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.clock = clock;
    }

    public SellerSalesStats getStats(String sellerId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate firstDailyDate = today.minusDays(DAILY_WINDOW_DAYS - 1L);
        List<Document> results = mongoTemplate.getCollection(ORDERS_COLLECTION)
                .aggregate(buildPipeline(sellerId, from, to, firstDailyDate, today))
                .hintString(INDEX_NAME)
                .into(new ArrayList<>());

        List<SellerSalesStats.RevenueByDay> dailyRevenue = fillDailyRevenue(
                firstDailyDate, today, results.isEmpty()
                        ? List.of()
                        : results.get(0).getList("revenueByDay", Document.class, List.of()));
        if (results.isEmpty()) {
            return SellerSalesStats.empty(dailyRevenue);
        }

        Document result = results.get(0);
        List<Document> summary = result.getList("summary", Document.class, List.of());
        if (summary.isEmpty()) {
            return SellerSalesStats.empty(dailyRevenue);
        }

        Document totals = summary.get(0);
        return new SellerSalesStats(
                decimalValue(totals.get(REVENUE)),
                numberValue(totals.get(UNITS)),
                numberValue(totals.get("ordersCount")),
                result.getList("bestSellers", Document.class, List.of()).stream()
                        .map(this::toBestSeller)
                        .toList(),
                dailyRevenue);
    }

    List<Document> buildPipeline(
            String sellerId, LocalDate from, LocalDate to, LocalDate firstDailyDate, LocalDate today) {
        Document match = new Document("sellerId", sellerId)
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

        Document amount = new Document("$multiply", List.of("$items.unitPrice", ITEMS_QUANTITY));
        List<Document> summary = List.of(
                new Document(GROUP_STAGE, new Document("_id", null)
                        .append(REVENUE, new Document("$sum", amount))
                        .append(UNITS, new Document("$sum", ITEMS_QUANTITY))
                        .append("orderIds", new Document("$addToSet", "$_id"))),
                new Document(PROJECT_STAGE, new Document("_id", 0)
                        .append(REVENUE, 1)
                        .append(UNITS, 1)
                        .append("ordersCount", new Document("$size", "$orderIds"))));

        List<Document> bestSellers = List.of(
                new Document(GROUP_STAGE, new Document("_id",
                        new Document(PRODUCT_ID, "$items.productId").append("name", "$items.name"))
                        .append(UNITS, new Document("$sum", ITEMS_QUANTITY))
                        .append(REVENUE, new Document("$sum", amount))),
                new Document("$sort",
                        new Document(UNITS, -1).append(REVENUE, -1).append("_id", 1)),
                new Document("$limit", 5),
                new Document(PROJECT_STAGE, new Document("_id", 0)
                        .append(PRODUCT_ID, "$_id.productId")
                        .append("name", "$_id.name")
                        .append(UNITS, 1)
                        .append(REVENUE, 1)));

        Document dailyMatch = new Document("createdAt", new Document("$gte",
                Date.from(firstDailyDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
                .append("$lt", Date.from(today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())));
        List<Document> revenueByDay = List.of(
                new Document("$match", dailyMatch),
                new Document(GROUP_STAGE, new Document("_id", new Document("$dateToString",
                        new Document("format", "%Y-%m-%d")
                                .append("date", "$createdAt")
                                .append("timezone", "UTC")))
                        .append(REVENUE, new Document("$sum", amount))),
                new Document("$sort", new Document("_id", 1)),
                new Document(PROJECT_STAGE, new Document("_id", 0)
                        .append("date", "$_id")
                        .append(REVENUE, 1)));

        return List.of(
                new Document("$match", match),
                new Document("$unwind", "$items"),
                new Document("$facet", new Document("summary", summary)
                        .append("bestSellers", bestSellers)
                        .append("revenueByDay", revenueByDay)));
    }

    private List<SellerSalesStats.RevenueByDay> fillDailyRevenue(
            LocalDate firstDate, LocalDate lastDate, List<Document> aggregatedDays) {
        Map<String, BigDecimal> amountsByDate = aggregatedDays.stream()
                .collect(Collectors.toMap(
                        day -> day.getString("date"),
                        day -> decimalValue(day.get(REVENUE))));

        return Stream.iterate(firstDate, date -> !date.isAfter(lastDate), date -> date.plusDays(1))
                .map(date -> new SellerSalesStats.RevenueByDay(
                        DATE_FORMAT.format(date),
                        amountsByDate.getOrDefault(DATE_FORMAT.format(date), BigDecimal.ZERO)))
                .toList();
    }

    private SellerSalesStats.BestSeller toBestSeller(Document document) {
        return new SellerSalesStats.BestSeller(
                document.getString(PRODUCT_ID),
                document.getString("name"),
                numberValue(document.get(UNITS)),
                decimalValue(document.get(REVENUE)));
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
