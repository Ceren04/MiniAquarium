package com.miniakvaryum.game.core;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.miniakvaryum.domain.core.account.Account;
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
    private final Account account;
    private final Path saveFolder;

    /**
     * @param restartFromSave rebuilds the game from the save file; see {@link #restartFromSave()}
     * @param account         the signed-in account
     * @param saveFolder      the folder of this account's save
     */
    public UiContext(GameSession session, Skin skin, Navigator nav, MainLayout main, ShapeTextures shapes,
            Runnable restartFromSave, Account account, Path saveFolder) {
        this.session = Objects.requireNonNull(session, "session");
        this.skin = Objects.requireNonNull(skin, "skin");
        this.nav = Objects.requireNonNull(nav, "nav");
        this.main = Objects.requireNonNull(main, "main");
        this.shapes = Objects.requireNonNull(shapes, "shapes");
        this.restartFromSave = Objects.requireNonNull(restartFromSave, "restartFromSave");
        this.account = Objects.requireNonNull(account, "account");
        this.saveFolder = Objects.requireNonNull(saveFolder, "saveFolder");
    }

    /** The signed-in account: names, birthday and picture. */
    public Account account() {
        return account;
    }

    /** The folder that holds the save of the signed-in account. */
    public Path saveFolder() {
        return saveFolder;
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
