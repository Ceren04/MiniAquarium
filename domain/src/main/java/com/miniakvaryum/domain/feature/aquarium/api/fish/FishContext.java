package com.miniakvaryum.domain.feature.aquarium.api.fish;

/** The situation a fish is in when it chooses its next action. Selects the column of weights. */
public enum FishContext {

    /** No free pellet within detection range. */
    NORMAL,

    /** A free pellet is within detection range, but too far away to eat. */
    FOOD_DETECTED,

    /** A free pellet is close enough to eat. */
    FOOD_IN_EATING_RANGE
}
