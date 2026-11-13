package com.miniakvaryum.domain.feature.security.api;

/** Outcome of a PIN check. */
public enum VerifyStatus {

    /** The PIN was correct. */
    OK,

    /** The PIN was wrong; see {@link VerifyResult#attemptsLeft()}. */
    WRONG,

    /** PIN entry is locked; the PIN was not checked. See {@link VerifyResult#lockedRemainingMillis()}. */
    LOCKED,

    /** No PIN has been set yet. */
    NOT_SET
}
