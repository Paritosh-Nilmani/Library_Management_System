package util;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Process-local throttling; a shared deployment should replace this with a shared store. */
public final class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private static final ConcurrentMap<String, Attempt> ATTEMPTS = new ConcurrentHashMap<>();

    private LoginAttemptLimiter() { }

    public static boolean isLocked(String key) {
        Attempt attempt = ATTEMPTS.get(key);
        if (attempt == null) return false;
        synchronized (attempt) {
            if (attempt.lockedUntil == null) return false;
            if (Instant.now().isBefore(attempt.lockedUntil)) return true;
            ATTEMPTS.remove(key, attempt);
            return false;
        }
    }

    public static void recordFailure(String key) {
        pruneIfNeeded();
        ATTEMPTS.compute(key, (ignored, current) -> {
            Attempt next = current == null ? new Attempt() : current;
            synchronized (next) {
                Instant now = Instant.now();
                if (next.lockedUntil != null && !now.isBefore(next.lockedUntil)
                        || next.windowStarted != null && now.isAfter(next.windowStarted.plus(FAILURE_WINDOW))) next = new Attempt();
                if (next.windowStarted == null) next.windowStarted = now;
                next.failures++;
                next.updatedAt = now;
                if (next.failures >= MAX_FAILURES) next.lockedUntil = now.plus(LOCK_DURATION);
            }
            return next;
        });
    }

    public static void recordSuccess(String key) {
        ATTEMPTS.remove(key);
    }

    private static void pruneIfNeeded() {
        if (ATTEMPTS.size() < 5000) return;
        Instant cutoff = Instant.now().minus(Duration.ofHours(1));
        for (Map.Entry<String, Attempt> entry : ATTEMPTS.entrySet()) {
            Attempt attempt = entry.getValue();
            synchronized (attempt) {
                if (attempt.updatedAt != null && attempt.updatedAt.isBefore(cutoff)
                        && (attempt.lockedUntil == null || !Instant.now().isBefore(attempt.lockedUntil))) {
                    ATTEMPTS.remove(entry.getKey(), attempt);
                }
            }
        }
    }

    private static final class Attempt {
        private int failures;
        private Instant windowStarted;
        private Instant updatedAt;
        private Instant lockedUntil;
    }
}
