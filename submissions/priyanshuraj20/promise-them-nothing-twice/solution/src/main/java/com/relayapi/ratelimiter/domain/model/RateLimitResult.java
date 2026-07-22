package com.relayapi.ratelimiter.domain.model;

public record RateLimitResult(
        boolean isAllowed,
        long remainingTokens,
        long resetSeconds,
        long capacity
) {}
