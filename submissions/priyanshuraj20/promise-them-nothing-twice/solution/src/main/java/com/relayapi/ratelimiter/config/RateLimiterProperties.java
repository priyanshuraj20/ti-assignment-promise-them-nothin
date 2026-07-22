package com.relayapi.ratelimiter.config;

import com.relayapi.ratelimiter.domain.model.ScheduledWindow;
import com.relayapi.ratelimiter.domain.model.TenantPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "ratelimiter")
public class RateLimiterProperties {

    private Map<String, PolicyDto> policies = new HashMap<>();

    public Map<String, PolicyDto> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, PolicyDto> policies) {
        this.policies = policies;
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
