package com.miniakvaryum.domain.feature.security.api;

import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import java.util.Objects;
import java.util.Optional;

/**
 * Sets and checks the parent PIN, including the lockout after too many wrong attempts.
 *
 * <p><b>SKELETON:</b> the constructor and the simple getters work; the methods marked TODO
 * must be implemented (specification section 7.5). Tests: {@code PinServiceTest}.
 *
 * <p>Rules for the implementation:
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
     * <p>TODO: validate the format, create a random salt, hash the PIN, store a new
     * {@link PinState} with all counters at 0, call {@code onChange}.
     *
     * @throws IllegalArgumentException if the PIN is not exactly {@code pinLength} digits
     * @throws IllegalStateException    if a PIN is already set (changing the PIN is not part of
     *                                  this milestone)
     */
    public void setPin(String pin) {
        throw new UnsupportedOperationException("TODO: PinService.setPin is not implemented yet");
    }

    /**
     * Checks a PIN.
     *
     * <p>TODO, in this order:
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
        throw new UnsupportedOperationException("TODO: PinService.verify is not implemented yet");
    }
}
