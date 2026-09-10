package com.ecommerce.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.userservice.dto.request.LoginRequest;
import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.response.ApiResponse;
import com.ecommerce.userservice.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthService authService;

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<String>> login(
                        @Valid @RequestBody LoginRequest request) {

                return ResponseEntity.ok(
                                authService.login(request));
        }

        @PostMapping("/validateJwt")
        public ResponseEntity<ApiResponse<Boolean>> validationJwt(
                        @RequestHeader("X-User-Id") String userId,
                        @RequestHeader("X-User-Role") String role) {

                return ResponseEntity.ok(
                                ApiResponse.success(
                                                "JWT is valid",
                                                authService.validateJwt(userId, role)));
        }

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<Void>> register(
                        @Valid @RequestBody RegisterRequest request) {

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(authService.register(request));
        }
}