package com.ecommerce.orderservice.analytics;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;


@Component
public class UserPurchaseStatsIndexInitializer implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    public UserPurchaseStatsIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps("orders").createIndex(new Index()
                .on("userId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named(UserPurchaseStatsService.INDEX_NAME));
        mongoTemplate.indexOps("orders").createIndex(new Index()
                .on("sellerId", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named(SellerSalesStatsService.INDEX_NAME));
    }
}
