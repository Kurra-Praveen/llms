package com.loanplatform.common.security;

import com.loanplatform.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory per-IP rate limiting for sensitive endpoints:
 *  - /v1/auth/login, /v1/auth/refresh (brute-force protection)
 *  - /v1/portal/** (borrower-facing API)
 *
 * Single-instance friendly (ConcurrentHashMap). For multi-instance deployments,
 * back the buckets with Redis instead.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final int authCapacity;
    private final int portalCapacity;

    public RateLimitingFilter(
            @Value("${app.rate-limit.auth-requests-per-minute:10}") int authCapacity,
            @Value("${app.rate-limit.portal-requests-per-minute:60}") int portalCapacity) {
        this.authCapacity = authCapacity;
        this.portalCapacity = portalCapacity;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        int capacity = path.contains("/v1/auth/") ? authCapacity : portalCapacity;

        String key = clientIp(request) + "|" + (path.contains("/v1/auth/") ? "auth" : "portal");
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(capacity));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for {} on {}", clientIp(request), path);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("Too many requests. Please try again later.", "RATE_LIMIT_EXCEEDED")));
    }

    private Bucket newBucket(int capacity) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.greedy(capacity, Duration.ofMinutes(1))))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean limited = path.contains("/v1/auth/login")
                || path.contains("/v1/auth/refresh")
                || path.contains("/v1/portal/");
        return !limited;
    }
}
