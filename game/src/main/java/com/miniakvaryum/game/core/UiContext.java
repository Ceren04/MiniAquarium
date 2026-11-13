package com.miniakvaryum.game.core;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.game.core.ui.MainLayout;
import com.miniakvaryum.game.core.ui.Navigator;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import java.util.Objects;

/**
 * Everything the UI core offers to a {@link GameFeature} and to its screens.
 */
public final class UiContext {

    private final GameSession session;
    private final Skin skin;
    private final Navigator nav;
    private final MainLayout main;
    private final ShapeTextures shapes;

    public UiContext(GameSession session, Skin skin, Navigator nav, MainLayout main, ShapeTextures shapes) {
        this.session = Objects.requireNonNull(session, "session");
        this.skin = Objects.requireNonNull(skin, "skin");
        this.nav = Objects.requireNonNull(nav, "nav");
        this.main = Objects.requireNonNull(main, "main");
        this.shapes = Objects.requireNonNull(shapes, "shapes");
    }

    /** The running game. Get domain services with {@code session().get(TaskService.class)}. */
    public GameSession session() {
        return session;
    }

    /** Shared look of labels, buttons and text fields. */
    public Skin skin() {
        return skin;
    }

    /** Registers screens and switches between them. */
    public Navigator nav() {
        return nav;
    }

    /** Extension points of the main screen: drawing layers, status texts, buttons, taps. */
    public MainLayout main() {
        return main;
    }

    /** Creates placeholder images in code and loads real images when they exist. */
    public ShapeTextures shapes() {
        return shapes;
    }
}
