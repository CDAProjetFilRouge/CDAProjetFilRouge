package fr.diginamic.hubevenementiel.services;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestRateLimiter {

    private static final int MAX_TRACKED_KEYS = 20_000;
    private static final long IDLE_FORGET_MILLIS = 10 * 60 * 1000;

    private final Clock clock;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Autowired
    public RequestRateLimiter() {
        this(Clock.systemUTC());
    }

    RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public long tryConsume(String key, int capacity, long refillMillis) {
        long now = clock.millis();
        if (buckets.size() > MAX_TRACKED_KEYS) {
            buckets.entrySet().removeIf(entry -> now - entry.getValue().lastRefillAt > IDLE_FORGET_MILLIS);
        }

        long[] retryAfter = new long[1];
        buckets.compute(key, (k, existing) -> {
            Bucket bucket = existing == null ? new Bucket(capacity, now) : existing;
            double refilled = (double) (now - bucket.lastRefillAt) * capacity / refillMillis;
            bucket.tokens = Math.min(capacity, bucket.tokens + refilled);
            bucket.lastRefillAt = now;
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
            } else {
                retryAfter[0] = (long) Math.ceil((1 - bucket.tokens) * refillMillis / capacity);
            }
            return bucket;
        });
        return retryAfter[0];
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillAt;

        private Bucket(double tokens, long lastRefillAt) {
            this.tokens = tokens;
            this.lastRefillAt = lastRefillAt;
        }
    }
}
