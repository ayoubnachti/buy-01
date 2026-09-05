package com.ecommerce.userservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

  @Bean
  CommandLineRunner seedUsers(UserRepository repository) {
    return args -> {
      if (repository.count() == 0) {
        repository.save(
            User.builder()
                .id("1")
                .name("Ayoub Nachti")
                .email("ayoub@gmail.com")
                .role(UserRole.USER)
                .password("123456")
                .build());
        repository.save(
            User.builder()
                .id("2")
                .name("Bob Walts")
                .email("Bob@gmail.com")
                .role(UserRole.USER)
                .password("123456")
                .build());
      }
    };
  }
}
