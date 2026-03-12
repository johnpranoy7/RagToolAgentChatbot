package com.vfu.chatbot.api;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ConditionalOnProperty(name = "resilience4j.ratelimiter.instances.rateLimitingApi.limit-for-period")
public class HealthController {

    @Autowired
    private RateLimiterRegistry registry;

    @GetMapping("/api/ratelimiter-status")
    public Map<String, Object> status() {
        var limiter = registry.rateLimiter("rateLimitingApi");
        return Map.of(
                "name", "rateLimitingApi",
                "limitPerMinute", limiter.getRateLimiterConfig().getLimitForPeriod(),
                "available", limiter.getMetrics().getAvailablePermissions(),
                "status", limiter.getMetrics().getAvailablePermissions() > 0 ? "OK" : "LIMITED"
        );
    }
}
