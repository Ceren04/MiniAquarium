package com.miniakvaryum.game.feature.devtools;

import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.miniakvaryum.domain.core.time.AdjustableLocalClock;
import com.miniakvaryum.domain.core.time.LocalClock;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeService;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.energy.api.EnergyService;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;
import java.io.UncheckedIOException;
import java.time.LocalTime;
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
 *   <li><b>Undo</b>: back to the moment before the first jump.</li>
 *   <li><b>E: 0 / E: 100</b>: sets the energy, to try the energy bar and the minigame
 *       limit (for example "E: 0", then "+1 h": the bar shows 10).</li>
 *   <li><b>Date</b>: opens the {@link DatePicker} to pretend another date, for example the
 *       child's birthday. The button then shows the pretend date.</li>
 *   <li><b>Clock</b>: real time, 21:00, 07:00: shifts only the game's local clock to try
 *       bedtime (TC-9) without touching the device clock.</li>
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
    private static final float WIDE_BUTTON = 125f;
    private static final float NARROW_BUTTON = 85f;
    private static final float UNDO_BUTTON = 100f;
    private static final float DATE_BUTTON = 105f;
    private static final float ENERGY_BUTTON = 90f;
    private static final long HOUR = 3_600_000L;
    private static final long MINUTE = 60_000L;
    private static final float CLOCK_BUTTON = 150f;
    private static final LocalTime BEDTIME_STARTS = LocalTime.of(21, 0);
    private static final LocalTime MORNING = LocalTime.of(7, 0);
    private static final int NOON = 12;

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
        bar.addButton(DevToolsStrings.UNDO_TIME, UNDO_BUTTON, () -> undo(ui, time));
        EnergyService energy = ui.session().get(EnergyService.class);
        bar.addButton(DevToolsStrings.ENERGY_0, ENERGY_BUTTON, () -> energy.setForDevelopment(0));
        bar.addButton(DevToolsStrings.ENERGY_100, ENERGY_BUTTON, () -> energy.setForDevelopment(energy.max()));
        DatePicker picker = new DatePicker(ui.skin(), worldWidth, ui.accounts(), () -> ui.account().birthday());
        TextButton date = bar.addButton(DevToolsStrings.DATE, DATE_BUTTON, picker::toggle);
        picker.setOnChange(() -> date.setText(picker.buttonText()));
        bar.addToPanel(picker);
        if (ui.session().get(LocalClock.class) instanceof AdjustableLocalClock clock) {
            addClockButton(bar, clock, ui.session().get(BedtimeService.class));
        }
        // After a jump the game was rebuilt; keep the bar open so the result can be seen.
        bar.setShown(time.hasCheckpoint());

        ui.main().addWorldLayer(BAR_LAYER_Z, bar);
        ui.main().addWorldTapHandler((x, y) -> bar.isShown() && picker.handleTap((float) x, (float) y)
                || bar.handleTap((float) x, (float) y));
    }

    /**
     * One button that cycles the local clock: real time, 21:00 (bedtime starts), 07:00 (morning),
     * real time again. At 21:00 and 07:00 the matching banner appears at once, also when it was
     * already shown today. Only the game's clock moves, never the device clock; the shift is not
     * saved, but it survives the restarts of the time travel.
     */
    private void addClockButton(DevBar bar, AdjustableLocalClock clock, BedtimeService bedtime) {
        TextButton[] button = new TextButton[1];
        String start = clock.isShifted() ? DevToolsStrings.CLOCK_SHIFTED : DevToolsStrings.CLOCK_REAL;
        button[0] = bar.addButton(start, CLOCK_BUTTON, () -> {
            if (!clock.isShifted()) {
                shiftTo(clock, button[0], BEDTIME_STARTS);
                bedtime.replayGreetingForDevelopment();
            } else if (clock.now().getHour() >= NOON) {
                shiftTo(clock, button[0], MORNING);
                bedtime.replayGreetingForDevelopment();
            } else {
                clock.resetForDevelopment();
                button[0].setText(DevToolsStrings.CLOCK_REAL);
            }
        });
    }

    private static void shiftTo(AdjustableLocalClock clock, TextButton button, LocalTime time) {
        clock.setTimeOfDayForDevelopment(time);
        button.setText(String.format(DevToolsStrings.CLOCK_AT, time));
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
