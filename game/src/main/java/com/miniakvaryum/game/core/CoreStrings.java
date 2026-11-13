package com.miniakvaryum.game.core;

/**
 * User-visible texts that belong to no feature. Each feature keeps its own texts in its
 * own {@code ...Strings} class next to its screens.
 */
public final class CoreStrings {

    /** Name of the game as shown to the player (window title). */
    public static final String APP_TITLE = "Mini-Aquarium";

    /** Label of the button that leads back to the main screen. */
    public static final String BACK = "Back";

    /** Shown when the player triggers something whose rules are not written yet. */
    public static final String NOT_IMPLEMENTED = "Not implemented yet";

    private CoreStrings() {
    }
}
