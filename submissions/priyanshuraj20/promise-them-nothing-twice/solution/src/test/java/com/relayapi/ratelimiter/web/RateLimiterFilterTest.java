package com.relayapi.ratelimiter.web;

import com.relayapi.ratelimiter.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRedisConfiguration.class)
@TestPropertySource(properties = {
        "ratelimiter.simulation.enabled=true",
        "ratelimiter.simulation.secret-token=test-secret-token"
})
class RateLimiterFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRequestInjectsRateLimitHeadersOnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/ping").header("X-Customer-Id", "growth"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "300"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"));
    }

    @Test
    void testRequestWithoutHeaderDefaultsToStarterTier() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "60"));
    }

    @Test
    void testExceedingQuotaReturns429AndRetryAfterHeader() throws Exception {
        String testCustomer = "test-429-user-" + UUID.randomUUID();

        // Starter tier has 60 tokens capacity
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/v1/ping").header("X-Customer-Id", testCustomer))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "60"));
        }

        // 61st request should trigger HTTP 429
        mockMvc.perform(get("/api/v1/ping").header("X-Customer-Id", testCustomer))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("X-RateLimit-Limit", "60"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testSimulatedTimeOverrideWithValidTokenResolvesBatchWindowCapacity() throws Exception {
        // 03:00 UTC falls inside Northwind scheduled batch window (02:00-04:00 UTC -> 1200 RPM)
        mockMvc.perform(get("/api/v1/ping")
                        .header("X-Customer-Id", "northwind")
                        .header("X-Simulated-Time", "2026-07-22T03:00:00Z")
                        .header("X-Harness-Token", "test-secret-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "1200"));
    }

    @Test
    void testSimulatedTimeHeaderWithoutValidTokenIsIgnored() throws Exception {
        // Without valid X-Harness-Token, simulated time header is ignored and off-peak limit (300) applies
        mockMvc.perform(get("/api/v1/ping")
                        .header("X-Customer-Id", "northwind")
                        .header("X-Simulated-Time", "2026-07-22T03:00:00Z")
                        .header("X-Harness-Token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "300"));
    }
}
