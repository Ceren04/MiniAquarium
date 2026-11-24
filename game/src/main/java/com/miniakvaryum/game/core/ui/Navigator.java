package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Knows all screens by a text id and switches between them.
 *
 * <p>A feature registers its screens under ids of its choice and other features open them by
 * id. Because only the id is shared, a feature never has to import a screen class of another
 * feature.
 *
 * <p>A screen is created the first time it is shown and then reused. Screens should therefore
 * refresh what they display in {@code show()} (for {@link BaseScreen}: in {@code onShow()}).
 */
public final class Navigator implements Disposable {

    /** Id of the main screen (the aquarium). */
    public static final String MAIN = "main";

    private final Game game;
    private final Map<String, Supplier<? extends Screen>> factories = new HashMap<>();
    private final Map<String, Screen> created = new HashMap<>();

    /** Screens shown before the current one, most recent first. */
    private final Deque<String> history = new ArrayDeque<>();
    private String current;

    public Navigator(Game game) {
        this.game = Objects.requireNonNull(game, "game");
    }

    /**
     * Makes a screen available under an id.
     *
     * @param id      for example {@code "tasks"}
     * @param factory creates the screen; called at most once, when the screen is first shown
     * @throws IllegalArgumentException if the id is already taken
     */
    public void register(String id, Supplier<? extends Screen> factory) {
        Objects.requireNonNull(id, "id");
        if (factories.putIfAbsent(id, Objects.requireNonNull(factory, "factory")) != null) {
            throw new IllegalArgumentException("A screen with id '" + id + "' is already registered");
        }
    }

    /**
     * Switches to the screen with the given id.
     *
     * @throws IllegalArgumentException if no screen has that id
     */
    public void show(String id) {
        Supplier<? extends Screen> factory = factories.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("No screen with id '" + id + "' is registered. "
                    + "Is the feature that provides it listed in GameFeatures?");
        }
        if (current != null && !current.equals(id)) {
            history.push(current);
        }
        current = id;
        game.setScreen(created.computeIfAbsent(id, key -> factory.get()));
    }

    /**
     * Goes back to the screen shown before the current one, for example from "Edit profile"
     * back to the profile; the main screen if there is none.
     */
    public void back() {
        String previous = history.isEmpty() ? MAIN : history.pop();
        current = null;
        show(previous);
    }

    /** Switches to the main screen. */
    public void showMain() {
        history.clear();
        current = null;
        show(MAIN);
    }

    @Override
    public void dispose() {
        created.values().forEach(Screen::dispose);
        created.clear();
    }
}
