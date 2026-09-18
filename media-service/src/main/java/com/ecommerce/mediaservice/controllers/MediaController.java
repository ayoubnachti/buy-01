package com.ecommerce.mediaservice.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.services.MediaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaController {
    public final MediaService mediaService;

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ResponseData<String>> saveMedia(@PathVariable String productId,
            @RequestParam(required = false) MultipartFile[] images) {
        return ResponseEntity.ok(mediaService.saveMedia(productId, images));
    }
}
