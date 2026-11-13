package com.miniakvaryum.domain.feature.security.api;

/**
 * Test helper: opens the parent area without a PIN, so features with parent-only operations
 * (tasks) can be tested independently of the PIN logic.
 */
public final class ParentSessions {

    private ParentSessions() {
    }

    /** Unlocks the given session as if the correct PIN had been entered. */
    public static void forceUnlock(ParentSession session) {
        session.forceUnlockForTests();
    }
}
