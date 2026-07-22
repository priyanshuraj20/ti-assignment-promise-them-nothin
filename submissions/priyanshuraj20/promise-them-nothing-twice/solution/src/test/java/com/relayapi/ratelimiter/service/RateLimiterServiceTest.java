package com.relayapi.ratelimiter.service;

import com.relayapi.ratelimiter.client.RedisTokenBucketClient;
import com.relayapi.ratelimiter.domain.model.EffectivePolicy;
import com.relayapi.ratelimiter.domain.model.RateLimitResult;
import com.relayapi.ratelimiter.domain.model.TokenBucketResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private PolicyEngineService policyEngineService;

    @Mock
    private RedisTokenBucketClient redisTokenBucketClient;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(policyEngineService, redisTokenBucketClient);
    }

    @Test
    void testCheckRateLimitAllowed() {
        String customerId = "northwind";
        Instant now = Instant.parse("2026-07-22T02:30:00Z"); // batch window

        EffectivePolicy effectivePolicy = new EffectivePolicy(1200, 20.0);
        TokenBucketResponse bucketResponse = new TokenBucketResponse(true, 1199, 1);

        when(policyEngineService.getEffectivePolicy(customerId, now)).thenReturn(effectivePolicy);
        when(redisTokenBucketClient.consumeToken(customerId, now, 1200, 20.0)).thenReturn(bucketResponse);

        RateLimitResult result = rateLimiterService.checkRateLimit(customerId, now);

        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(1199, result.remainingTokens());
        assertEquals(1, result.resetSeconds());
        assertEquals(1200, result.capacity());

        verify(policyEngineService).getEffectivePolicy(customerId, now);
        verify(redisTokenBucketClient).consumeToken(customerId, now, 1200, 20.0);
    }

    @Test
    void testCheckRateLimitRejected() {
        String customerId = "starter-user";
        Instant now = Instant.parse("2026-07-22T10:00:00Z");

        EffectivePolicy effectivePolicy = new EffectivePolicy(60, 1.0);
        TokenBucketResponse bucketResponse = new TokenBucketResponse(false, 0, 5);

        when(policyEngineService.getEffectivePolicy(customerId, now)).thenReturn(effectivePolicy);
        when(redisTokenBucketClient.consumeToken(customerId, now, 60, 1.0)).thenReturn(bucketResponse);

        RateLimitResult result = rateLimiterService.checkRateLimit(customerId, now);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals(0, result.remainingTokens());
        assertEquals(5, result.resetSeconds());
        assertEquals(60, result.capacity());
    }
}
