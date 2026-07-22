package com.relayapi.ratelimiter;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.Socket;

@TestConfiguration
public class TestRedisConfiguration {

    private static RedisServer redisServer;
    private static boolean startedByUs = false;

    @PostConstruct
    public synchronized void postConstruct() {
        if (isPortInUse("localhost", 6379)) {
            // Redis is already running (Docker or previously started instance)
            return;
        }
        if (redisServer == null) {
            try {
                redisServer = new RedisServer(6379);
                redisServer.start();
                startedByUs = true;
            } catch (Exception e) {
                System.out.println("Embedded Redis startup warning: " + e.getMessage());
            }
        }
    }

    @PreDestroy
    public synchronized void preDestroy() {
        if (startedByUs && redisServer != null && redisServer.isActive()) {
            try {
                redisServer.stop();
                redisServer = null;
                startedByUs = false;
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isPortInUse(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
