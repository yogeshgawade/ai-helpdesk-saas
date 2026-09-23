package com.helpdesk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.auth.User;
import com.helpdesk.web.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LOGIN_LIMIT = 5;
    private static final Duration LOGIN_WINDOW =
            Duration.ofMinutes(1);

    private static final int REGISTER_LIMIT = 3;
    private static final Duration REGISTER_WINDOW =
            Duration.ofHours(1);

    private static final int AI_LIMIT = 10;
    private static final Duration AI_WINDOW =
            Duration.ofMinutes(1);

    private static final int KB_SEARCH_LIMIT = 20;
    private static final Duration KB_SEARCH_WINDOW =
            Duration.ofMinutes(1);

    private final RedisRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RedisRateLimiter rateLimiter,
            ObjectMapper objectMapper
    ) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        RateLimitDecision decision = determineLimit(request, path);

        if (decision == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (rateLimiter.allow(
                decision.key(),
                decision.limit(),
                decision.window()
        )) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter =
                rateLimiter.retryAfterSeconds(decision.key());

        writeTooManyRequests(
                request,
                response,
                retryAfter
        );
    }

    private RateLimitDecision determineLimit(
            HttpServletRequest request,
            String path
    ) {

        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/login".equals(path)) {

            return new RateLimitDecision(
                    "rate-limit:login:ip:" + request.getRemoteAddr(),
                    LOGIN_LIMIT,
                    LOGIN_WINDOW
            );
        }

        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/auth/register".equals(path)) {

            return new RateLimitDecision(
                    "rate-limit:register:ip:" + request.getRemoteAddr(),
                    REGISTER_LIMIT,
                    REGISTER_WINDOW
            );
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            if (path.matches(
                    "/api/orgs/[^/]+/kb/search"
            )) {
                return authenticatedUserDecision(
                        "kb-search",
                        KB_SEARCH_LIMIT,
                        KB_SEARCH_WINDOW
                );
            }

            return null;
        }

        if (path.matches(
                "/api/orgs/[^/]+/tickets/[^/]+/ai/suggest-response"
        )) {
            return authenticatedUserDecision(
                    "ai-suggest-response",
                    AI_LIMIT,
                    AI_WINDOW
            );
        }

        if (path.matches(
                "/api/orgs/[^/]+/tickets/[^/]+/ai/stream-response"
        )) {
            return authenticatedUserDecision(
                    "ai-stream-response",
                    AI_LIMIT,
                    AI_WINDOW
            );
        }

        if (path.matches(
                "/api/orgs/[^/]+/kb/rag"
        )) {
            return authenticatedUserDecision(
                    "ai-rag",
                    AI_LIMIT,
                    AI_WINDOW
            );
        }

        return null;
    }

    private RateLimitDecision authenticatedUserDecision(
            String operation,
            int limit,
            Duration window
    ) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User user)
                || user.getId() == null) {

            return null;
        }

        return new RateLimitDecision(
                "rate-limit:"
                        + operation
                        + ":user:"
                        + user.getId(),
                limit,
                window
        );
    }

    private void writeTooManyRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfter
    ) throws IOException {

        String requestId =
                (String) request.getAttribute(
                        CorrelationIdFilter.ATTRIBUTE_NAME
                );

        if (requestId == null || requestId.isBlank()) {
            requestId =
                    request.getHeader(
                            CorrelationIdFilter.HEADER_NAME
                    );
        }

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limit exceeded. Please try again later."
                );

        problemDetail.setTitle("Too Many Requests");
        problemDetail.setType(
                URI.create(
                        "https://api.helpdesk.local/errors/rate-limit-exceeded"
                )
        );
        problemDetail.setInstance(
                URI.create(request.getRequestURI())
        );
        problemDetail.setProperty(
                "requestId",
                requestId
        );
        problemDetail.setProperty(
                "retryAfterSeconds",
                retryAfter
        );
        problemDetail.setProperty(
                "timestamp",
                Instant.now().toString()
        );

        response.setStatus(
                HttpStatus.TOO_MANY_REQUESTS.value()
        );
        response.setContentType(
                MediaType.APPLICATION_PROBLEM_JSON_VALUE
        );
        response.setHeader(
                "Retry-After",
                String.valueOf(retryAfter)
        );

        objectMapper.writeValue(
                response.getWriter(),
                problemDetail
        );
    }

    private record RateLimitDecision(
            String key,
            int limit,
            Duration window
    ) {
    }
}
