package com.miniakvaryum.game.core;

/**
 * The visible side of one feature: its screens, buttons, status texts and drawings.
 *
 * <p>This is the UI counterpart of {@code DomainFeature}. To add UI for a feature, implement
 * this interface and add one line to {@code GameFeatures}. To remove it, delete that line.
 *
 * <p>A game feature may use the domain services of its own and of other features (through
 * {@code ui.session().get(...)}), but never the classes of another <i>game</i> feature. Game
 * features meet only through the {@link UiContext}: the main screen's extension points and
 * screen ids.
 */
public interface GameFeature {

    /** A short, unique name such as {@code "tasks"}; used in log messages. */
    String id();

    /**
     * Called once at start-up, after the game session exists. Register screens, add buttons
     * and status texts to the main screen, add drawing layers.
     */
    void install(UiContext ui);
}
