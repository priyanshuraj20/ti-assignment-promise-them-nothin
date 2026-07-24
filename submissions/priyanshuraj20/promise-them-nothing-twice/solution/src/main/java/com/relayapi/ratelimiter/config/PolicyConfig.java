package com.relayapi.ratelimiter.config;

import com.relayapi.ratelimiter.domain.model.TenantPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class PolicyConfig {

    @Bean
    public Map<String, TenantPolicy> tenantPolicies(RateLimiterProperties properties) {
        return properties.toTenantPolicyMap();
    }
}
