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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

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
public class SellerSalesStatsService {
    static final String INDEX_NAME = "seller_created_at_stats";
    private static final String ORDERS_COLLECTION = "orders";
    private static final String REVENUE = "revenue";
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
        Document match = orderMatch("sellerId", sellerId, from, to);
        Document amount = itemAmount();
        List<Document> summary = orderSummary(REVENUE, amount, true);
        List<Document> bestSellers = bestProducts(REVENUE, amount);

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

}
