package com.miniakvaryum.domain.core.account;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes passwords and safety colors with PBKDF2, the same method the parent PIN uses
 * ({@code PinService}), but as a separate class so that the security feature stays unchanged.
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final int iterations;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * @param iterations PBKDF2 iteration count for new hashes; existing hashes keep theirs
     */
    public PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be >= 1");
        }
        this.iterations = iterations;
    }

    /** Creates a new random salt and hashes the text with it. */
    public Secret hash(String text) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        Base64.Encoder base64 = Base64.getEncoder();
        return new Secret(base64.encodeToString(salt), base64.encodeToString(pbkdf2(text, salt, iterations)),
                iterations);
    }

    /** Returns true if the text has the given hash. Compares in constant time. */
    public boolean matches(Secret secret, String text) {
        Base64.Decoder base64 = Base64.getDecoder();
        byte[] expected = base64.decode(secret.hashB64());
        byte[] actual = pbkdf2(text, base64.decode(secret.saltB64()), secret.iterations());
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(String text, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(text.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            // Every Java 17 runtime ships PBKDF2WithHmacSHA256; reaching this is a broken JDK.
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}
