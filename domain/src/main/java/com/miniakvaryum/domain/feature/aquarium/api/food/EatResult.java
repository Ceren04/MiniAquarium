package com.miniakvaryum.domain.feature.aquarium.api.food;

/** Outcome of {@link FoodService#completeEating}. */
public enum EatResult {

    /** The pellet was eaten: one food was deducted and the fish's feed count went up. */
    OK,

    /** The pellet is not (or no longer) claimed by this fish; nothing changed. */
    CLAIM_LOST,

    /** The food stock was empty; the pellet was released and nothing was deducted. */
    INSUFFICIENT_FOOD
}
