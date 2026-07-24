package com.relayapi.ratelimiter.config;

import com.relayapi.ratelimiter.domain.model.ScheduledWindow;
import com.relayapi.ratelimiter.domain.model.TenantPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimiterProperties {

    private Map<String, PolicyDto> policies = new HashMap<>();
    private SimulationDto simulation = new SimulationDto();

    public Map<String, PolicyDto> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, PolicyDto> policies) {
        this.policies = policies;
    }

    public SimulationDto getSimulation() {
        return simulation;
    }

    public void setSimulation(SimulationDto simulation) {
        this.simulation = simulation;
    }

    public Map<String, TenantPolicy> toTenantPolicyMap() {
        Map<String, TenantPolicy> map = new HashMap<>();
        if (policies != null) {
            policies.forEach((key, dto) -> {
                List<ScheduledWindow> windows = new ArrayList<>();
                if (dto.getScheduledWindows() != null) {
                    dto.getScheduledWindows().forEach(w -> {
                        windows.add(new ScheduledWindow(
                                LocalTime.parse(w.getStartTime()),
                                LocalTime.parse(w.getEndTime()),
                                w.getCapacity(),
                                w.getRefillRate()
                        ));
                    });
                }
                map.put(key, new TenantPolicy(
                        key,
                        dto.getCapacity(),
                        dto.getRefillRate(),
                        windows
                ));
            });
        }
        return Map.copyOf(map);
    }

    public static class SimulationDto {
        private boolean enabled = false;
        private String secretToken = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecretToken() {
            return secretToken;
        }

        public void setSecretToken(String secretToken) {
            this.secretToken = secretToken;
        }
    }

    public static class PolicyDto {
        private long capacity;
        private double refillRate;
        private List<WindowDto> scheduledWindows = new ArrayList<>();

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public double getRefillRate() {
            return refillRate;
        }

        public void setRefillRate(double refillRate) {
            this.refillRate = refillRate;
        }

        public List<WindowDto> getScheduledWindows() {
            return scheduledWindows;
        }

        public void setScheduledWindows(List<WindowDto> scheduledWindows) {
            this.scheduledWindows = scheduledWindows;
        }
    }

    public static class WindowDto {
        private String startTime;
        private String endTime;
        private long capacity;
        private double refillRate;

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public double getRefillRate() {
            return refillRate;
        }

        public void setRefillRate(double refillRate) {
            this.refillRate = refillRate;
        }
    }
}
