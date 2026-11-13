package com.miniakvaryum.domain.feature.security.api;

/**
 * Thrown when a parent-only operation is called while the parent area is locked.
 * This is a programming error: the UI must not offer parent actions before the PIN was entered.
 */
public final class ParentAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ParentAccessException() {
        super("The parent area is locked");
    }
}
