package com.miniakvaryum.domain.feature.security.api;

import java.util.Objects;

/**
 * Remembers whether the parent area is open right now. Parent-only operations of other
 * features (creating or approving tasks) call {@link #requireUnlocked()} first.
 *
 * <p>The unlocked flag is runtime state: it is never saved, so every start of the game begins
 * locked.
 *
 * <p>Specification section 9.2. Tests: {@code ParentSessionTest}.
 */
public final class ParentSession {

    private final PinService pins;
    private boolean unlocked;

    public ParentSession(PinService pins) {
        this.pins = Objects.requireNonNull(pins, "pins");
    }

    /**
     * Tries to open the parent area with the given PIN.
     *
     * <p>Calls {@code pins.verify(pin)}; the area is unlocked exactly when the status is
     * {@link VerifyStatus#OK}. The result is returned unchanged so the UI can show its message.
     *
     * @throws IllegalArgumentException if the PIN is not exactly {@code pinLength} digits
     */
    public VerifyResult unlock(String pin) {
        VerifyResult result = pins.verify(pin);
        unlocked = result.status() == VerifyStatus.OK;
        return result;
    }

    /** Returns true while the parent area is open. */
    public boolean isUnlocked() {
        return unlocked;
    }

    /** Closes the parent area. Safe to call when it is already closed. */
    public void lock() {
        unlocked = false;
    }

    /**
     * Guard for parent-only operations.
     *
     * @throws ParentAccessException if the parent area is locked
     */
    public void requireUnlocked() {
        if (!unlocked) {
            throw new ParentAccessException();
        }
    }

    /**
     * Opens the parent area without a PIN. Package-private on purpose: only the test helper
     * {@code ParentSessions} (test sources, same package) can call it, so features that depend
     * on an unlocked parent area can be tested before and independently of the PIN logic.
     */
    void forceUnlockForTests() {
        unlocked = true;
    }
}
