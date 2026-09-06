package com.ecommerce.apigateway.security;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class UserClaimsRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> headers = new HashMap<>();

    public UserClaimsRequestWrapper(
            HttpServletRequest request,
            String userId,
            String role) {

        super(request);

        headers.put("X-User-Id", userId);
        headers.put("X-User-Role", role);
    }

    @Override
    public String getHeader(String name) {

        String value = headers.get(name);

        if (value != null) {
            return value;
        }

        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {

        String value = headers.get(name);

        if (value != null) {
            return Collections.enumeration(
                    Collections.singletonList(value)
            );
        }

        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {

        var names = new java.util.HashSet<String>();

        Enumeration<String> originalNames =
                super.getHeaderNames();

        if (originalNames != null) {
            while (originalNames.hasMoreElements()) {
                names.add(originalNames.nextElement());
            }
        }

        names.addAll(headers.keySet());

        return Collections.enumeration(names);
    }
}