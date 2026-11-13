package com.miniakvaryum.game.feature.security;

import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.PlaceholderScreen;

/**
 * The PIN gate in front of the parent area.
 *
 * <p><b>SKELETON</b> (specification section 8.5). To implement:
 * <ul>
 *   <li>If no PIN is set ({@code PinService.isPinSet()} is false): show
 *       {@link SecurityStrings#SET_PIN}, ask for the new PIN twice, call
 *       {@code PinService.setPin}, then continue as if the PIN had just been entered.</li>
 *   <li>Otherwise show {@link SecurityStrings#ENTER_PIN} and call
 *       {@code ParentSession.unlock(pin)} when OK is pressed.</li>
 *   <li>Input: an on-screen number pad (0 to 9, clear, OK; every key at least 64 x 64) and the
 *       digit keys of the keyboard. Show the typed digits as dots, never as numbers.</li>
 *   <li>Result WRONG: show {@link SecurityStrings#WRONG_PIN} with {@code attemptsLeft}.
 *       Result LOCKED: show {@link SecurityStrings#LOCKED} with the remaining seconds and
 *       refresh it in {@code update(delta)} so it counts down.</li>
 *   <li>On success: {@code ui.nav().show(afterUnlockScreenId)}.</li>
 *   <li>Clear the typed digits in {@code onShow()}.</li>
 * </ul>
 */
final class ParentGateScreen extends PlaceholderScreen {

    private final UiContext ui;
    private final String afterUnlockScreenId;

    /**
     * @param afterUnlockScreenId id of the screen to open after a correct PIN
     */
    ParentGateScreen(UiContext ui, String afterUnlockScreenId) {
        super(ui.skin(), ui.nav(), SecurityStrings.GATE_TITLE);
        this.ui = ui;
        this.afterUnlockScreenId = afterUnlockScreenId;
    }
}
