package com.ecommerce.mediaservice.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.exceptions.media.CloudinaryUploadException;
import com.ecommerce.mediaservice.exceptions.media.ImageNullOrEmptyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageBodyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageTypeException;
import com.ecommerce.mediaservice.exceptions.media.InvalidSizeLimitException;
import com.ecommerce.mediaservice.exceptions.media.MediaPersistenceException;
import com.ecommerce.mediaservice.models.Media;
import com.ecommerce.mediaservice.repositories.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    public final MediaRepository mediaRepository;
    private final Cloudinary cloudinary;

    public ResponseData<String> saveMedia(String productId, MultipartFile[] images) {
        for (MultipartFile image : images) {
            validateImage(image);
            String imageUrl = uploadToCloudinary(image, productId);
            try {
                Media media = Media.builder().imagePath(imageUrl).productId(productId).build();
                mediaRepository.save(media);
            } catch (Exception ex) {
                throw new MediaPersistenceException("Failed to save media to the database !", ex);
            }
        }
        return ResponseData.success("Product saved successfully !", null);
    }

    private String uploadToCloudinary(MultipartFile image, String productId) {
        Map<?, ?> uploadResult;
        try {
            uploadResult = cloudinary.uploader().upload(image.getBytes(),
                    ObjectUtils.asMap("folder", "products/" + productId));
        } catch (IOException e) {
            throw new CloudinaryUploadException("Failed to upload image to Cloudinary !", e);
        }

        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null) {
            throw new CloudinaryUploadException("Cloudinary did not return a valid upload result !");
        }
        return secureUrl.toString();
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ImageNullOrEmptyException("The image is empty or null !");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new InvalidSizeLimitException("The image has more than 2MB !");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidImageTypeException("Invalid content type !");
        }

        try {
            if (ImageIO.read(image.getInputStream()) == null) {
                throw new InvalidImageBodyException("The image body doesn't contain data of an image !");
            }
        } catch (IOException e) {
            throw new InvalidImageBodyException("The image body doesn't contain data of an image !");
        }
    }
}
