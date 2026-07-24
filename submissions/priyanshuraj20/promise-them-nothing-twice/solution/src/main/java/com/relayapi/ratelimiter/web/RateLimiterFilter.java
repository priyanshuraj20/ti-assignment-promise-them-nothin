package com.relayapi.ratelimiter.web;

import com.relayapi.ratelimiter.config.RateLimiterProperties;
import com.relayapi.ratelimiter.domain.model.RateLimitResult;
import com.relayapi.ratelimiter.service.PolicyEngineService;
import com.relayapi.ratelimiter.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    public static final String HEADER_CUSTOMER_ID = "X-Customer-Id";
    public static final String HEADER_SIMULATED_TIME = "X-Simulated-Time";
    public static final String HEADER_HARNESS_TOKEN = "X-Harness-Token";
    public static final String HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    private final RateLimiterService rateLimiterService;
    private final Clock clock;
    private final boolean simulationEnabled;
    private final String configuredToken;

    public RateLimiterFilter(
            RateLimiterService rateLimiterService,
            @Autowired(required = false) Clock clock,
            @Value("${ratelimiter.simulation.enabled:false}") boolean simulationEnabled,
            @Value("${ratelimiter.simulation.secret-token:harness-verification-token-2026}") String configuredToken
    ) {
        this.rateLimiterService = rateLimiterService;
        this.clock = (clock == null) ? Clock.systemUTC() : clock;
        this.simulationEnabled = simulationEnabled;
        this.configuredToken = (configuredToken != null) ? configuredToken.trim() : "";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String customerId = request.getHeader(HEADER_CUSTOMER_ID);
        if (customerId == null || customerId.isBlank()) {
            customerId = PolicyEngineService.DEFAULT_STARTER_TIER;
        }

        Instant requestInstant = resolveRequestInstant(request, customerId);

        RateLimitResult result = rateLimiterService.checkRateLimit(customerId, requestInstant);

        // Always attach rate limit headers to response
        response.setHeader(HEADER_LIMIT, String.valueOf(result.capacity()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remainingTokens()));
        response.setHeader(HEADER_RESET, String.valueOf(result.resetSeconds()));

        if (result.isAllowed()) {
            filterChain.doFilter(request, response);
        } else {
            // Exceeded quota -> HTTP 429 Short Circuit
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(result.resetSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Retry after %d seconds.\"}",
                    result.resetSeconds()
            ));
        }
    }

    private Instant resolveRequestInstant(HttpServletRequest request, String customerId) {
        Instant defaultInstant = clock.instant();

        String envSimEnabled = System.getenv("ENABLE_SIMULATION");
        boolean isSimEnabled = simulationEnabled || "true".equalsIgnoreCase(envSimEnabled != null ? envSimEnabled.trim() : "");

        if (isSimEnabled) {
            String simTimeHeader = request.getHeader(HEADER_SIMULATED_TIME);
            String tokenHeader = request.getHeader(HEADER_HARNESS_TOKEN);

            String activeToken = !configuredToken.isBlank()
                    ? configuredToken
                    : (System.getenv("SIMULATION_SECRET") != null ? System.getenv("SIMULATION_SECRET").trim() : "");

            if (simTimeHeader != null && !simTimeHeader.isBlank()
                    && Objects.equals(tokenHeader != null ? tokenHeader.trim() : null, activeToken)) {
                try {
                    Instant simulatedInstant = Instant.parse(simTimeHeader.trim());
                    log.warn("SIMULATION OVERRIDE ACTIVE: Customer '{}' resolved policy using simulated time header '{}' (parsed: {})",
                            customerId, simTimeHeader.trim(), simulatedInstant);
                    return simulatedInstant;
                } catch (DateTimeParseException e) {
                    log.warn("Invalid X-Simulated-Time header format '{}' for customer '{}', falling back to system clock",
                            simTimeHeader, customerId);
                }
            }
        }

        return defaultInstant;
    }
}
