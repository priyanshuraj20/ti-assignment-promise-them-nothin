package com.relayapi.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestRedisConfiguration.class)
class RedisLuaScriptTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private DefaultRedisScript<List> tokenBucketScript;

    @BeforeEach
    void setUp() {
        tokenBucketScript = new DefaultRedisScript<>();
        tokenBucketScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/token_bucket.lua")));
        tokenBucketScript.setResultType(List.class);
    }

    @Test
    void testTokenBucketScriptAdmitAndDeplete() {
        String testKey = "ratelimit:test:" + UUID.randomUUID();
        double now = System.currentTimeMillis() / 1000.0;
        long capacity = 3;
        double refillRate = 1.0; // 1 token per sec
        long requested = 1;
        long ttl = 60;

        // Request 1: Admitted, remaining = 2
        List<Long> result1 = redisTemplate.execute(
                tokenBucketScript,
                List.of(testKey),
                String.valueOf(now),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(requested),
                String.valueOf(ttl)
        );
        assertNotNull(result1);
        assertEquals(1L, result1.get(0), "First request should be admitted");
        assertEquals(2L, result1.get(1), "Remaining tokens should be 2");

        // Request 2: Admitted, remaining = 1
        List<Long> result2 = redisTemplate.execute(
                tokenBucketScript,
                List.of(testKey),
                String.valueOf(now),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(requested),
                String.valueOf(ttl)
        );
        assertEquals(1L, result2.get(0));
        assertEquals(1L, result2.get(1));

        // Request 3: Admitted, remaining = 0
        List<Long> result3 = redisTemplate.execute(
                tokenBucketScript,
                List.of(testKey),
                String.valueOf(now),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(requested),
                String.valueOf(ttl)
        );
        assertEquals(1L, result3.get(0));
        assertEquals(0L, result3.get(1));

        // Request 4: Depleted! Rejected, remaining = 0, retry_after > 0
        List<Long> result4 = redisTemplate.execute(
                tokenBucketScript,
                List.of(testKey),
                String.valueOf(now),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(requested),
                String.valueOf(ttl)
        );
        assertEquals(0L, result4.get(0), "Fourth request should be rejected");
        assertEquals(0L, result4.get(1), "Remaining tokens should be 0");
        assertTrue(result4.get(2) > 0, "Retry after seconds should be > 0");

        // Clean up
        redisTemplate.delete(testKey);
    }
}
