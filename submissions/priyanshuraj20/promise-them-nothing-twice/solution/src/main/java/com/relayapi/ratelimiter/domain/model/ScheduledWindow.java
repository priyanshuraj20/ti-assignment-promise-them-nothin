package com.relayapi.ratelimiter.domain.model;

import java.time.LocalTime;
import java.util.Objects;

public record ScheduledWindow(
        LocalTime startTime,
        LocalTime endTime,
        long burstCapacity,
        double burstRefillRateTokensPerSec
) {
    public ScheduledWindow {
        Objects.requireNonNull(startTime, "startTime cannot be null");
        Objects.requireNonNull(endTime, "endTime cannot be null");
        if (burstCapacity <= 0) {
            throw new IllegalArgumentException("burstCapacity must be positive");
        }
        if (burstRefillRateTokensPerSec <= 0) {
            throw new IllegalArgumentException("burstRefillRateTokensPerSec must be positive");
        }
    }

    public boolean isActiveAt(LocalTime time) {
        Objects.requireNonNull(time, "time cannot be null");
        if (startTime.equals(endTime)) {
            return false;
        }
        if (startTime.isBefore(endTime)) {
            return !time.isBefore(startTime) && time.isBefore(endTime);
        } else {
            // Overnight window (e.g. 23:00 to 02:00)
            return !time.isBefore(startTime) || time.isBefore(endTime);
        }
    }
}
