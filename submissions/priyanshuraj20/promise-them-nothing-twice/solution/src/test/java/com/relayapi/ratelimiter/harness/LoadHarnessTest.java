package com.relayapi.ratelimiter.harness;

import com.relayapi.ratelimiter.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestRedisConfiguration.class)
class LoadHarnessTest {

    @LocalServerPort
    private int port;

    @Test
    void testLoadHarnessExecutionAgainstLocalSpringBootPort() {
        String testUrl = "http://localhost:" + port + "/api/v1/ping";
        LoadHarnessApplication harness = new LoadHarnessApplication();

        // Run harness scenario for Starter tier (60 capacity, send 65 requests -> expect 60 admitted, 5 rejected)
        LoadHarnessApplication.ScenarioResult result = harness.runScenario(
                "Starter Quota Test",
                testUrl,
                "harness-test-user-boundary",
                65,
                60,
                5
        );

        assertTrue(result.passed(), "Harness should prove 60 admitted / 5 rejected boundary");
    }
}
