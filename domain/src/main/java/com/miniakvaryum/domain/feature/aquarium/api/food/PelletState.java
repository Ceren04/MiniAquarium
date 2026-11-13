package com.miniakvaryum.domain.feature.aquarium.api.food;

/** Whether a pellet is still available. */
public enum PelletState {

    /** Any fish may claim it. */
    FREE,

    /** One fish is eating it; no other fish may claim it. */
    CLAIMED
}
