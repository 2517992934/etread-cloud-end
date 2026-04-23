package com.etread.gateway.ratelimit;

import lombok.Data;

@Data
public class RateLimitDecision {

    private final boolean allowed;
    private final boolean locked;
    private final String message;

    private RateLimitDecision(boolean allowed, boolean locked, String message) {
        this.allowed = allowed;
        this.locked = locked;
        this.message = message;
    }

    public static RateLimitDecision allow() {
        return new RateLimitDecision(true, false, "OK");
    }

    public static RateLimitDecision reject(String message) {
        return new RateLimitDecision(false, false, message);
    }

    public static RateLimitDecision locked(String message) {
        return new RateLimitDecision(false, true, message);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isLocked() {
        return locked;
    }

    public String getMessage() {
        return message;
    }
}
