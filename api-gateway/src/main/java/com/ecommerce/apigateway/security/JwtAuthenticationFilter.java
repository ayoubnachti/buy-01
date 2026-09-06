package com.ecommerce.apigateway.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey secretKey;

    public JwtAuthenticationFilter(
            @Value("${jwt.secret}") String secret) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // No JWT → let Spring Security handle authentication
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        try {

            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.get("userId", String.class);
            String role = claims.get("role", String.class);

            if (userId == null || role == null) {
                sendUnauthorized(response, "Invalid JWT claims");
                return;
            }

            /*
             * Put authenticated user into Spring Security context.
             */
            var authorities = java.util.List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

            /*
             * Forward trusted user information
             * to the downstream microservice.
             */
            HttpServletRequest wrappedRequest =
                    new UserClaimsRequestWrapper(
                            request,
                            userId,
                            role
                    );

            filterChain.doFilter(wrappedRequest, response);

        } catch (Exception e) {

            SecurityContextHolder.clearContext();

            sendUnauthorized(response, "Invalid or expired JWT");
        }
    }

    private void sendUnauthorized(
            HttpServletResponse response,
            String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        response.getWriter().write(
                "{\"error\":\"" + message + "\"}"
        );
    }
}