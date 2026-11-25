package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.domain.feature.trophies.api.TrophyService;
import com.miniakvaryum.domain.feature.trophies.api.TrophyUnlocked;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * UI of the trophy road: the "Trophies" button in the top bar (with the number of presents
 * waiting, "Trophies (2)"), the message "New trophy!" when an approval reaches a trophy, and the
 * {@link TrophyRoadScreen}.
 */
public final class TrophiesGameFeature implements GameFeature {

    private static final float MESSAGE_SECONDS = 3f;

    @Override
    public String id() {
        return "trophies";
    }

    @Override
    public void install(UiContext ui) {
        TrophyService trophies = ui.session().get(TrophyService.class);
        TrophyIcons icons = TrophyIcons.load(ui.shapes());
        RewardReveal reveal = new SimpleReveal(ui.skin());
        ui.nav().register(TrophyRoadScreen.ID, () -> new TrophyRoadScreen(ui, icons, reveal));

        TextButton button = ui.main().addButton(TrophyStrings.TROPHIES, () -> ui.nav().show(TrophyRoadScreen.ID));
        button.addAction(Actions.forever(Actions.run(() -> {
            int ready = trophies.readyCount();
            String label = ready > 0 ? String.format(TrophyStrings.TROPHIES_READY, ready) : TrophyStrings.TROPHIES;
            if (!label.contentEquals(button.getText())) {
                button.setText(label);
            }
        })));

        // Approvals happen in the parent area; the message waits there and shows on the main screen.
        ui.session().events().subscribe(TrophyUnlocked.class,
                e -> ui.main().showMessage(TrophyStrings.NEW_TROPHY, MESSAGE_SECONDS));
    }
}
