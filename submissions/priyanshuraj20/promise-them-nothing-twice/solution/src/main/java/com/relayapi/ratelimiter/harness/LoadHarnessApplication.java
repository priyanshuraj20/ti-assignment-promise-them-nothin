package com.relayapi.ratelimiter.harness;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class LoadHarnessApplication {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api/v1/ping";
    private static final String HARNESS_TOKEN = "harness-verification-token-2026";
    private static final String OFF_PEAK_SIMULATED_TIME = "2026-07-22T12:00:00Z";
    private static final String BATCH_WINDOW_SIMULATED_TIME = "2026-07-22T03:00:00Z";

    public static void main(String[] args) {
        String baseUrl = (args.length > 0) ? args[0] : DEFAULT_BASE_URL;
        System.out.println("Starting RelayAPI Rate Limiter Boundary Verification Harness against: " + baseUrl);
        
        LoadHarnessApplication harness = new LoadHarnessApplication();
        boolean allPassed = harness.runAllScenarios(baseUrl);

        if (!allPassed) {
            System.exit(1);
        }
    }

    public boolean runAllScenarios(String baseUrl) {
        List<ScenarioResult> results = new ArrayList<>();
        String runId = UUID.randomUUID().toString().substring(0, 8);

        // Scenario 1: Starter Tier (60 capacity limit). 65 requests -> exactly 60 admitted, 5 rejected.
        results.add(runScenario("1. Starter (60 RPM Limit)", baseUrl, "starter-demo-" + runId, 65, 60, 5, OFF_PEAK_SIMULATED_TIME, HARNESS_TOKEN));
        pauseBetweenScenarios();

        // Scenario 2: Northwind Off-Peak Policy (300 RPM limit). 330 requests -> exactly 300 admitted, 30 rejected.
        results.add(runScenario("2. Northwind Off-Peak (300 RPM)", baseUrl, "northwind-offpeak-" + runId, 330, 300, 30, OFF_PEAK_SIMULATED_TIME, HARNESS_TOKEN));
        pauseBetweenScenarios();

        // Scenario 3: Northwind Scheduled Batch Window (1200 RPM limit during 02:00-04:00 UTC). 1260 requests -> exactly 1200 admitted, 60 rejected.
        results.add(runScenario("3. Northwind Batch (1200 RPM)", baseUrl, "northwind-batch-" + runId, 1260, 1200, 60, BATCH_WINDOW_SIMULATED_TIME, HARNESS_TOKEN));

        printSummaryTable(results);

        return results.stream().allMatch(ScenarioResult::passed);
    }

    private void pauseBetweenScenarios() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }

    public ScenarioResult runScenario(
            String scenarioName,
            String url,
            String customerId,
            int totalRequests,
            int expectedAdmitted,
            int expectedRejected
    ) {
        return runScenario(scenarioName, url, customerId, totalRequests, expectedAdmitted, expectedRejected, null, null);
    }

    public ScenarioResult runScenario(
            String scenarioName,
            String url,
            String customerId,
            int totalRequests,
            int expectedAdmitted,
            int expectedRejected,
            String simulatedTime,
            String harnessToken
    ) {
        Map<Integer, Integer> statusCodeCounts = new ConcurrentHashMap<>();
        Map<String, Integer> nodeCounts = new ConcurrentHashMap<>();
        Map<String, Integer> clientErrors = new ConcurrentHashMap<>();
        Semaphore semaphore = new Semaphore(50); // Restrain concurrent socket bursts to 50 active connections

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            HttpClient client = HttpClient.newBuilder()
                    .executor(executor)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < totalRequests; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        semaphore.acquire();
                        sendRequestWithRetry(client, url, customerId, simulatedTime, harnessToken, statusCodeCounts, nodeCounts, clientErrors);
                    } catch (InterruptedException ignored) {
                    } finally {
                        semaphore.release();
                    }
                }));
            }

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception ignored) {
                }
            }
        }

        int actual200 = statusCodeCounts.getOrDefault(200, 0);
        int actual429 = statusCodeCounts.getOrDefault(429, 0);

        // Strict boundary assertion logic:
        // Scenarios PASS ONLY IF actual admitted (200) EXACTLY equals expectedAdmitted AND actual rejected (429) EXACTLY equals expectedRejected.
        boolean passed = (actual200 == expectedAdmitted) && (actual429 == expectedRejected);

        if (!passed) {
            System.out.printf("SCENARIO FAILURE [%s]: Expected 200/429=%d/%d, got 200/429=%d/%d | Statuses=%s | ClientErrors=%s%n",
                    scenarioName, expectedAdmitted, expectedRejected, actual200, actual429, statusCodeCounts, clientErrors);
        }

        return new ScenarioResult(
                scenarioName,
                totalRequests,
                expectedAdmitted,
                expectedRejected,
                actual200,
                actual429,
                nodeCounts,
                passed
        );
    }

    private void sendRequestWithRetry(
            HttpClient client,
            String url,
            String customerId,
            String simulatedTime,
            String harnessToken,
            Map<Integer, Integer> statusCodeCounts,
            Map<String, Integer> nodeCounts,
            Map<String, Integer> clientErrors
    ) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-Customer-Id", customerId)
                        .timeout(Duration.ofSeconds(5))
                        .GET();

                if (simulatedTime != null && !simulatedTime.isBlank()) {
                    builder.header("X-Simulated-Time", simulatedTime);
                }
                if (harnessToken != null && !harnessToken.isBlank()) {
                    builder.header("X-Harness-Token", harnessToken);
                }

                HttpRequest request = builder.build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                statusCodeCounts.merge(status, 1, Integer::sum);

                String node = response.headers().firstValue("X-Served-By").orElse("unknown");
                nodeCounts.merge(node, 1, Integer::sum);
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    clientErrors.merge(e.getClass().getSimpleName() + ": " + e.getMessage(), 1, Integer::sum);
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    private void printSummaryTable(List<ScenarioResult> results) {
        System.out.println();
        System.out.println("=============================================================================================================");
        System.out.println("                                RELAYAPI RATE LIMITER BOUNDARY VERIFICATION                                 ");
        System.out.println("=============================================================================================================");
        System.out.printf("%-32s | %-12s | %-12s | %-35s | %-8s%n",
                "Scenario", "Expected (OK/429)", "Actual (200/429)", "Node Distribution", "Status");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        for (ScenarioResult res : results) {
            String expected = res.expectedAdmitted() + " / " + res.expectedRejected();
            String actual = res.actual200() + " / " + res.actual429();
            String status = res.passed() ? "PASSED" : "FAILED";
            String nodeDist = formatNodeDist(res.nodeCounts());

            System.out.printf("%-32s | %-17s | %-16s | %-35s | %-8s%n",
                    res.scenarioName(), expected, actual, nodeDist, status);
        }

        System.out.println("=============================================================================================================");
        boolean allPassed = results.stream().allMatch(ScenarioResult::passed);
        System.out.println("OVERALL VERIFICATION STATUS: " + (allPassed ? "ALL BOUNDARY PROOFS PASSED" : "VERIFICATION FAILED"));
        System.out.println("=============================================================================================================");
        System.out.println();
    }

    private String formatNodeDist(Map<String, Integer> nodeCounts) {
        if (nodeCounts == null || nodeCounts.isEmpty()) {
            return "N/A";
        }
        List<String> parts = new ArrayList<>();
        nodeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> parts.add(e.getKey() + ":" + e.getValue()));
        return String.join(", ", parts);
    }

    public record ScenarioResult(
            String scenarioName,
            int totalRequests,
            int expectedAdmitted,
            int expectedRejected,
            int actual200,
            int actual429,
            Map<String, Integer> nodeCounts,
            boolean passed
    ) {}
}
