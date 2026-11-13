package com.miniakvaryum.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.miniakvaryum.game.MiniAkvaryumGame;
import com.miniakvaryum.game.core.CoreStrings;

/**
 * Starts the game in a desktop window. This is the only class with a {@code main} method.
 *
 * <p>Keep this class tiny: it configures the window, creates the platform-specific parts
 * (clock, boot id) and hands them to the game. Nothing else.
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(CoreStrings.APP_TITLE);
        config.setWindowedMode(1280, 720);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);

        MiniAkvaryumGame game = new MiniAkvaryumGame(new SystemClock(), new DesktopBootIdProvider());
        // Blocks until the window is closed.
        new Lwjgl3Application(game, config);
    }
}
