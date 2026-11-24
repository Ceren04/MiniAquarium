package com.miniakvaryum.domain.core.account;

import java.util.Objects;

/**
 * A salted hash of a password or of a safety color. The plain text is never stored.
 *
 * @param saltB64    random salt, Base64
 * @param hashB64    PBKDF2 hash, Base64
 * @param iterations PBKDF2 iteration count used for this hash
 */
public record Secret(String saltB64, String hashB64, int iterations) {

    public Secret {
        Objects.requireNonNull(saltB64, "saltB64");
        Objects.requireNonNull(hashB64, "hashB64");
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be >= 1");
        }
    }
}
