package com.ecommerce.orderservice.order.services;

import com.ecommerce.orderservice.common.ResponseData;
import com.ecommerce.orderservice.order.dtos.response.OrderResponse;
import com.ecommerce.orderservice.order.dtos.response.PageResponse;
import com.ecommerce.orderservice.order.models.Order;
import com.ecommerce.orderservice.order.models.OrderStatus;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OrderSearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_TERM_LENGTH = 100;

    private final MongoTemplate mongoTemplate;

    /**
     * ownerField is "userId" for a client's own orders, "sellerId" for a seller's orders.
     */
    public ResponseData<PageResponse<OrderResponse>> search(String ownerField, String ownerId, boolean hideDeleted,
            OrderStatus status, LocalDate from, LocalDate to, String q, int page, int size, boolean forSeller) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where(ownerField).is(ownerId));

        if (hideDeleted) {
            filters.add(Criteria.where("deletedByUser").is(false));
        }
        if (status != null) {
            filters.add(Criteria.where("status").is(status));
        }
        if (from != null) {
            filters.add(Criteria.where("createdAt").gte(from.atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (to != null) {
            // include the whole "to" day
            filters.add(Criteria.where("createdAt").lt(to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (q != null && !q.isBlank()) {
            String term = q.trim();
            if (term.length() > MAX_SEARCH_TERM_LENGTH) {
                term = term.substring(0, MAX_SEARCH_TERM_LENGTH);
            }

            List<Criteria> any = new ArrayList<>();
            any.add(Criteria.where("items.name").regex(Pattern.quote(term), "i")); // quote = no regex injection
            if (term.matches("[a-fA-F0-9]{24}")) {
                any.add(Criteria.where("id").is(term));
            }
            filters.add(new Criteria().orOperator(any));
        }

        Query query = new Query(new Criteria().andOperator(filters));
        long total = mongoTemplate.count(query, Order.class);

        query.with(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Order> foundOrders = mongoTemplate.find(query, Order.class);

        List<OrderResponse> content = new ArrayList<>();
        for (Order order : foundOrders) {
            content.add(OrderResponse.from(order, forSeller));
        }

        int totalPages = (int) Math.ceil((double) total / safeSize);
        PageResponse<OrderResponse> pageResponse = new PageResponse<>(content, safePage, safeSize, total, totalPages);

        return ResponseData.success("Orders retrieved successfully", pageResponse);
    }
}
