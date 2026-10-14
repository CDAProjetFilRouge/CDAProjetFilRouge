package fr.diginamic.hubevenementiel.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {

    private static final long MINUTE = 60_000;

    private MutableClock clock;
    private RequestRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        limiter = new RequestRateLimiter(clock);
    }

    private int allowedOutOf(String key, int attempts, int capacity) {
        int allowed = 0;
        for (int i = 0; i < attempts; i++) {
            if (limiter.tryConsume(key, capacity, MINUTE) == 0) {
                allowed++;
            }
        }
        return allowed;
    }

    @Test
    void allowsUpToTheCapacityThenRefuses() {
        assertThat(allowedOutOf("a", 10, 3)).isEqualTo(3);
    }

    @Test
    void refusalGivesTheWaitBeforeTheNextToken() {
        allowedOutOf("a", 3, 3);

        assertThat(limiter.tryConsume("a", 3, MINUTE)).isEqualTo(20_000);
    }

    @Test
    void tokensComeBackProgressively() {
        allowedOutOf("a", 3, 3);
        clock.advanceMillis(20_000);

        assertThat(limiter.tryConsume("a", 3, MINUTE)).isZero();
        assertThat(limiter.tryConsume("a", 3, MINUTE)).isPositive();
    }

    @Test
    void bucketNeverExceedsItsCapacityAfterALongPause() {
        allowedOutOf("a", 3, 3);
        clock.advanceMillis(10 * MINUTE);

        assertThat(allowedOutOf("a", 10, 3)).isEqualTo(3);
    }

    @Test
    void keysAreIndependent() {
        allowedOutOf("a", 3, 3);

        assertThat(limiter.tryConsume("b", 3, MINUTE)).isZero();
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-14T12:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
