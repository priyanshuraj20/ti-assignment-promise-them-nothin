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

public class LoadHarnessApplication {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api/v1/ping";

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

        results.add(runScenario("1. Starter (60 RPM Limit)", baseUrl, "starter-client-harness", 65, 60, 5));
        results.add(runScenario("2. Northwind Off-Peak (300 RPM)", baseUrl, "northwind", 310, 300, 10));

        printSummaryTable(results);

        return results.stream().allMatch(ScenarioResult::passed);
    }

    public ScenarioResult runScenario(
            String scenarioName,
            String url,
            String customerId,
            int totalRequests,
            int expectedAdmitted,
            int expectedRejected
    ) {
        Map<Integer, Integer> statusCodeCounts = new ConcurrentHashMap<>();
        Map<String, Integer> nodeCounts = new ConcurrentHashMap<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            HttpClient client = HttpClient.newBuilder()
                    .executor(executor)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < totalRequests; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .header("X-Customer-Id", customerId)
                                .GET()
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        int status = response.statusCode();
                        statusCodeCounts.merge(status, 1, Integer::sum);

                        String node = response.headers().firstValue("X-Served-By").orElse("unknown");
                        nodeCounts.merge(node, 1, Integer::sum);
                    } catch (Exception e) {
                        statusCodeCounts.merge(500, 1, Integer::sum);
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

        boolean passed = (actual200 == expectedAdmitted) && (actual429 == expectedRejected);

        if (!passed) {
            System.out.printf("SCENARIO FAILURE [%s]: Expected 200=%d, 429=%d | Got 200=%d, 429=%d, statusCounts=%s%n",
                    scenarioName, expectedAdmitted, expectedRejected, actual200, actual429, statusCodeCounts);
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

    private void printSummaryTable(List<ScenarioResult> results) {
        System.out.println();
        System.out.println("=============================================================================================================");
        System.out.println("                                RELAYAPI RATE LIMITER BOUNDARY VERIFICATION                                 ");
        System.out.println("=============================================================================================================");
        System.out.printf("%-30s | %-12s | %-12s | %-20s | %-8s%n",
                "Scenario", "Expected (OK/429)", "Actual (200/429)", "Node Distribution", "Status");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        for (ScenarioResult res : results) {
            String expected = res.expectedAdmitted() + " / " + res.expectedRejected();
            String actual = res.actual200() + " / " + res.actual429();
            String status = res.passed() ? "PASSED" : "FAILED";
            String nodeDist = formatNodeDist(res.nodeCounts());

            System.out.printf("%-30s | %-17s | %-16s | %-20s | %-8s%n",
                    res.scenarioName(), expected, actual, nodeDist, status);
        }

        System.out.println("=============================================================================================================");
        boolean overallPass = results.stream().allMatch(ScenarioResult::passed);
        System.out.println("OVERALL VERIFICATION STATUS: " + (overallPass ? "ALL BOUNDARY PROOFS PASSED" : "SOME BOUNDARY PROOFS FAILED"));
        System.out.println("=============================================================================================================");
        System.out.println();
    }

    private String formatNodeDist(Map<String, Integer> nodeCounts) {
        if (nodeCounts.isEmpty()) return "N/A";
        StringBuilder sb = new StringBuilder();
        nodeCounts.forEach((node, count) -> {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(node).append(":").append(count);
        });
        return sb.toString();
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
