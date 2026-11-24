package com.miniakvaryum.domain.feature.energy.api;

/** Outcome of {@link EnergyService#spend}. */
public enum SpendResult {
    /** The energy was taken. */
    OK,
    /** Not enough energy; nothing changed. */
    NOT_ENOUGH,
    /** This refId was already spent; nothing changed (a second click on the same game). */
    ALREADY_APPLIED
}
