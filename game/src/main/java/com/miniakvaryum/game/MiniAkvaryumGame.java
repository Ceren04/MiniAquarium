package com.miniakvaryum.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.miniakvaryum.domain.DomainFeatures;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.AccountService;
import com.miniakvaryum.domain.core.account.AccountStore;
import com.miniakvaryum.domain.core.account.AccountsConfig;
import com.miniakvaryum.domain.core.account.PasswordHasher;
import com.miniakvaryum.domain.core.save.JsonFileSaveStore;
import com.miniakvaryum.domain.core.session.CoreConfig;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.session.GameSessionFactory;
import com.miniakvaryum.domain.core.time.AdjustableLocalClock;
import com.miniakvaryum.domain.core.time.BootIdProvider;
import com.miniakvaryum.domain.core.time.Clock;
import com.miniakvaryum.domain.core.util.JavaRandomSource;
import com.miniakvaryum.domain.core.util.UuidIdGenerator;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.PhotoSource;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.account.AccountFlow;
import com.miniakvaryum.game.core.account.AccountPictures;
import com.miniakvaryum.game.core.account.AccountScreens;
import com.miniakvaryum.game.core.ui.MainScreen;
import com.miniakvaryum.game.core.ui.Navigator;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import com.miniakvaryum.game.core.ui.UiSkin;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * Entry point of the game on every platform, and the composition root of the UI.
 *
 * <p>libGDX calls {@link #create()} once at start-up, {@link #render()} once per frame and
 * {@link #dispose()} once when the window closes. Platform modules (desktop, later android)
 * only create this object, give it their clock and boot id, and hand it to libGDX.
 *
 * <p><b>Accounts.</b> Before a game session exists, the account screens ({@link AccountFlow})
 * run: Welcome, Sign up, Sign in. With "Remember me" the remembered account starts directly.
 * Each account plays with the save in its own folder ({@code accounts/<id>}), so everything in
 * the game, the parent PIN included, belongs to one account.
 */
public final class MiniAkvaryumGame extends Game {

    /** Longest time step given to the game rules, so a hiccup cannot make things jump. */
    private static final float MAX_FRAME_SECONDS = 1f / 15f;

    private final Clock clock;
    private final BootIdProvider bootIdProvider;
    private final PhotoSource photoSource;
    /** Local time for bedtime; kept across restarts so a developer shift stays. */
    private final AdjustableLocalClock localClock;

    private GameSession session;
    private Skin skin;
    private ShapeTextures shapes;
    private Navigator navigator;
    private boolean restartRequested;

    private AccountService accounts;
    /** Profile pictures; one instance for the whole life of the game. */
    private AccountPictures pictures;
    /** The account whose game is running; null while the account screens are shown. */
    private Account account;
    /** The account screens; null while a game is running. */
    private AccountFlow accountFlow;
    /** Set by the account screens; the game for it is started between two frames. */
    private Account accountToStart;
    private boolean signOutRequested;

    public MiniAkvaryumGame(Clock clock, BootIdProvider bootIdProvider) {
        this(clock, bootIdProvider, PhotoSource.NONE);
    }

    /**
     * @param photoSource where profile photos come from (file window, later camera)
     */
    public MiniAkvaryumGame(Clock clock, BootIdProvider bootIdProvider, PhotoSource photoSource) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.bootIdProvider = Objects.requireNonNull(bootIdProvider, "bootIdProvider");
        this.photoSource = Objects.requireNonNull(photoSource, "photoSource");
        this.localClock = AdjustableLocalClock.ofSystemZone(clock);
    }

    @Override
    public void create() {
        skin = UiSkin.create();
        AccountsConfig accountsConfig = AccountsConfig.defaults();
        accounts = new AccountService(new AccountStore(JsonFileSaveStore.defaultDirectory()),
                new PasswordHasher(accountsConfig.pbkdf2Iterations()), clock, ZoneId.systemDefault(),
                new UuidIdGenerator(), accountsConfig);
        pictures = new AccountPictures(skin);
        Optional<Account> remembered = accounts.remembered();
        if (remembered.isPresent()) {
            startGame(remembered.get());
        } else {
            showAccountScreens();
        }
    }

    /** Shows Welcome / Sign up / Sign in. */
    private void showAccountScreens() {
        accountFlow = new AccountFlow(this, skin, accounts, pictures, signedIn -> accountToStart = signedIn);
        accountFlow.start();
    }

    /** Leaves the account screens and starts the game of the given account. */
    private void startGame(Account signedIn) {
        if (accountFlow != null) {
            setScreen(null);
            accountFlow.dispose();
            accountFlow = null;
        }
        account = signedIn;
        buildGame();
    }

    /** Saves and closes the running game, forgets "Remember me" and shows the Welcome screen. */
    private void signOut() {
        session.saveNow();
        setScreen(null);
        navigator.dispose();
        shapes.dispose();
        navigator = null;
        shapes = null;
        session = null;
        account = null;
        accounts.forget();
        showAccountScreens();
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
                new JsonFileSaveStore(accounts.folderOf(account)),
                // The clock reading is used only as the seed of the random numbers.
                new JavaRandomSource(clock.monotonicNanos()),
                new UuidIdGenerator(),
                CoreConfig.defaults(),
                DomainFeatures.all(localClock));

        // 2. UI core: placeholder images, screen switching, the empty main screen.
        shapes = new ShapeTextures();
        navigator = new Navigator(this);
        MainScreen mainScreen = new MainScreen(session, skin);
        navigator.register(Navigator.MAIN, () -> mainScreen);

        // 3. UI features: each one adds its screens, buttons and drawings.
        UiContext ui = new UiContext(session, skin, navigator, mainScreen, shapes, () -> restartRequested = true,
                accounts, account.id(), pictures, () -> signOutRequested = true);
        // Account screens used from the profile: edit, picture, safety color, sign-out question.
        AccountScreens.register(ui, photoSource);
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
        // Restart, sign-in and sign-out are done here, between frames, never in the middle of
        // handling a click.
        if (restartRequested) {
            restartRequested = false;
            restartFromSave();
        }
        if (accountToStart != null) {
            Account next = accountToStart;
            accountToStart = null;
            startGame(next);
        }
        if (signOutRequested) {
            signOutRequested = false;
            signOut();
        }
        // Advance the game rules first, on every screen, then let the active screen draw.
        if (session != null) {
            session.tick(Math.min(Gdx.graphics.getDeltaTime(), MAX_FRAME_SECONDS));
        }
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
        if (accountFlow != null) {
            accountFlow.dispose();
        }
        if (pictures != null) {
            pictures.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        if (session != null) {
            session.saveNow();
        }
    }
}
