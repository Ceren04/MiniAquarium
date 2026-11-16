package com.miniakvaryum.domain.feature.security.api;

import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Sets and checks the parent PIN, including the lockout after too many wrong attempts.
 *
 * <p>Specification section 9.2. Tests: {@code PinServiceTest}.
 *
 * <p>Rules:
 * <ul>
 *   <li>A PIN is exactly {@code pinLength} digits ({@code [0-9]}); anything else throws
 *       {@link IllegalArgumentException}.</li>
 *   <li>Hash: {@code PBKDF2WithHmacSHA256} ({@code javax.crypto.SecretKeyFactory}), 16 random
 *       salt bytes from {@code java.security.SecureRandom}, 256-bit key, iteration count from
 *       the config. Compare hashes with {@code MessageDigest.isEqual}.</li>
 *   <li>Never store, log or print the PIN.</li>
 *   <li>All times are game time ({@link GameTime#nowMillis()}), never the device clock.</li>
 *   <li>Call {@code onChange.run()} after every change of the state, so that it gets saved.</li>
 * </ul>
 */
public final class PinService {

    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final long MILLIS_PER_SECOND = 1_000L;

    private final SecureRandom secureRandom = new SecureRandom();
    private final GameTime time;
    private final SecurityConfig config;
    private final Runnable onChange;

    /** Null while no PIN has been set. */
    private PinState state;

    /** Creates a service that tells nobody about changes (handy in unit tests). */
    public PinService(PinState stateOrNull, GameTime time, SecurityConfig config) {
        this(stateOrNull, time, config, () -> { });
    }

    /**
     * @param stateOrNull saved PIN state, or null if no PIN has been set
     * @param time        trusted game time, used for the lockout
     * @param config      PIN rules
     * @param onChange    called after every change of the PIN state
     */
    public PinService(PinState stateOrNull, GameTime time, SecurityConfig config, Runnable onChange) {
        this.state = stateOrNull;
        this.time = Objects.requireNonNull(time, "time");
        this.config = Objects.requireNonNull(config, "config");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /** Returns true if a PIN has been set. */
    public boolean isPinSet() {
        return state != null;
    }

    /** Returns the state to save, or empty while no PIN has been set. */
    public Optional<PinState> state() {
        return Optional.ofNullable(state);
    }

    /**
     * Sets the PIN for the first time.
     *
     * <p>Validates the format, creates a random salt, hashes the PIN, stores a new
     * {@link PinState} with all counters at 0 and calls {@code onChange}.
     *
     * @throws IllegalArgumentException if the PIN is not exactly {@code pinLength} digits
     * @throws IllegalStateException    if a PIN is already set (changing the PIN is not part of
     *                                  this milestone)
     */
    public void setPin(String pin) {
        requireValidFormat(pin);
        if (state != null) {
            throw new IllegalStateException("A PIN is already set; changing it is not supported");
        }
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        int iterations = config.pbkdf2Iterations();
        byte[] hash = hash(pin, salt, iterations);
        Base64.Encoder base64 = Base64.getEncoder();
        state = new PinState(base64.encodeToString(salt), base64.encodeToString(hash), iterations, 0, 0, 0L);
        onChange.run();
    }

    /**
     * Checks a PIN.
     *
     * <p>In this order:
     * <ol>
     *   <li>No PIN set: return {@link VerifyStatus#NOT_SET}.</li>
     *   <li>Locked ({@code now < lockedUntilGameMillis}): return {@link VerifyStatus#LOCKED} with
     *       the remaining time. Do NOT check the PIN, not even a correct one.</li>
     *   <li>Correct PIN: reset {@code failedAttempts} and {@code lockoutStage} to 0, return
     *       {@link VerifyStatus#OK}.</li>
     *   <li>Wrong PIN: increment {@code failedAttempts}. When it reaches {@code pinMaxAttempts}:
     *       {@code lockedUntil = now + lockoutSeconds[min(stage, last index)] * 1000}, increment
     *       {@code lockoutStage}, reset {@code failedAttempts} to 0, return LOCKED. Otherwise
     *       return {@link VerifyStatus#WRONG} with the attempts left.</li>
     * </ol>
     * A wrongly formatted PIN is not an attempt: it throws and changes nothing.
     *
     * @throws IllegalArgumentException if the PIN is not exactly {@code pinLength} digits
     */
    public VerifyResult verify(String pin) {
        requireValidFormat(pin);
        if (state == null) {
            return new VerifyResult(VerifyStatus.NOT_SET, config.pinMaxAttempts(), 0L);
        }
        long now = time.nowMillis();
        if (now < state.lockedUntilGameMillis()) {
            return new VerifyResult(VerifyStatus.LOCKED, 0, state.lockedUntilGameMillis() - now);
        }
        if (matches(pin)) {
            if (state.failedAttempts() != 0 || state.lockoutStage() != 0) {
                state = new PinState(state.saltB64(), state.hashB64(), state.iterations(),
                        0, 0, state.lockedUntilGameMillis());
                onChange.run();
            }
            return new VerifyResult(VerifyStatus.OK, config.pinMaxAttempts(), 0L);
        }
        return registerWrongAttempt(now);
    }

    /** Counts one wrong PIN and starts a lockout when the attempts are used up. */
    private VerifyResult registerWrongAttempt(long now) {
        int failed = state.failedAttempts() + 1;
        if (failed >= config.pinMaxAttempts()) {
            List<Integer> durations = config.lockoutSeconds();
            int index = Math.min(state.lockoutStage(), durations.size() - 1);
            long lockMillis = durations.get(index) * MILLIS_PER_SECOND;
            state = new PinState(state.saltB64(), state.hashB64(), state.iterations(),
                    0, state.lockoutStage() + 1, now + lockMillis);
            onChange.run();
            return new VerifyResult(VerifyStatus.LOCKED, 0, lockMillis);
        }
        state = new PinState(state.saltB64(), state.hashB64(), state.iterations(),
                failed, state.lockoutStage(), state.lockedUntilGameMillis());
        onChange.run();
        return new VerifyResult(VerifyStatus.WRONG, config.pinMaxAttempts() - failed, 0L);
    }

    /** Hashes the candidate with the stored salt and iteration count and compares in constant time. */
    private boolean matches(String pin) {
        Base64.Decoder base64 = Base64.getDecoder();
        byte[] salt = base64.decode(state.saltB64());
        byte[] expected = base64.decode(state.hashB64());
        return MessageDigest.isEqual(expected, hash(pin, salt, state.iterations()));
    }

    /**
     * Checks that the PIN is exactly {@code pinLength} ASCII digits. The message never contains
     * the PIN itself.
     */
    private void requireValidFormat(String pin) {
        if (pin == null || pin.length() != config.pinLength()) {
            throw new IllegalArgumentException("A PIN must be exactly " + config.pinLength() + " digits");
        }
        for (int i = 0; i < pin.length(); i++) {
            char c = pin.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("A PIN must be exactly " + config.pinLength() + " digits");
            }
        }
    }

    private static byte[] hash(String pin, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(HASH_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Every Java 17 runtime ships PBKDF2WithHmacSHA256; reaching this is a broken JDK.
            throw new IllegalStateException(HASH_ALGORITHM + " is not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}
