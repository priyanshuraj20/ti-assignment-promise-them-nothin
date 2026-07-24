package com.relayapi.ratelimiter.service;

import com.relayapi.ratelimiter.domain.model.EffectivePolicy;
import com.relayapi.ratelimiter.domain.model.TenantPolicy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Service
public class PolicyEngineService {

    public static final String DEFAULT_STARTER_TIER = "starter";
    private static final TenantPolicy FALLBACK_STARTER_POLICY = new TenantPolicy(
            DEFAULT_STARTER_TIER,
            60,
            1.0,
            Collections.emptyList()
    );

    private final Map<String, TenantPolicy> tenantPolicies;

    public PolicyEngineService(Map<String, TenantPolicy> tenantPolicies) {
        this.tenantPolicies = (tenantPolicies == null) ? Collections.emptyMap() : tenantPolicies;
    }

    public EffectivePolicy getEffectivePolicy(String customerId, Instant instant) {
        Objects.requireNonNull(instant, "instant cannot be null");
        LocalTime utcTime = instant.atZone(ZoneOffset.UTC).toLocalTime();

        TenantPolicy policy = null;
        if (customerId != null && !customerId.isBlank()) {
            String key = customerId.toLowerCase();
            policy = tenantPolicies.get(key);
            if (policy == null && key.startsWith("northwind")) {
                policy = tenantPolicies.get("northwind");
            }
        }

        if (policy == null) {
            policy = tenantPolicies.getOrDefault(DEFAULT_STARTER_TIER, FALLBACK_STARTER_POLICY);
        }

        return policy.getEffectivePolicyAt(utcTime);
    }
}
