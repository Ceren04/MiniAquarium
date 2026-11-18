package com.miniakvaryum.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.miniakvaryum.domain.DomainFeatures;
import com.miniakvaryum.domain.core.save.JsonFileSaveStore;
import com.miniakvaryum.domain.core.session.CoreConfig;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.session.GameSessionFactory;
import com.miniakvaryum.domain.core.time.BootIdProvider;
import com.miniakvaryum.domain.core.time.Clock;
import com.miniakvaryum.domain.core.util.JavaRandomSource;
import com.miniakvaryum.domain.core.util.UuidIdGenerator;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.MainScreen;
import com.miniakvaryum.game.core.ui.Navigator;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.util.Objects;

/**
 * Entry point of the game on every platform, and the composition root of the UI.
 *
 * <p>libGDX calls {@link #create()} once at start-up, {@link #render()} once per frame and
 * {@link #dispose()} once when the window closes. Platform modules (desktop, later android)
 * only create this object, give it their clock and boot id, and hand it to libGDX.
 */
public final class MiniAkvaryumGame extends Game {

    /** Longest time step given to the game rules, so a hiccup cannot make things jump. */
    private static final float MAX_FRAME_SECONDS = 1f / 15f;

    private final Clock clock;
    private final BootIdProvider bootIdProvider;

    private GameSession session;
    private Skin skin;
    private ShapeTextures shapes;
    private Navigator navigator;
    private boolean restartRequested;

    public MiniAkvaryumGame(Clock clock, BootIdProvider bootIdProvider) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.bootIdProvider = Objects.requireNonNull(bootIdProvider, "bootIdProvider");
    }

    @Override
    public void create() {
        skin = UiSkin.create();
        buildGame();
    }

    /**
     * Loads the save, installs every domain feature, then builds the UI and installs every UI
     * feature. Runs at start-up and again for {@link UiContext#restartFromSave()}.
     */
    private void buildGame() {
        // 1. Game rules: load the save and install every domain feature.
        session = GameSessionFactory.create(
                clock,
                bootIdProvider,
                new JsonFileSaveStore(JsonFileSaveStore.defaultDirectory()),
                // The clock reading is used only as the seed of the random numbers.
                new JavaRandomSource(clock.monotonicNanos()),
                new UuidIdGenerator(),
                CoreConfig.defaults(),
                DomainFeatures.all());

        // 2. UI core: placeholder images, screen switching, the empty main screen.
        shapes = new ShapeTextures();
        navigator = new Navigator(this);
        MainScreen mainScreen = new MainScreen(session, skin);
        navigator.register(Navigator.MAIN, () -> mainScreen);

        // 3. UI features: each one adds its screens, buttons and drawings.
        UiContext ui = new UiContext(session, skin, navigator, mainScreen, shapes, () -> restartRequested = true);
        for (GameFeature feature : GameFeatures.all()) {
            feature.install(ui);
        }

        navigator.showMain();
    }

    /** Throws the running game away (without saving) and builds it again from the save file. */
    private void restartFromSave() {
        setScreen(null);
        navigator.dispose();
        shapes.dispose();
        buildGame();
    }

    @Override
    public void render() {
        // A restart is done here, between frames, never in the middle of handling a click.
        if (restartRequested) {
            restartRequested = false;
            restartFromSave();
        }
        // Advance the game rules first, on every screen, then let the active screen draw.
        session.tick(Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_SECONDS));
        super.render();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (navigator != null) {
            navigator.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        if (session != null) {
            session.saveNow();
        }
    }
}
