package com.relayapi.ratelimiter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Import(TestRedisConfiguration.class)
class RedisIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testRedisReadWriteOperation() {
        String testKey = "relay:test:" + UUID.randomUUID();
        String testValue = "pong-" + System.currentTimeMillis();

        // Write to Redis
        redisTemplate.opsForValue().set(testKey, testValue);

        // Read from Redis
        String retrievedValue = redisTemplate.opsForValue().get(testKey);

        assertNotNull(retrievedValue, "Retrieved value from Redis should not be null");
        assertEquals(testValue, retrievedValue, "Retrieved value should match stored value");

        // Clean up
        redisTemplate.delete(testKey);
    }
}
