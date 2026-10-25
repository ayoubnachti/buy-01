package com.ecommerce.mediaservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.ecommerce.mediaservice.clients.ProductServiceGateway;
import com.ecommerce.mediaservice.dtos.TargetType;
import com.ecommerce.mediaservice.exceptions.Product.ProductNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.ImageNotFoundException;
import com.ecommerce.mediaservice.exceptions.media.ImageNullOrEmptyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageBodyException;
import com.ecommerce.mediaservice.exceptions.media.InvalidImageTypeException;
import com.ecommerce.mediaservice.exceptions.media.InvalidSizeLimitException;
import com.ecommerce.mediaservice.exceptions.profile.ForbiddenToChangeProfileException;

class MediaHelperTest {

    private Cloudinary cloudinary;
    private ProductServiceGateway productServiceGateway;
    private MediaHelper helper;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        productServiceGateway = mock(ProductServiceGateway.class);
        helper = new MediaHelper(cloudinary, productServiceGateway);
    }

    private static byte[] realPng() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    // ---- validateImage

    @Test
    void acceptsAValidImage() throws IOException {
        MockMultipartFile image = new MockMultipartFile("file", "a.png", "image/png", realPng());
        assertThatCode(() -> helper.validateImage(image)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnEmptyImage() {
        MockMultipartFile image = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> helper.validateImage(image)).isInstanceOf(ImageNullOrEmptyException.class);
    }

    @Test
    void rejectsAnImageOver2MB() {
        MockMultipartFile image = new MockMultipartFile("file", "a.png", "image/png", new byte[2 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> helper.validateImage(image)).isInstanceOf(InvalidSizeLimitException.class);
    }

    @Test
    void rejectsANonImageContentType() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", realPng());
        assertThatThrownBy(() -> helper.validateImage(file)).isInstanceOf(InvalidImageTypeException.class);
    }

    @Test
    void rejectsABodyThatIsNotAnImage() {
        MockMultipartFile fake = new MockMultipartFile("file", "a.png", "image/png", "not an image".getBytes());
        assertThatThrownBy(() -> helper.validateImage(fake)).isInstanceOf(InvalidImageBodyException.class);
    }

    // ---- folders and ownership

    @Test
    void buildsTheFolderForEachTargetType() {
        assertThat(helper.getFolder(TargetType.PRODUCT, "p1")).isEqualTo("products/p1/");
        assertThat(helper.getFolder(TargetType.PROFILE, "u1")).isEqualTo("profile/u1/");
    }

    @Test
    void rejectsAnImageFromAnotherProduct() {
        String otherProductImage = "https://res.cloudinary.com/demo/image/upload/v1/products/p2/a.jpg";
        assertThatThrownBy(() -> helper.verifyImageBelongsToTarget(otherProductImage, TargetType.PRODUCT, "p1"))
                .isInstanceOf(ImageNotFoundException.class);
    }

    @Test
    void forbidsChangingSomeoneElsesProfile() {
        assertThatThrownBy(() -> helper.checkOwnership(TargetType.PROFILE, "u1", "u2"))
                .isInstanceOf(ForbiddenToChangeProfileException.class);
    }

    @Test
    void rejectsAnUnknownProduct() {
        when(productServiceGateway.getProduct("p1")).thenReturn(null);
        assertThatThrownBy(() -> helper.checkOwnership(TargetType.PRODUCT, "p1", "u1"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ---- Cloudinary deletion: the public id is extracted from the URL

    @Test
    void deletesFromCloudinaryUsingThePublicIdFromTheUrl() throws IOException {
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("products/p1/abc"), anyMap())).thenReturn(Map.of("result", "ok"));

        helper.deleteFromCloudinary("https://res.cloudinary.com/demo/image/upload/v123/products/p1/abc.jpg");

        verify(uploader).destroy(eq("products/p1/abc"), anyMap());
    }
}