package com.relayapi.ratelimiter.domain.model;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TenantPolicy(
        String customerId,
        long baseCapacity,
        double baseRefillRateTokensPerSec,
        List<ScheduledWindow> scheduledWindows
) {
    public TenantPolicy {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        if (baseCapacity <= 0) {
            throw new IllegalArgumentException("baseCapacity must be positive");
        }
        if (baseRefillRateTokensPerSec <= 0) {
            throw new IllegalArgumentException("baseRefillRateTokensPerSec must be positive");
        }
        scheduledWindows = (scheduledWindows == null) ? Collections.emptyList() : List.copyOf(scheduledWindows);
    }

    public Optional<ScheduledWindow> getActiveWindow(LocalTime time) {
        return scheduledWindows.stream()
                .filter(w -> w.isActiveAt(time))
                .findFirst();
    }

    public EffectivePolicy getEffectivePolicyAt(LocalTime time) {
        return getActiveWindow(time)
                .map(w -> new EffectivePolicy(w.burstCapacity(), w.burstRefillRateTokensPerSec()))
                .orElseGet(() -> new EffectivePolicy(baseCapacity, baseRefillRateTokensPerSec));
    }
}
