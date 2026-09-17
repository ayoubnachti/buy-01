package com.ecommerce.mediaservice.models;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Document(collection = "media")
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Media {
    
}
