package com.miniakvaryum.domain.core.account;

import java.util.List;

/**
 * Rules of sign-up, sign-in and password reset.
 *
 * @param nameMaxLength        longest first or last name (after trimming)
 * @param usernameMinLength    shortest username
 * @param usernameMaxLength    longest username
 * @param passwordMinLength    shortest password
 * @param passwordMaxLength    longest password
 * @param minAge               youngest allowed age in years on the day of sign-up
 * @param maxAge               oldest allowed age in years on the day of sign-up
 * @param signInMaxFailed      wrong passwords in a row before sign-in is locked
 * @param signInWaitSeconds    lock duration per lock stage; the last value repeats
 * @param safetyMaxFailed      wrong safety colors in a row before the reset is locked
 * @param safetyWaitSeconds    lock duration per lock stage; the last value repeats
 * @param pbkdf2Iterations     cost of the password and safety color hashes
 */
public record AccountsConfig(int nameMaxLength, int usernameMinLength, int usernameMaxLength,
        int passwordMinLength, int passwordMaxLength, int minAge, int maxAge,
        int signInMaxFailed, List<Integer> signInWaitSeconds,
        int safetyMaxFailed, List<Integer> safetyWaitSeconds, int pbkdf2Iterations) {

    public AccountsConfig {
        signInWaitSeconds = List.copyOf(signInWaitSeconds);
        safetyWaitSeconds = List.copyOf(safetyWaitSeconds);
        if (signInWaitSeconds.isEmpty() || safetyWaitSeconds.isEmpty()) {
            throw new IllegalArgumentException("wait lists must not be empty");
        }
        if (nameMaxLength < 1 || usernameMinLength < 1 || usernameMaxLength < usernameMinLength
                || passwordMinLength < 1 || passwordMaxLength < passwordMinLength || minAge < 0
                || maxAge < minAge || signInMaxFailed < 1 || safetyMaxFailed < 1 || pbkdf2Iterations < 1) {
            throw new IllegalArgumentException("invalid accounts config");
        }
    }

    /**
     * The values of the plan "Konten und Profil": names up to 30, username 3 to 15, password 6
     * to 64, age 3 to 17, 5 wrong passwords (30/60/300/900 s, as for the PIN), 3 wrong safety
     * colors (1/5/15/60 min), 120 000 hash iterations (as for the PIN).
     */
    public static AccountsConfig defaults() {
        return new AccountsConfig(30, 3, 15, 6, 64, 3, 17,
                5, List.of(30, 60, 300, 900),
                3, List.of(60, 300, 900, 3_600),
                120_000);
    }
}
