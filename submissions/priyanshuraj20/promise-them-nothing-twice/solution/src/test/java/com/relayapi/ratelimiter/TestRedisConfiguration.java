package com.relayapi.ratelimiter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class TestRedisConfiguration {

    private RedisServer redisServer;

    public TestRedisConfiguration() throws IOException {
        this.redisServer = new RedisServer(6379);
    }

    @PostConstruct
    public void postConstruct() throws IOException {
        try {
            redisServer.start();
        } catch (Exception e) {
            // Redis may already be running locally or in container
            System.out.println("Embedded Redis start warning: " + e.getMessage());
        }
    }

    @PreDestroy
    public void preDestroy() throws IOException {
        try {
            if (redisServer != null && redisServer.isActive()) {
                redisServer.stop();
            }
        } catch (Exception ignored) {
        }
    }
}
