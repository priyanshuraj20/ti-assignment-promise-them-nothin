package com.relayapi.ratelimiter.web;

import com.relayapi.ratelimiter.domain.model.RateLimitResult;
import com.relayapi.ratelimiter.service.PolicyEngineService;
import com.relayapi.ratelimiter.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    public static final String HEADER_CUSTOMER_ID = "X-Customer-Id";
    public static final String HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimiterService rateLimiterService;

    public RateLimiterFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String customerId = request.getHeader(HEADER_CUSTOMER_ID);
        if (customerId == null || customerId.isBlank()) {
            customerId = PolicyEngineService.DEFAULT_STARTER_TIER;
        }

        RateLimitResult result = rateLimiterService.checkRateLimit(customerId, Instant.now());

        // Always attach rate limit headers to response
        response.setHeader(HEADER_LIMIT, String.valueOf(result.capacity()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remainingTokens()));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetSeconds()));

        if (result.isAllowed()) {
            filterChain.doFilter(request, response);
        } else {
            // Exceeded quota -> HTTP 429 Short Circuit
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.resetSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Retry after %d seconds.\"}",
                    result.resetSeconds()
            ));
        }
    }
}
