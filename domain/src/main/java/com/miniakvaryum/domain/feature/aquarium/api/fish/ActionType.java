package com.miniakvaryum.domain.feature.aquarium.api.fish;

/**
 * What a fish is doing. Every action has a fixed, short duration; a fish always finishes its
 * current action before it chooses the next one.
 *
 * <p>The order of the constants matters: the weighted random draw walks them in this order.
 */
public enum ActionType {

    /** Swim slowly in one direction. */
    SLOW_MOVE,

    /** Swim fast in one direction for a short time. */
    DASH,

    /** Stay in place. */
    IDLE,

    /** Eat a claimed pellet; the fish does not move. */
    EAT
}
