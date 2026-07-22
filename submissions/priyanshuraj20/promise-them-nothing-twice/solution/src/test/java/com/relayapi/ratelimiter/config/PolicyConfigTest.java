package com.relayapi.ratelimiter.config;

import com.relayapi.ratelimiter.domain.model.ScheduledWindow;
import com.relayapi.ratelimiter.domain.model.TenantPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PolicyConfigTest {

    @Autowired
    private Map<String, TenantPolicy> tenantPolicies;

    @Test
    void testTenantPoliciesLoadedFromYaml() {
        assertNotNull(tenantPolicies, "Tenant policies map should be injected");
        assertTrue(tenantPolicies.containsKey("starter"), "Starter policy should exist");
        assertTrue(tenantPolicies.containsKey("growth"), "Growth policy should exist");
        assertTrue(tenantPolicies.containsKey("northwind"), "Northwind policy should exist");

        // Verify Starter policy
        TenantPolicy starter = tenantPolicies.get("starter");
        assertEquals(60, starter.baseCapacity());
        assertEquals(1.0, starter.baseRefillRateTokensPerSec());

        // Verify Northwind policy & batch window
        TenantPolicy northwind = tenantPolicies.get("northwind");
        assertEquals(300, northwind.baseCapacity());
        assertEquals(5.0, northwind.baseRefillRateTokensPerSec());
        assertEquals(1, northwind.scheduledWindows().size());

        Optional<ScheduledWindow> batchWindow = northwind.getActiveWindow(LocalTime.of(2, 30));
        assertTrue(batchWindow.isPresent(), "Batch window should be active at 02:30 UTC");
        assertEquals(1200, batchWindow.get().burstCapacity());
        assertEquals(20.0, batchWindow.get().burstRefillRateTokensPerSec());

        Optional<ScheduledWindow> offPeakWindow = northwind.getActiveWindow(LocalTime.of(10, 0));
        assertFalse(offPeakWindow.isPresent(), "Batch window should NOT be active at 10:00 UTC");
    }
}
