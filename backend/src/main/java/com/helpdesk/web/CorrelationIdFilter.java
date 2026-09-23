package com.helpdesk.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String ATTRIBUTE_NAME = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(HEADER_NAME);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        request.setAttribute(ATTRIBUTE_NAME, requestId);
        response.setHeader(HEADER_NAME, requestId);

        try {
            MDC.put(ATTRIBUTE_NAME, requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(ATTRIBUTE_NAME);
        }
    }
}
