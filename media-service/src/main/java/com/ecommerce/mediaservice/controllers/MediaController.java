package com.ecommerce.mediaservice.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import com.ecommerce.mediaservice.common.ResponseData;
import com.ecommerce.mediaservice.dtos.MediaRequest;
import com.ecommerce.mediaservice.services.MediaService;
import jakarta.validation.Valid;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media/images")
public class MediaController {
    public final MediaService mediaService;

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ResponseData<String>> saveMedia(@Valid @RequestBody MediaRequest request,
            @RequestParam(required = false) MultipartFile[] images) {
        return ResponseEntity.ok(mediaService.saveMedia(request, images));
    }

    @PermitAll
    @GetMapping
    public ResponseEntity<ResponseData<Map<String, List<String>>>> getProductsMedias() {
        return ResponseEntity.ok(mediaService.getProductsMedias());
    }

    @PermitAll
    @GetMapping("/{target}/{id}")
    public ResponseEntity<ResponseData<List<String>>> getMedia(@PathVariable String target, @PathVariable String targetId) {
        // check if the id is for product or user
        return null;
    }
}
