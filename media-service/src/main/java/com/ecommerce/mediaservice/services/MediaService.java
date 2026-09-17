package com.ecommerce.mediaservice.services;

import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.repositories.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    public final MediaRepository mediaRepository;

    public ResponseData<String> saveMedia(String productId, MultipartFile[] images) {
        return null;
    }

    private boolean isValidImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return false;
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            return false;
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return false;
        }

        try {
            return ImageIO.read(image.getInputStream()) != null;
        } catch (IOException e) {
            return false;
        }
    }
}
