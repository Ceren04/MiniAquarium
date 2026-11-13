package com.miniakvaryum.domain.feature.security.api;

/**
 * Result of a PIN check, with the numbers the UI needs for its message
 * ("Wrong PIN, N attempts left" / "Locked, M seconds left").
 *
 * @param status                what happened
 * @param attemptsLeft          wrong attempts still allowed before the next lockout
 * @param lockedRemainingMillis remaining lock time in game milliseconds; 0 if not locked
 */
public record VerifyResult(VerifyStatus status, int attemptsLeft, long lockedRemainingMillis) {
}
