package com.miniakvaryum.game.core.account;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Disposable;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.AccountService;
import com.miniakvaryum.domain.core.account.SignUpDraft;
import com.miniakvaryum.game.core.ui.Navigator;
import java.util.Objects;

/**
 * The screens before the game starts: Welcome, Sign up (About you, Safety color, Your picture,
 * All set), Sign in and Forgot password. They run before any game session exists, with their
 * own {@link Navigator}. When the child is signed in, {@link Done#signedIn} is called and the
 * game builds the session of that account.
 *
 * <p>Nothing here touches the parent PIN: it stays in the save of each account and is set the
 * first time the parent area is opened, as before.
 */
public final class AccountFlow implements AccountUi, Disposable {

    /** Called once the child is signed in or has just signed up. */
    public interface Done {
        /**
         * @param account the account to start the game with
         */
        void signedIn(Account account);
    }

    static final String WELCOME = "welcome";
    static final String ABOUT_YOU = "about-you";
    static final String SAFETY_COLOR = "safety-color";
    static final String PICTURE = "picture";
    static final String ALL_SET = "all-set";
    static final String SIGN_IN = "sign-in";
    static final String FORGOT_PASSWORD = "forgot-password";

    private static final Color BACKGROUND = new Color(0.05f, 0.24f, 0.38f, 1f);

    private final Skin skin;
    private final AccountService accounts;
    private final Done done;
    private final AccountPictures pictures;
    private final Navigator nav;
    private TextField.TextFieldStyle fieldStyle;

    /** Sign-up data collected so far; only in memory. */
    private SignUpDraft draft = SignUpDraft.empty();
    /** Username typed on the sign-in screen, handed to "Forgot password?". */
    private String typedUsername = "";

    /**
     * @param pictures profile pictures; owned by the game, not disposed here
     */
    public AccountFlow(Game game, Skin skin, AccountService accounts, AccountPictures pictures, Done done) {
        this.skin = Objects.requireNonNull(skin, "skin");
        this.pictures = Objects.requireNonNull(pictures, "pictures");
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        this.done = Objects.requireNonNull(done, "done");
        this.nav = new Navigator(game);
        nav.register(WELCOME, () -> new WelcomeScreen(this));
        nav.register(ABOUT_YOU, () -> new AboutYouScreen(this));
        nav.register(SAFETY_COLOR, () -> new SafetyColorScreen(this));
        nav.register(PICTURE, () -> new PictureScreen(this));
        nav.register(ALL_SET, () -> new AllSetScreen(this));
        nav.register(SIGN_IN, () -> new SignInScreen(this));
        nav.register(FORGOT_PASSWORD, () -> new ForgotPasswordScreen(this));
    }

    /** Shows the Welcome screen. */
    public void start() {
        nav.show(WELCOME);
    }

    // ---------------------------------------------------------------- for the screens

    @Override
    public Skin skin() {
        return skin;
    }

    @Override
    public AccountService accounts() {
        return accounts;
    }

    /** Text field look of the account screens: the skin's look with some space inside. */
    @Override
    public TextField.TextFieldStyle fieldStyle() {
        if (fieldStyle == null) {
            fieldStyle = AccountScreen.paddedFieldStyle(skin);
        }
        return fieldStyle;
    }

    /** Profile pictures, avatars and circles. */
    @Override
    public AccountPictures pictures() {
        return pictures;
    }

    @Override
    public Color background() {
        return BACKGROUND;
    }

    void show(String screenId) {
        nav.show(screenId);
    }

    /** Starts a new sign-up with empty fields. */
    void startSignUp() {
        draft = SignUpDraft.empty();
        nav.show(ABOUT_YOU);
    }

    SignUpDraft draft() {
        return draft;
    }

    void setDraft(SignUpDraft newDraft) {
        draft = Objects.requireNonNull(newDraft, "newDraft");
    }

    String typedUsername() {
        return typedUsername;
    }

    void setTypedUsername(String username) {
        typedUsername = username == null ? "" : username;
    }

    /**
     * Ends the flow: applies "Remember me" and hands the account to the game.
     *
     * @param remember true: open this account directly next time; false: show Welcome next time
     */
    void finish(Account account, boolean remember) {
        if (remember) {
            accounts.remember(account.id());
        } else {
            accounts.forget();
        }
        draft = SignUpDraft.empty();
        done.signedIn(account);
    }

    @Override
    public void dispose() {
        nav.dispose();
    }
}
