package com.ecommerce.userservice.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ecommerce.userservice.enums.UserRole;
import com.ecommerce.userservice.model.User;
import com.ecommerce.userservice.repository.UserRepository;

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
                        .role(UserRole.CLIENT)
                        .password("123456")
                        .build()
                );

                repository.save(
                    User.builder()
                        .id("2")
                        .name("Bob Walts")
                        .email("Bob@gmail.com")
                        .role(UserRole.CLIENT)
                        .password("123456")
                        .build()
                );
            }
        };
    }
}