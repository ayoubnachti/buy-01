package com.ecommerce.mediaservice.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mediaservice.models.Media;


@Repository
public interface MediaRepository extends MongoRepository<Media, String> {
    
}
