package com.relayapi.ratelimiter.client;

import com.relayapi.ratelimiter.domain.model.TokenBucketResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class RedisTokenBucketClient {

    private static final String KEY_PREFIX = "ratelimit:";
    private static final long DEFAULT_TTL_SECONDS = 3600;

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<List> tokenBucketScript;

    public RedisTokenBucketClient(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        tokenBucketScript = new DefaultRedisScript<>();
        tokenBucketScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/token_bucket.lua")));
        tokenBucketScript.setResultType(List.class);
    }

    public TokenBucketResponse consumeToken(String customerId, Instant instant, long capacity, double refillRate) {
        Objects.requireNonNull(customerId, "customerId cannot be null");
        Objects.requireNonNull(instant, "instant cannot be null");

        String key = KEY_PREFIX + customerId.toLowerCase();
        double nowSeconds = instant.toEpochMilli() / 1000.0;

        @SuppressWarnings("unchecked")
        List<Long> result = (List<Long>) redisTemplate.execute(
                tokenBucketScript,
                List.of(key),
                String.valueOf(nowSeconds),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                "1",
                String.valueOf(DEFAULT_TTL_SECONDS)
        );

        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Redis Lua script returned unexpected result: " + result);
        }

        boolean allowed = (result.get(0) != null && result.get(0) == 1L);
        long remaining = (result.get(1) != null) ? result.get(1) : 0L;
        long resetSeconds = (result.get(2) != null) ? result.get(2) : 0L;

        return new TokenBucketResponse(allowed, remaining, resetSeconds);
    }
}
