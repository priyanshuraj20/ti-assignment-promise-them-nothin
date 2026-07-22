package com.relayapi.ratelimiter.service;

import com.relayapi.ratelimiter.domain.model.EffectivePolicy;
import com.relayapi.ratelimiter.domain.model.ScheduledWindow;
import com.relayapi.ratelimiter.domain.model.TenantPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PolicyEngineServiceTest {

    private PolicyEngineService policyEngineService;

    @BeforeEach
    void setUp() {
        TenantPolicy starter = new TenantPolicy("starter", 60, 1.0, List.of());
        TenantPolicy growth = new TenantPolicy("growth", 300, 5.0, List.of());

        ScheduledWindow northwindBatch = new ScheduledWindow(
                LocalTime.of(2, 0),
                LocalTime.of(4, 0),
                1200,
                20.0
        );
        TenantPolicy northwind = new TenantPolicy("northwind", 300, 5.0, List.of(northwindBatch));

        Map<String, TenantPolicy> policyMap = Map.of(
                "starter", starter,
                "growth", growth,
                "northwind", northwind
        );

        policyEngineService = new PolicyEngineService(policyMap);
    }

    @Test
    void testNorthwindOffPeakReturnsBaseCapacity() {
        // 01:30:00 UTC
        Instant instant = Instant.parse("2026-07-22T01:30:00Z");
        EffectivePolicy policy = policyEngineService.getEffectivePolicy("northwind", instant);

        assertNotNull(policy);
        assertEquals(300, policy.capacity(), "Off-peak capacity should be 300 RPM");
        assertEquals(5.0, policy.refillRateTokensPerSec());
    }

    @Test
    void testNorthwindBatchWindowReturnsBurstCapacity() {
        // 02:30:00 UTC (inside 02:00-04:00 batch window)
        Instant instant = Instant.parse("2026-07-22T02:30:00Z");
        EffectivePolicy policy = policyEngineService.getEffectivePolicy("northwind", instant);

        assertNotNull(policy);
        assertEquals(1200, policy.capacity(), "Batch window capacity should be 1200 RPM");
        assertEquals(20.0, policy.refillRateTokensPerSec());
    }

    @Test
    void testUnknownCustomerFallsBackToStarterTier() {
        Instant instant = Instant.parse("2026-07-22T10:00:00Z");
        EffectivePolicy policy = policyEngineService.getEffectivePolicy("unknown-customer-xyz", instant);

        assertNotNull(policy);
        assertEquals(60, policy.capacity(), "Unknown customer should fall back to Starter 60 RPM");
        assertEquals(1.0, policy.refillRateTokensPerSec());
    }

    @Test
    void testNullCustomerFallsBackToStarterTier() {
        Instant instant = Instant.parse("2026-07-22T10:00:00Z");
        EffectivePolicy policy = policyEngineService.getEffectivePolicy(null, instant);

        assertNotNull(policy);
        assertEquals(60, policy.capacity(), "Null customer should fall back to Starter 60 RPM");
    }
}
