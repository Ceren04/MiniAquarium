package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The extension points of the main screen. The main screen itself is empty; everything on it
 * is put there by features through this interface.
 *
 * <pre>
 *  +--------------------------------------------------------------+
 *  | status  status            [toggle] [button] [button]         |  top bar
 *  +--------------------------------------------------------------+
 *  |                                                              |
 *  |                    world layers (by z)                       |  taps go to the
 *  |                                                              |  WorldTapHandlers
 *  +--------------------------------------------------------------+
 * </pre>
 */
public interface MainLayout {

    /**
     * Adds something that is drawn in the aquarium. World units equal stage units, so an actor
     * at (100, 200) is drawn at world position (100, 200).
     *
     * @param z     drawing order: lower values are drawn first (further back)
     * @param actor a Scene2D actor or group; use {@code act(delta)} to follow the domain state
     */
    void addWorldLayer(int z, Actor actor);

    /**
     * Adds a text to the top bar that is refreshed every frame, for example the coin balance.
     *
     * @param text returns the text to show now
     */
    void addStatus(Supplier<String> text);

    /**
     * Adds a button to the top bar.
     *
     * @return the button, in case the feature wants to change its label later
     */
    TextButton addButton(String label, Runnable onClick);

    /**
     * Adds a button that stays highlighted while switched on, for example "feed mode".
     *
     * @param onToggle receives the new state after every click
     */
    TextButton addToggle(String label, Consumer<Boolean> onToggle);

    /**
     * Registers a handler for taps into the aquarium. Handlers are asked in registration order
     * until one returns true.
     */
    void addWorldTapHandler(WorldTapHandler handler);

    /** Adds a line to the F3 debug overlay. */
    void addDebugLine(Supplier<String> line);

    /** Shows a short text near the bottom of the main screen. */
    void showMessage(String text, float seconds);
}
