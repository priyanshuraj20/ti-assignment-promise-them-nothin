package com.relayapi.ratelimiter.config;

import com.relayapi.ratelimiter.domain.model.TenantPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
public class PolicyConfig {

    @Bean
    public Map<String, TenantPolicy> tenantPolicies(RateLimiterProperties properties) {
        return properties.toTenantPolicyMap();
    }
}
