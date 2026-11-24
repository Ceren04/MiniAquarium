package com.miniakvaryum.game.core;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.AccountService;
import com.miniakvaryum.game.core.account.AccountPictures;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.game.core.ui.MainLayout;
import com.miniakvaryum.game.core.ui.Navigator;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import java.nio.file.Path;
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
    private final Runnable restartFromSave;
    private final AccountService accounts;
    private final String accountId;
    private final AccountPictures pictures;
    private final Runnable signOut;

    /**
     * @param restartFromSave rebuilds the game from the save file; see {@link #restartFromSave()}
     * @param accounts        all accounts of the device
     * @param accountId       the signed-in account
     * @param pictures        profile pictures
     * @param signOut         saves, closes the game and shows the Welcome screen; see {@link #signOut()}
     */
    public UiContext(GameSession session, Skin skin, Navigator nav, MainLayout main, ShapeTextures shapes,
            Runnable restartFromSave, AccountService accounts, String accountId, AccountPictures pictures,
            Runnable signOut) {
        this.session = Objects.requireNonNull(session, "session");
        this.skin = Objects.requireNonNull(skin, "skin");
        this.nav = Objects.requireNonNull(nav, "nav");
        this.main = Objects.requireNonNull(main, "main");
        this.shapes = Objects.requireNonNull(shapes, "shapes");
        this.restartFromSave = Objects.requireNonNull(restartFromSave, "restartFromSave");
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.pictures = Objects.requireNonNull(pictures, "pictures");
        this.signOut = Objects.requireNonNull(signOut, "signOut");
    }

    /** The signed-in account as it is now (names, birthday, picture); read again after edits. */
    public Account account() {
        return accounts.find(accountId).orElseThrow();
    }

    /** All accounts of the device; for profile edits of the signed-in account. */
    public AccountService accounts() {
        return accounts;
    }

    /** The folder that holds the save (and the photo) of the signed-in account. */
    public Path saveFolder() {
        return accounts.folderOf(account());
    }

    /** Profile pictures (avatars, photos, first letters). */
    public AccountPictures pictures() {
        return pictures;
    }

    /**
     * Asks to sign out: between two frames the game is saved and closed, "Remember me" is
     * forgotten and the Welcome screen appears. Ask the child first ({@code SignOutConfirmScreen}).
     */
    public void signOut() {
        signOut.run();
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

    /**
     * Throws away the running game and builds it again from the save file, at the start of the
     * next frame, <b>without saving first</b>: everything that is not in the save file is lost.
     * Exactly the same steps run as when the app is started, including the offline time.
     *
     * <p>For developer tools only (for example time travel, which edits the save file and then
     * restarts). Game features must not use it.
     */
    public void restartFromSave() {
        restartFromSave.run();
    }
}
