package com.miniakvaryum.domain.feature.security.api;

/**
 * Everything about the parent PIN that is saved. The PIN itself is never stored, only a
 * salted hash that cannot be turned back into the PIN.
 *
 * @param saltB64               random salt, Base64 encoded
 * @param hashB64               PBKDF2 hash of the PIN with that salt, Base64 encoded
 * @param iterations            PBKDF2 iteration count that was used for this hash
 * @param failedAttempts        wrong attempts since the last success or lockout
 * @param lockoutStage          how many lockouts happened in a row (selects the lock duration)
 * @param lockedUntilGameMillis game time until which PIN entry is locked; 0 if not locked
 */
public record PinState(String saltB64, String hashB64, int iterations,
        int failedAttempts, int lockoutStage, long lockedUntilGameMillis) {
}
