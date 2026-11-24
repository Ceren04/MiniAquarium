package com.miniakvaryum.game.feature.devtools;

import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;
import java.io.UncheckedIOException;
import java.util.OptionalLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Developer tools for trying the game by hand. NOT part of the product.
 *
 * <p>Press the toggle key ({@link DevBar#TOGGLE_KEY}) on the main screen to show or hide the
 * developer bar at the bottom of the screen. It offers:
 * <ul>
 *   <li><b>+5 food</b>: food is added through the normal {@link Wallet}, so the ledger rules
 *       still apply and the added food is saved like any other ledger entry.</li>
 *   <li><b>+1 h, +6 h, +24 h</b>: the game restarts as if it had been closed that long
 *       ({@link TimeTravel}). The bar stays open and shows how far the game is ahead.</li>
 *   <li><b>Undo time</b>: back to the moment before the first jump.</li>
 *   <li><b>Date</b>: opens the {@link DatePicker} to pretend another date, for example the
 *       child's birthday. The button then shows the pretend date.</li>
 * </ul>
 *
 * <p>Register this feature in {@code GameFeatures} <b>before</b> the aquarium feature: in feed
 * mode the aquarium takes every tap, and the first tap handler that returns true wins.
 */
public final class DevToolsGameFeature implements GameFeature {

    private static final Logger LOG = Logger.getLogger(DevToolsGameFeature.class.getName());

    private static final int FOOD_AMOUNT = 5;
    private static final String REASON = "dev";
    private static final String REF_PREFIX = "dev:food:";
    private static final int BAR_LAYER_Z = 100;
    private static final float MESSAGE_SECONDS = 2f;
    private static final float WIDE_BUTTON = 160f;
    private static final float NARROW_BUTTON = 110f;
    private static final long HOUR = 3_600_000L;
    private static final long MINUTE = 60_000L;

    @Override
    public String id() {
        return "dev-tools";
    }

    @Override
    public void install(UiContext ui) {
        Wallet wallet = ui.session().get(Wallet.class);
        float worldWidth = (float) ui.session().get(AquariumWorld.class).config().world().worldWidth();
        TimeTravel time = new TimeTravel(ui.session(), ui.saveFolder(), ui::restartFromSave);

        DevBar bar = new DevBar(ui.skin(), worldWidth, () -> aheadText(time));
        bar.addButton(DevToolsStrings.ADD_FOOD, WIDE_BUTTON, () -> addFood(ui, wallet));
        bar.addButton(DevToolsStrings.JUMP_1H, NARROW_BUTTON, () -> jump(ui, time, HOUR));
        bar.addButton(DevToolsStrings.JUMP_6H, NARROW_BUTTON, () -> jump(ui, time, 6 * HOUR));
        bar.addButton(DevToolsStrings.JUMP_24H, NARROW_BUTTON, () -> jump(ui, time, 24 * HOUR));
        bar.addButton(DevToolsStrings.UNDO_TIME, WIDE_BUTTON, () -> undo(ui, time));
        DatePicker picker = new DatePicker(ui.skin(), worldWidth, ui.accounts(), () -> ui.account().birthday());
        TextButton date = bar.addButton(DevToolsStrings.DATE, NARROW_BUTTON, picker::toggle);
        picker.setOnChange(() -> date.setText(picker.buttonText()));
        bar.addToPanel(picker);
        // After a jump the game was rebuilt; keep the bar open so the result can be seen.
        bar.setShown(time.hasCheckpoint());

        ui.main().addWorldLayer(BAR_LAYER_Z, bar);
        ui.main().addWorldTapHandler((x, y) -> bar.isShown() && picker.handleTap((float) x, (float) y)
                || bar.handleTap((float) x, (float) y));
    }

    private void addFood(UiContext ui, Wallet wallet) {
        // Every wallet transaction needs a refId that has never been used. Game time only goes
        // forward, also across restarts, so it makes each click unique.
        long now = ui.session().time().nowMillis();
        TxResult result = wallet.credit(Currency.FOOD, FOOD_AMOUNT, REASON, REF_PREFIX + now, now);
        if (result != TxResult.OK) {
            ui.main().showMessage(DevToolsStrings.NOT_ADDED, MESSAGE_SECONDS);
        }
    }

    private void jump(UiContext ui, TimeTravel time, long millis) {
        try {
            time.jump(millis);
        } catch (UncheckedIOException e) {
            LOG.log(Level.WARNING, "Time travel failed", e);
            ui.main().showMessage(DevToolsStrings.TIME_FAILED, MESSAGE_SECONDS);
        }
    }

    private void undo(UiContext ui, TimeTravel time) {
        try {
            if (!time.undo()) {
                ui.main().showMessage(DevToolsStrings.NOTHING_TO_UNDO, MESSAGE_SECONDS);
            }
        } catch (UncheckedIOException e) {
            LOG.log(Level.WARNING, "Undo of time travel failed", e);
            ui.main().showMessage(DevToolsStrings.TIME_FAILED, MESSAGE_SECONDS);
        }
    }

    private static String aheadText(TimeTravel time) {
        OptionalLong ahead = time.millisSinceCheckpoint();
        if (ahead.isEmpty()) {
            return "";
        }
        long millis = Math.max(0, ahead.getAsLong());
        return String.format(DevToolsStrings.AHEAD, millis / HOUR, (millis % HOUR) / MINUTE);
    }
}
