package fr.diginamic.hubevenementiel.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.diginamic.hubevenementiel.logging.LogCapture;
import fr.diginamic.hubevenementiel.exceptions.TooManyRequestsException;

class LoginAttemptServiceTest {

    private static final String EMAIL = "alice@example.com";
    private static final String IP = "203.0.113.7";

    private MutableClock clock;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        service = new LoginAttemptService(clock, 5, 30, 900, 60, 1800, 3600);
    }

    private void fail(String email, String ip, int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure(email, ip);
        }
    }

    private long retryAfter(String email, String ip) {
        try {
            service.checkAllowed(email, ip);
            return 0;
        } catch (TooManyRequestsException exception) {
            return exception.getRetryAfterSeconds();
        }
    }

    @Test
    void allowsAttemptsBelowTheLimit() {
        fail(EMAIL, IP, 4);

        assertThatCode(() -> service.checkAllowed(EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    void fifthFailureLocksTheAccountForOneMinute() {
        fail(EMAIL, IP, 5);

        assertThatThrownBy(() -> service.checkAllowed(EMAIL, IP)).isInstanceOf(TooManyRequestsException.class);
        assertThat(retryAfter(EMAIL, IP)).isEqualTo(60);
    }

    @Test
    void aBlockedAttempt_isLoggedAsAWarningWithoutTheEmailOrTheIp() {
        fail(EMAIL, IP, 5);

        try (LogCapture logs = new LogCapture()) {
            assertThatThrownBy(() -> service.checkAllowed(EMAIL, IP)).isInstanceOf(TooManyRequestsException.class);

            assertThat(logs.warnings()).hasSize(1);
            assertThat(logs.warnings().get(0)).startsWith("Connexion bloquée : trop de tentatives")
                    .doesNotContain(EMAIL).doesNotContain(IP);
        }
    }

    @Test
    void lockEndsAfterItsDurationAndTheNextFailureDoublesIt() {
        fail(EMAIL, IP, 5);
        clock.advanceSeconds(60);

        assertThatCode(() -> service.checkAllowed(EMAIL, IP)).doesNotThrowAnyException();

        fail(EMAIL, IP, 1);

        assertThat(retryAfter(EMAIL, IP)).isEqualTo(120);
    }

    @Test
    void lockDurationDoublesThenIsCappedAtThirtyMinutes() {
        fail(EMAIL, IP, 5);
        long[] expected = { 60, 120, 240, 480, 960, 1800, 1800 };

        for (int i = 0; i < expected.length; i++) {
            assertThat(retryAfter(EMAIL, IP)).as("palier %d", i).isEqualTo(expected[i]);
            clock.advanceSeconds(expected[i]);
            fail(EMAIL, IP, 1);
        }
    }

    @Test
    void attemptsDuringALockDoNotExtendIt() {
        fail(EMAIL, IP, 5);
        clock.advanceSeconds(20);

        fail(EMAIL, IP, 3);

        assertThat(retryAfter(EMAIL, IP)).isEqualTo(40);
    }

    @Test
    void lockAppliesWhateverTheIp() {
        fail(EMAIL, IP, 5);

        assertThatThrownBy(() -> service.checkAllowed(EMAIL, "198.51.100.9"))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void accountKeyIgnoresCaseAndSpaces() {
        fail("Alice@Example.com", IP, 3);
        fail("  alice@example.com ", IP, 2);

        assertThatThrownBy(() -> service.checkAllowed(EMAIL, IP)).isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void otherAccountIsNotAffected() {
        fail(EMAIL, IP, 5);

        assertThatCode(() -> service.checkAllowed("bob@example.com", "198.51.100.9")).doesNotThrowAnyException();
    }

    @Test
    void successResetsTheProgression() {
        fail(EMAIL, IP, 5);
        clock.advanceSeconds(60);
        service.recordSuccess(EMAIL);

        fail(EMAIL, IP, 4);

        assertThatCode(() -> service.checkAllowed(EMAIL, IP)).doesNotThrowAnyException();
        fail(EMAIL, IP, 1);
        assertThat(retryAfter(EMAIL, IP)).isEqualTo(60);
    }

    @Test
    void aQuietHourForgetsPastFailures() {
        fail(EMAIL, IP, 4);
        clock.advanceSeconds(3600);

        fail(EMAIL, IP, 4);

        assertThatCode(() -> service.checkAllowed(EMAIL, IP)).doesNotThrowAnyException();
    }

    @Test
    void quietPeriodIsMeasuredFromTheEndOfTheLock() {
        fail(EMAIL, IP, 5);
        long[] steps = { 60, 120, 240, 480, 960, 1800 };
        for (long step : steps) {
            clock.advanceSeconds(step);
            fail(EMAIL, IP, 1);
        }
        clock.advanceSeconds(1799);

        assertThat(retryAfter(EMAIL, IP)).isEqualTo(1);
    }

    @Test
    void blocksAnIpThatSpraysManyAccounts() {
        for (int i = 0; i < 30; i++) {
            service.recordFailure("user" + i + "@example.com", IP);
        }

        assertThatThrownBy(() -> service.checkAllowed("never-seen@example.com", IP))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void ipBlockEndsWhenTheWindowPasses() {
        for (int i = 0; i < 30; i++) {
            service.recordFailure("user" + i + "@example.com", IP);
        }
        clock.advanceSeconds(900);

        assertThatCode(() -> service.checkAllowed("never-seen@example.com", IP)).doesNotThrowAnyException();
    }

    @Test
    void nullEmailAndIpAreHandled() {
        assertThatCode(() -> {
            service.recordFailure(null, null);
            service.checkAllowed(null, null);
        }).doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-14T12:00:00Z");

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
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
