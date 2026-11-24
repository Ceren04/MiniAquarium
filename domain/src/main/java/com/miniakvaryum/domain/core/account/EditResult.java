package com.miniakvaryum.domain.core.account;

/** Outcome of a profile edit in {@link AccountService}. */
public enum EditResult {
    /** Saved. */
    OK,
    /** The current password was wrong; nothing changed. */
    WRONG_PASSWORD,
    /** The new value breaks a rule; nothing changed. */
    INVALID
}
