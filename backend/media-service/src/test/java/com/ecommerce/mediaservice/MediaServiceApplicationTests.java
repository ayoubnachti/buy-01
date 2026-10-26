package com.ecommerce.mediaservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.ecommerce.mediaservice.services.MediaService;

@SpringBootTest(properties = "eureka.client.enabled=false")   // no discovery server in tests
class MediaServiceApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context.getBean(MediaService.class)).isNotNull();
    }
}