package com.relayapi.ratelimiter.service;

import com.relayapi.ratelimiter.TestRedisConfiguration;
import com.relayapi.ratelimiter.domain.model.RateLimitResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestRedisConfiguration.class)
class RateLimiterServiceIntegrationTest {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Test
    void testNorthwindBatchWindowIntegrationAdmittance() {
        String customerId = "northwind";
        // 02:30 UTC -> inside 1200 RPM batch window
        Instant batchTime = Instant.parse("2026-07-22T02:30:00Z");

        RateLimitResult result = rateLimiterService.checkRateLimit(customerId, batchTime);

        assertNotNull(result);
        assertTrue(result.isAllowed(), "Northwind batch request should be allowed");
        assertEquals(1200, result.capacity(), "Capacity during batch window should be 1200");
    }

    @Test
    void testStarterTierDepletionIntegration() {
        String customerId = "starter-integration-test-user";
        Instant now = Instant.now();

        // Consume 60 tokens for Starter tier (60 capacity)
        for (int i = 0; i < 60; i++) {
            RateLimitResult result = rateLimiterService.checkRateLimit(customerId, now);
            assertTrue(result.isAllowed(), "Request " + (i + 1) + " should be allowed");
        }

        // 61st request should be rejected (429)
        RateLimitResult exceeded = rateLimiterService.checkRateLimit(customerId, now);
        assertFalse(exceeded.isAllowed(), "Request 61 should be rate limited");
        assertEquals(0, exceeded.remainingTokens());
        assertTrue(exceeded.resetSeconds() > 0, "Reset seconds should be > 0");
    }
}
