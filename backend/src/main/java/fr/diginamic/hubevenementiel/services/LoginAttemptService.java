package fr.diginamic.hubevenementiel.services;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.exceptions.TooManyRequestsException;

@Service
public class LoginAttemptService {

    private static final int MAX_TRACKED_KEYS = 10_000;
    private static final int MAX_DOUBLINGS = 20;

    private final Clock clock;
    private final int maxPerAccount;
    private final int maxPerIp;
    private final long windowMillis;
    private final long baseLockMillis;
    private final long maxLockMillis;
    private final long resetMillis;
    private final Map<String, AccountState> accounts = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> ipFailures = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(
            @Value("${app.security.login.max-attempts-per-account:5}") int maxPerAccount,
            @Value("${app.security.login.max-attempts-per-ip:30}") int maxPerIp,
            @Value("${app.security.login.window-seconds:900}") long windowSeconds,
            @Value("${app.security.login.base-lock-seconds:60}") long baseLockSeconds,
            @Value("${app.security.login.max-lock-seconds:1800}") long maxLockSeconds,
            @Value("${app.security.login.reset-seconds:3600}") long resetSeconds) {
        this(Clock.systemUTC(), maxPerAccount, maxPerIp, windowSeconds, baseLockSeconds, maxLockSeconds, resetSeconds);
    }

    LoginAttemptService(Clock clock, int maxPerAccount, int maxPerIp, long windowSeconds, long baseLockSeconds,
            long maxLockSeconds, long resetSeconds) {
        this.clock = clock;
        this.maxPerAccount = maxPerAccount;
        this.maxPerIp = maxPerIp;
        this.windowMillis = windowSeconds * 1000;
        this.baseLockMillis = baseLockSeconds * 1000;
        this.maxLockMillis = maxLockSeconds * 1000;
        this.resetMillis = resetSeconds * 1000;
    }

    public void checkAllowed(String email, String ip) throws TooManyRequestsException {
        long now = clock.millis();
        long retryAfterMillis = Math.max(accountRetryAfterMillis(accountKey(email), now),
                ipRetryAfterMillis(ipKey(ip), now));

        if (retryAfterMillis > 0) {
            long seconds = Math.max(1, (retryAfterMillis + 999) / 1000);
            throw new TooManyRequestsException(
                    "Trop de tentatives de connexion. Réessayez dans " + humanDuration(seconds) + ".", seconds);
        }
    }

    public void recordFailure(String email, String ip) {
        long now = clock.millis();
        if (accounts.size() > MAX_TRACKED_KEYS || ipFailures.size() > MAX_TRACKED_KEYS) {
            purgeExpired(now);
        }
        recordAccountFailure(accountKey(email), now);
        recordIpFailure(ipKey(ip), now);
    }

    public void recordSuccess(String email) {
        accounts.remove(accountKey(email));
    }

    private void recordAccountFailure(String key, long now) {
        accounts.compute(key, (k, existing) -> {
            AccountState state = existing;
            if (state == null || isExpired(state, now)) {
                state = new AccountState();
            }
            if (state.lockedUntil > now) {
                return state;
            }
            state.failures++;
            state.lastFailureAt = now;
            if (state.failures >= maxPerAccount) {
                int level = Math.min(state.failures - maxPerAccount, MAX_DOUBLINGS);
                long duration = Math.min(baseLockMillis << level, maxLockMillis);
                state.lockedUntil = now + duration;
            }
            return state;
        });
    }

    private long accountRetryAfterMillis(String key, long now) {
        long[] result = new long[1];
        accounts.computeIfPresent(key, (k, state) -> {
            if (isExpired(state, now)) {
                return null;
            }
            result[0] = Math.max(0, state.lockedUntil - now);
            return state;
        });
        return result[0];
    }

    private boolean isExpired(AccountState state, long now) {
        return now >= Math.max(state.lastFailureAt, state.lockedUntil) + resetMillis;
    }

    private void recordIpFailure(String key, long now) {
        ipFailures.compute(key, (k, deque) -> {
            Deque<Long> window = deque == null ? new ArrayDeque<>() : deque;
            pruneIp(window, now);
            window.addLast(now);
            return window;
        });
    }

    private long ipRetryAfterMillis(String key, long now) {
        long[] result = new long[1];
        ipFailures.computeIfPresent(key, (k, deque) -> {
            pruneIp(deque, now);
            if (deque.isEmpty()) {
                return null;
            }
            if (deque.size() >= maxPerIp) {
                result[0] = deque.peekFirst() + windowMillis - now;
            }
            return deque;
        });
        return result[0];
    }

    private void pruneIp(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() >= windowMillis) {
            deque.pollFirst();
        }
    }

    private void purgeExpired(long now) {
        accounts.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        ipFailures.entrySet().removeIf(entry -> {
            pruneIp(entry.getValue(), now);
            return entry.getValue().isEmpty();
        });
    }

    private String humanDuration(long seconds) {
        if (seconds < 60) {
            return seconds + (seconds > 1 ? " secondes" : " seconde");
        }
        long minutes = (seconds + 59) / 60;
        return minutes + (minutes > 1 ? " minutes" : " minute");
    }

    private String accountKey(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String ipKey(String ip) {
        return ip == null ? "" : ip;
    }

    private static final class AccountState {
        private int failures;
        private long lastFailureAt;
        private long lockedUntil;
    }
}
