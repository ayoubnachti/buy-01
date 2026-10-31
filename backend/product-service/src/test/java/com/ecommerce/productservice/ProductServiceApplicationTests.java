package com.ecommerce.productservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ecommerce.productservice.dtos.request.ProductSearchRequest;
import com.ecommerce.productservice.models.Product;
import com.ecommerce.productservice.models.ProductCategory;
import com.ecommerce.productservice.repositories.ProductSearchRepository;

@SpringBootTest
@Testcontainers
class ProductServiceApplicationTests {
	@Container
	private static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private ProductSearchRepository productSearchRepository;

	@DynamicPropertySource
	static void configureMongo(DynamicPropertyRegistry registry) {
		registry.add("spring.mongodb.uri", mongo::getReplicaSetUrl);
		registry.add("eureka.client.enabled", () -> false);
		registry.add("eureka.client.fetch-registry", () -> false);
		registry.add("eureka.client.register-with-eureka", () -> false);
	}

	@BeforeEach
	void clearProducts() {
		mongoTemplate.remove(new Query(), Product.class);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void searchAppliesEachFilterAndAllFiltersTogether() {
		insertProducts();

		assertThat(search(null, List.of(ProductCategory.HOME), null, null, "newest", 0, 10))
				.extracting(Product::getId)
				.containsExactly("p5", "p4", "p2", "p1");
		assertThat(search(null, null, new BigDecimal("30"), null, "newest", 0, 10))
				.extracting(Product::getId)
				.containsExactly("p5", "p4", "p3");
		assertThat(search(null, null, null, new BigDecimal("20"), "newest", 0, 10))
				.extracting(Product::getId)
				.containsExactly("p2", "p1");
		assertThat(search("desk", List.of(ProductCategory.HOME),
				new BigDecimal("20"), new BigDecimal("50"), "newest", 0, 10))
				.extracting(Product::getId)
				.containsExactly("p5", "p2");
	}

	@Test
	void searchSupportsEverySortAndPagesIncludingLastAndOutOfRange() {
		insertProducts();

		assertThat(search("", null, null, null, "price_asc", 0, 10))
				.extracting(Product::getId)
				.containsExactly("p1", "p2", "p3", "p4", "p5");
		assertThat(search("", null, null, null, "price_desc", 0, 10))
				.extracting(Product::getId)
				.containsExactly("p5", "p4", "p3", "p2", "p1");
		assertThat(search("", null, null, null, "newest", 0, 10))
				.extracting(Product::getId)
				.containsExactly("p5", "p4", "p3", "p2", "p1");
		assertThat(search("desk", null, null, null, "relevance", 0, 10))
				.extracting(Product::getId)
				.contains("p1", "p2", "p3", "p5");

		assertThat(productSearchRepository.search(request("", null, null, null, "newest", 1, 2))
				.getContent())
				.extracting(Product::getId)
				.containsExactly("p3", "p2");
		assertThat(productSearchRepository.search(request("", null, null, null, "newest", 2, 2))
				.getContent())
				.extracting(Product::getId)
				.containsExactly("p1");

		var outOfRange = productSearchRepository.search(request("", null, null, null, "newest", 3, 2));
		assertThat(outOfRange.getContent()).isEmpty();
		assertThat(outOfRange.getTotalElements()).isEqualTo(5);
		assertThat(outOfRange.getTotalPages()).isEqualTo(3);
	}

	@Test
	void facetsReturnCategoryCountsAndOverallPriceRange() {
		insertProducts();

		var facets = productSearchRepository.getFacets();

		assertThat(facets.categories())
				.containsExactlyInAnyOrder(
						new com.ecommerce.productservice.dtos.response.ProductSearchFacetsResponse.CategoryCount(
								ProductCategory.HOME, 4),
						new com.ecommerce.productservice.dtos.response.ProductSearchFacetsResponse.CategoryCount(
								ProductCategory.BOOKS, 1));
		assertThat(facets.minPrice()).isEqualByComparingTo("10");
		assertThat(facets.maxPrice()).isEqualByComparingTo("50");
	}

	@Test
	void partialWordSearchFallsBackToNameAndDescriptionPrefixes() {
		insertProducts();

		assertThat(search("off", null, null, null, "relevance", 0, 12))
				.extracting(Product::getId)
				.containsExactly("p4");
	}

	@Test
	void textSearchExplainUsesTextIndex() {
		insertProducts();
		TextQuery query = TextQuery.queryText(TextCriteria.forDefaultLanguage().matchingAny("desk"));
		query.addCriteria(new Criteria().andOperator(
				Criteria.where("category").is(ProductCategory.HOME),
				Criteria.where("price").gte(new BigDecimal("10")).lte(new BigDecimal("50"))));
		query.sortByScore();

		Document explain = mongoTemplate.getCollection("products")
				.find(query.getQueryObject())
				.sort(query.getSortObject())
				.explain();

		assertThat(explain.toJson()).contains("product_name_description_text");
		assertThat(mongoTemplate.indexOps(Product.class).getIndexInfo())
				.extracting(org.springframework.data.mongodb.core.index.IndexInfo::getName)
				.contains("product_category_price", "product_created_at");
	}

	private List<Product> search(
			String query,
			List<ProductCategory> categories,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			String sort,
			int page,
			int size) {
		return productSearchRepository.search(request(query, categories, minPrice, maxPrice, sort, page, size))
				.getContent();
	}

	private ProductSearchRequest request(
			String query,
			List<ProductCategory> categories,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			String sort,
			int page,
			int size) {
		return new ProductSearchRequest(query, categories, minPrice, maxPrice, sort, page, size);
	}

	private void insertProducts() {
		mongoTemplate.insertAll(List.of(
				product("p1", "Desk table", "desk", "10", ProductCategory.HOME, "2026-01-01T00:00:00Z"),
				product("p2", "Desk lamp", "desk lamp", "20", ProductCategory.HOME, "2026-02-01T00:00:00Z"),
				product("p3", "Desk organizer", "desk organizer", "30", ProductCategory.BOOKS, "2026-03-01T00:00:00Z"),
				product("p4", "Office chair", "chair", "40", ProductCategory.HOME, "2026-04-01T00:00:00Z"),
				product("p5", "Desk stool", "desk", "50", ProductCategory.HOME, "2026-05-01T00:00:00Z")));
	}

	private Product product(
			String id, String name, String description, String price, ProductCategory category, String createdAt) {
		return Product.builder()
				.id(id)
				.name(name)
				.description(description)
				.price(new BigDecimal(price))
				.category(category)
				.userId("seller")
				.createdAt(Instant.parse(createdAt))
				.build();
	}
}
