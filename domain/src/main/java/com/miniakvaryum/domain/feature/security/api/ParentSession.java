package com.miniakvaryum.domain.feature.security.api;

import java.util.Objects;

/**
 * Remembers whether the parent area is open right now. Parent-only operations of other
 * features (creating or approving tasks) call {@link #requireUnlocked()} first.
 *
 * <p>The unlocked flag is runtime state: it is never saved, so every start of the game begins
 * locked.
 *
 * <p><b>SKELETON:</b> {@link #unlock} must be implemented (specification section 7.5).
 * Tests: {@code ParentSessionTest}.
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
     * <p>TODO: call {@code pins.verify(pin)}; the area is unlocked exactly when the status is
     * {@link VerifyStatus#OK}. Return the result unchanged so the UI can show its message.
     */
    public VerifyResult unlock(String pin) {
        throw new UnsupportedOperationException("TODO: ParentSession.unlock is not implemented yet");
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
