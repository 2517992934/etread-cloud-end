package com.etread.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "etread.gateway.ratelimit")
public class GatewayRateLimitProperties {

    private Map<String, Rule> rules = new HashMap<>();

    public Map<String, Rule> getRules() {
        return rules;
    }

    public void setRules(Map<String, Rule> rules) {
        this.rules = rules;
    }

    public static class Rule {
        private Integer windowSeconds;
        private Integer maxRequests;
        private Integer lockSeconds;
        private String keyStrategy;
        private String mode;

        public Integer getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(Integer windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public Integer getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(Integer maxRequests) {
            this.maxRequests = maxRequests;
        }

        public Integer getLockSeconds() {
            return lockSeconds;
        }

        public void setLockSeconds(Integer lockSeconds) {
            this.lockSeconds = lockSeconds;
        }

        public String getKeyStrategy() {
            return keyStrategy;
        }

        public void setKeyStrategy(String keyStrategy) {
            this.keyStrategy = keyStrategy;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }
}
