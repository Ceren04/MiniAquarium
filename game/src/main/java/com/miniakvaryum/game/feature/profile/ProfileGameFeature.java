package com.miniakvaryum.game.feature.profile;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.miniakvaryum.domain.core.account.Account;
import com.miniakvaryum.domain.core.account.AccountService;
import com.miniakvaryum.domain.core.account.Picture;
import com.miniakvaryum.domain.feature.stats.api.StatsService;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * UI of the profile: the child's picture at the very left of the top bar opens the profile
 * screen. Also counts today as a day played when the game starts.
 */
public final class ProfileGameFeature implements GameFeature {

    @Override
    public String id() {
        return "profile";
    }

    @Override
    public void install(UiContext ui) {
        ui.nav().register(ProfileScreen.ID, () -> new ProfileScreen(ui));
        Texture frame = ui.shapes().fileOr(BirthdayFrame.SMALL_FILE, () -> null);
        StatsService stats = ui.session().get(StatsService.class);
        // Days played use the real date, never the developer date picker (D-055).
        stats.dayPlayed(ui.accounts().realToday());
        ui.main().addStatusWidget(new ProfileButton(ui, frame, stats));
    }

    /**
     * The small round picture in the top bar; follows picture changes made on the profile. On the
     * birthday the picture is a bit smaller and wears the birthday frame, so both fit in the bar.
     */
    private static final class ProfileButton extends Container<Actor> {
        private static final float SIZE = 56f;
        private static final float BIRTHDAY_SIZE = 44f;
        /** How often the date is checked, so a new day counts while the game is open. */
        private static final float DAY_CHECK_SECONDS = 1f;

        private final UiContext ui;
        /** The birthday frame, or null when the image file is missing. */
        private final Texture frame;
        private final StatsService stats;
        private float sinceDayCheck;
        private boolean birthday;
        private Picture shown;
        private String shownName;
        private int photoVersion = -1;

        ProfileButton(UiContext ui, Texture frame, StatsService stats) {
            this.ui = ui;
            this.frame = frame;
            this.stats = stats;
            size(SIZE);
            setTouchable(Touchable.enabled);
            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    ui.nav().show(ProfileScreen.ID);
                }
            });
            refresh();
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            refresh();
            sinceDayCheck += delta;
            if (sinceDayCheck >= DAY_CHECK_SECONDS) {
                sinceDayCheck = 0f;
                stats.dayPlayed(ui.accounts().realToday());
            }
        }

        private void refresh() {
            Account account = ui.account();
            int version = ui.pictures().photoVersion();
            // The date can change while the game runs (midnight, or the developer date picker).
            boolean today = frame != null && AccountService.isBirthday(account.birthday(), ui.accounts().today());
            if (!account.picture().equals(shown) || !account.firstName().equals(shownName) || version != photoVersion
                    || today != birthday) {
                shown = account.picture();
                shownName = account.firstName();
                photoVersion = version;
                birthday = today;
                if (birthday) {
                    BirthdayFrame framed = new BirthdayFrame(
                            ui.pictures().of(account, ui.saveFolder(), BIRTHDAY_SIZE), BIRTHDAY_SIZE, frame);
                    size(framed.getPrefWidth(), framed.getPrefHeight());
                    setActor(framed);
                } else {
                    size(SIZE);
                    setActor(ui.pictures().of(account, ui.saveFolder(), SIZE));
                }
                invalidateHierarchy();
            }
        }
    }
}
