package com.miniakvaryum.domain.feature.economy.api;

/** The things a player can own an amount of. Both are tracked in the same ledger. */
public enum Currency {

    /** Money of the game, earned with tasks and spent in the shop. */
    COIN,

    /** Fish food. One unit is used up when a fish finishes eating one pellet. */
    FOOD
}
