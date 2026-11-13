package com.miniakvaryum.domain.feature.security;

import java.util.List;

/**
 * Tunable numbers of the security feature (parent PIN).
 *
 * @param pinLength        number of digits of a PIN
 * @param pinMaxAttempts   wrong attempts allowed before the PIN entry is locked
 * @param pbkdf2Iterations cost of the PIN hash; higher is slower to guess and slower to check
 * @param lockoutSeconds   lock duration per lockout stage; the last value repeats
 */
public record SecurityConfig(int pinLength, int pinMaxAttempts, int pbkdf2Iterations, List<Integer> lockoutSeconds) {

    public SecurityConfig {
        if (pinLength < 1 || pinMaxAttempts < 1 || pbkdf2Iterations < 1) {
            throw new IllegalArgumentException("pinLength, pinMaxAttempts and pbkdf2Iterations must be >= 1");
        }
        lockoutSeconds = List.copyOf(lockoutSeconds);
        if (lockoutSeconds.isEmpty()) {
            throw new IllegalArgumentException("lockoutSeconds must not be empty");
        }
    }

    /** The values from the specification: 4 digits, 5 attempts, 120 000 iterations, 30/60/300/900 s. */
    public static SecurityConfig defaults() {
        return new SecurityConfig(4, 5, 120_000, List.of(30, 60, 300, 900));
    }
}
