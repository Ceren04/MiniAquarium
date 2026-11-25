package com.miniakvaryum.domain.feature.trophies.api;

/** What kind of thing an {@link ItemReward} gives. The inventory (V11) decides where it goes. */
public enum ItemKind {
    /** Something to put into the aquarium, for example a plant. */
    DECO,
    /** Something a fish can wear, for example a hat. */
    ACCESSORY,
    /** One part (head, body, fin or tail) of a special fish set, for the custom fish (V10). */
    FISH_PART,
    /** A whole new fish. */
    FISH
}
