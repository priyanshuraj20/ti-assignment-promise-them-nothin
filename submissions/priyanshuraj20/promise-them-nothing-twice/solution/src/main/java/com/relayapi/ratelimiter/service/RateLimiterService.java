package com.relayapi.ratelimiter.service;

import com.relayapi.ratelimiter.client.RedisTokenBucketClient;
import com.relayapi.ratelimiter.domain.model.EffectivePolicy;
import com.relayapi.ratelimiter.domain.model.RateLimitResult;
import com.relayapi.ratelimiter.domain.model.TokenBucketResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class RateLimiterService {

    private final PolicyEngineService policyEngineService;
    private final RedisTokenBucketClient redisTokenBucketClient;

    public RateLimiterService(PolicyEngineService policyEngineService, RedisTokenBucketClient redisTokenBucketClient) {
        this.policyEngineService = policyEngineService;
        this.redisTokenBucketClient = redisTokenBucketClient;
    }

    public RateLimitResult checkRateLimit(String customerId, Instant instant) {
        Objects.requireNonNull(instant, "instant cannot be null");

        // 1. Resolve time-aware effective policy
        EffectivePolicy policy = policyEngineService.getEffectivePolicy(customerId, instant);

        // 2. Consume token via atomic Redis client
        TokenBucketResponse bucketResponse = redisTokenBucketClient.consumeToken(
                customerId != null && !customerId.isBlank() ? customerId : PolicyEngineService.DEFAULT_STARTER_TIER,
                instant,
                policy.capacity(),
                policy.refillRateTokensPerSec()
        );

        // 3. Assemble and return final RateLimitResult
        return new RateLimitResult(
                bucketResponse.isAllowed(),
                bucketResponse.remainingTokens(),
                bucketResponse.resetSeconds(),
                policy.capacity()
        );
    }
}
