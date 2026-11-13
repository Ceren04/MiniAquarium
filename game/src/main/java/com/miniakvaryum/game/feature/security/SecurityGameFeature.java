package com.miniakvaryum.game.feature.security;

import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;
import java.util.Objects;

/**
 * UI of the security feature: the "Parents" button on the main screen and the PIN gate.
 *
 * <p>The gate does not know what lies behind it: the id of the screen to open after a correct
 * PIN is passed in from {@code GameFeatures}. That keeps this feature independent of the tasks
 * feature, which owns the parent area.
 */
public final class SecurityGameFeature implements GameFeature {

    /** Id of the PIN gate screen. */
    public static final String SCREEN_GATE = "parent-gate";

    private final String afterUnlockScreenId;

    /**
     * @param afterUnlockScreenId id of the screen to open after a correct PIN
     */
    public SecurityGameFeature(String afterUnlockScreenId) {
        this.afterUnlockScreenId = Objects.requireNonNull(afterUnlockScreenId, "afterUnlockScreenId");
    }

    @Override
    public String id() {
        return "security";
    }

    @Override
    public void install(UiContext ui) {
        ui.nav().register(SCREEN_GATE, () -> new ParentGateScreen(ui, afterUnlockScreenId));
        ui.main().addButton(SecurityStrings.PARENTS_BUTTON, () -> ui.nav().show(SCREEN_GATE));
    }
}
