package com.ecommerce.mediaservice.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.repositories.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {
    public final MediaRepository mediaRepository;

    public ResponseData<String> saveMedia(String productId, MultipartFile[] images) {
        return null;
    } 
}
