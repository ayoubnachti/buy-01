package com.ecommerce.mediaservice.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.dtos.DeleteMediaRequest;
import com.ecommerce.mediaservice.dtos.MediaRequest;
import com.ecommerce.mediaservice.dtos.TargetType;
import com.ecommerce.mediaservice.exceptions.Product.ProducIdNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.CloudinaryDeleteException;
import com.ecommerce.mediaservice.exceptions.media.CloudinaryUploadException;
import com.ecommerce.mediaservice.exceptions.media.ImageNotDeletedException;
import com.ecommerce.mediaservice.exceptions.media.ImageNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.ImageNullOrEmptyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageBodyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageTypeException;
import com.ecommerce.mediaservice.exceptions.media.InvalidSizeLimitException;
import com.ecommerce.mediaservice.exceptions.media.MediaPersistenceException;
import com.ecommerce.mediaservice.exceptions.profile.ForbiddenToChangeProfileException;
import com.ecommerce.mediaservice.models.Media;
import com.ecommerce.mediaservice.repositories.MediaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaService {
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    public final MediaRepository mediaRepository;
    private final Cloudinary cloudinary;

    public ResponseData<List<String>> saveMedia(MediaRequest request, MultipartFile[] images) {
        List<String> imagesPaths = new ArrayList<>();
        if (images == null || images.length == 0) {
            throw new ImageNullOrEmptyException("At least one image is required !");
        }
        for (MultipartFile image : images) {
            validateImage(image);
            String imageUrl = uploadToCloudinary(image, request.targetType(), request.targetId());
            if (request.targetType().equals(TargetType.PRODUCT)) {
                Media media = Media.builder().imagePath(imageUrl).productId(request.targetId()).build();
                try {
                    mediaRepository.save(media);
                } catch (Exception ex) {
                    throw new MediaPersistenceException("Failed to save media to the database !", ex);
                }
            }
            imagesPaths.add(imageUrl);

        }
        return ResponseData.success("Media saved successfully !", imagesPaths);
    }

    public ResponseData<Map<String, List<String>>> getProductsMedias() {
        Map<String, List<String>> mediasByProduct = new HashMap<>();

        for (Media media : mediaRepository.findAll()) {
            if (media.getProductId() == null) {
                continue;
            }
            List<String> images = mediasByProduct.get(media.getProductId());
            if (images == null) {
                images = new ArrayList<>();
                mediasByProduct.put(media.getProductId(), images);
            }
            images.add(media.getImagePath());
        }

        return ResponseData.success("Products medias retrieved successfully !", mediasByProduct);
    }

    public ResponseData<List<String>> getMedias(String productId) {
        List<Media> medias = new ArrayList<>();

        medias = mediaRepository.findByProductId(productId)
                .orElseThrow(() -> new ProducIdNotFoundException("Product id not valid !"));

        List<String> imagesPaths = new ArrayList<>();
        for (Media m : medias) {
            imagesPaths.add(m.getImagePath());
        }
        return ResponseData.success("Product medias retrieved successfully !", imagesPaths);
    }

    public ResponseData<String> deleteMedias(String userId, DeleteMediaRequest request) {
        checkOwnership(request.targetType(), request.targetId(), userId);

        for (String imagePath : request.imagePaths()) {
            deleteFromCloudinary(imagePath);
            Media media = mediaRepository.findByImagePath(imagePath)
                    .orElseThrow(() -> new ImageNotFoundException("Image not found !"));
            try {
                mediaRepository.delete(media);
            } catch (Exception ex) {
                throw new ImageNotDeletedException("This image is not deleted, please try again later !");
            }
        }

        String folder = request.targetType().equals(TargetType.PRODUCT)
                ? "products/" + request.targetId()
                : "profile/" + request.targetId();
        deleteFolderIfEmpty(folder);
        return ResponseData.success("Image(s) deleted successfully !", null);
    }

    public ResponseData<List<String>> updateMedias(String userId, MediaRequest request, MultipartFile[] images) {
        checkOwnership(request.targetType(), request.targetId(), userId);

        if (images == null || images.length == 0) {
            throw new ImageNullOrEmptyException("At least one image is required !");
        }

        for (MultipartFile image : images) {
            validateImage(image);
        }

        if (request.oldImagePaths() != null) {
            for (String oldImagePath : request.oldImagePaths()) {
                if (request.targetType().equals(TargetType.PRODUCT)) {
                    Media media = mediaRepository.findByImagePath(oldImagePath)
                            .orElseThrow(() -> new ImageNotFoundException("Image not found !"));
                    if (!media.getProductId().equals(request.targetId())) {
                        throw new ImageNotFoundException("Image not found !");
                    }
                    deleteFromCloudinary(oldImagePath);
                    try {
                        mediaRepository.delete(media);
                    } catch (Exception ex) {
                        throw new ImageNotDeletedException("This image is not deleted, please try again later !");
                    }
                } else {
                    deleteFromCloudinary(oldImagePath);
                }
            }
        }

        List<String> newImagePaths = new ArrayList<>();
        for (MultipartFile image : images) {
            String imageUrl = uploadToCloudinary(image, request.targetType(), request.targetId());
            newImagePaths.add(imageUrl);
            if (request.targetType().equals(TargetType.PRODUCT)) {
                Media media = Media.builder()
                        .imagePath(imageUrl)
                        .productId(request.targetId())
                        .build();
                mediaRepository.save(media);
            }
        }

        String folder = request.targetType().equals(TargetType.PRODUCT)
                ? "products/" + request.targetId()
                : "profile/" + request.targetId();
        deleteFolderIfEmpty(folder);

        return ResponseData.success("Media updated successfully !", newImagePaths);
    }

    private String uploadToCloudinary(MultipartFile image, TargetType targetType, String targetId) {
        Map<?, ?> uploadResult;
        try {
            if (targetType.equals(TargetType.PRODUCT)) {
                uploadResult = cloudinary.uploader().upload(image.getBytes(),
                        ObjectUtils.asMap("folder", "products/" + targetId));
            } else {
                uploadResult = cloudinary.uploader().upload(image.getBytes(),
                        ObjectUtils.asMap("folder", "profile/" + targetId));
            }
        } catch (IOException e) {
            throw new CloudinaryUploadException("Failed to upload image to Cloudinary !", e);
        }

        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null) {
            throw new CloudinaryUploadException("Cloudinary did not return a valid upload result !");
        }
        return secureUrl.toString();
    }

    private void checkOwnership(TargetType targetType, String targetId, String userId) {
        if (targetType.equals(TargetType.PROFILE)) {
            boolean isOwner = targetId.equals(userId);
            if (!isOwner) {
                throw new ForbiddenToChangeProfileException("You do not have access to delete this image");
            }
        } else {
            // here I should check with the product service to see if the user wanting to
            // delete the medias is the owner of the product
        }
    }

    private void deleteFolderIfEmpty(String folder) {
        try {
            Map<?, ?> resourcesResult = cloudinary.api().resources(ObjectUtils.asMap(
                    "type", "upload",
                    "prefix", folder,
                    "max_results", 1));
            List<?> resources = (List<?>) resourcesResult.get("resources");
            if (resources == null || resources.isEmpty()) {
                cloudinary.api().deleteFolder(folder, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            throw new CloudinaryDeleteException("Failed to delete empty folder from Cloudinary !", e);
        }
    }

    private void deleteFromCloudinary(String imagePath) {
        String publicId = extractPublicId(imagePath);
        Map<?, ?> result;
        try {
            result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new CloudinaryDeleteException("Failed to delete image from Cloudinary !", e);
        }

        Object status = result.get("result");
        if (status == null || !status.equals("ok")) {
            throw new ImageNotFoundException("Image not found on Cloudinary : " + publicId);
        }
    }

    private String extractPublicId(String imageUrl) {
        int uploadIndex = imageUrl.indexOf("/upload/");
        if (uploadIndex == -1) {
            throw new CloudinaryDeleteException("Invalid Cloudinary image URL !");
        }

        String path = imageUrl.substring(uploadIndex + "/upload/".length());
        if (path.matches("^v\\d+/.*")) { // valid: v12/anything
            path = path.substring(path.indexOf('/') + 1);
        }

        int dotIndex = path.lastIndexOf('.');
        return dotIndex == -1 ? path : path.substring(0, dotIndex);
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
