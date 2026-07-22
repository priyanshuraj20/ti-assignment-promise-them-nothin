package com.relayapi.ratelimiter.domain.model;

public record TokenBucketResponse(
        boolean isAllowed,
        long remainingTokens,
        long resetSeconds
) {}
