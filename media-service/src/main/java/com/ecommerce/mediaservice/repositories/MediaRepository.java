package com.ecommerce.mediaservice.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ecommerce.mediaservice.models.Media;



public interface MediaRepository extends MongoRepository<Media, String> {
    
}
