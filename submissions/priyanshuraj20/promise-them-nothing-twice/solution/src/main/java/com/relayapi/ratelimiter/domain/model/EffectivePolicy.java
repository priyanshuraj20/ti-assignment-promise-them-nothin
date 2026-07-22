package com.relayapi.ratelimiter.domain.model;

import java.util.Objects;

public record EffectivePolicy(
        long capacity,
        double refillRateTokensPerSec
) {
    public EffectivePolicy {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (refillRateTokensPerSec <= 0) {
            throw new IllegalArgumentException("refillRateTokensPerSec must be positive");
        }
    }
}
