package util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {
    @Test
    void locksAfterFiveFailuresAndUnlocksAfterSuccess() {
        String key = "test-" + UUID.randomUUID();
        for (int i = 0; i < 5; i++) LoginAttemptLimiter.recordFailure(key);
        assertTrue(LoginAttemptLimiter.isLocked(key));
        LoginAttemptLimiter.recordSuccess(key);
        assertFalse(LoginAttemptLimiter.isLocked(key));
    }
}
