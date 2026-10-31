package com.ecommerce.productservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ProductServiceApplicationTests {
	@Container
	private static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

	@DynamicPropertySource
	static void configureMongo(DynamicPropertyRegistry registry) {
		registry.add("spring.mongodb.uri", mongo::getReplicaSetUrl);
		registry.add("eureka.client.enabled", () -> false);
		registry.add("eureka.client.fetch-registry", () -> false);
		registry.add("eureka.client.register-with-eureka", () -> false);
	}

	@Test
	void contextLoads() {
	}

}
