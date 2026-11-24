package com.miniakvaryum.game.feature.bedtime;

import com.miniakvaryum.domain.feature.aquarium.WorldConfig;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeEnded;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeService;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeStarted;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * UI of the bedtime feature: the night color over the aquarium and the greetings
 * "Goodnight, little fish!" (once per night) and "Good morning, little fish!" (once per day,
 * when the app is opened between 07:00 and 12:00 or is open at 07:00).
 *
 * <p>Fish sleeping and the feeding lock belong to the aquarium (V10); minigames ask
 * {@code BedtimeService.isBedtime()} themselves.
 */
public final class BedtimeGameFeature implements GameFeature {

    /** Over the dirt (20), under the greeting card. */
    private static final int OVERLAY_Z = 30;
    /** Over everything in the aquarium, under the developer bar (100). */
    private static final int BANNER_Z = 90;

    @Override
    public String id() {
        return "bedtime";
    }

    @Override
    public void install(UiContext ui) {
        BedtimeService bedtime = ui.session().get(BedtimeService.class);
        WorldConfig size = ui.session().get(AquariumWorld.class).config().world();

        ui.main().addWorldLayer(OVERLAY_Z,
                new NightOverlay(ui.skin(), bedtime, (float) size.worldWidth(), (float) size.waterTop()));
        BedtimeBanner banner = new BedtimeBanner(ui.skin(), ui.shapes(), (float) size.worldWidth());
        ui.main().addWorldLayer(BANNER_Z, banner);
        ui.main().addWorldTapHandler((x, y) -> banner.handleTap((float) x, (float) y));

        // When the game is opened.
        if (bedtime.consumeGoodnight()) {
            banner.show(BedtimeBanner.Kind.GOODNIGHT);
        } else if (bedtime.consumeGoodMorning()) {
            banner.show(BedtimeBanner.Kind.GOOD_MORNING);
        }
        // At 21:00 and 07:00 while the game is open.
        ui.session().events().subscribe(BedtimeStarted.class, e -> {
            if (bedtime.consumeGoodnight()) {
                banner.show(BedtimeBanner.Kind.GOODNIGHT);
            }
        });
        ui.session().events().subscribe(BedtimeEnded.class, e -> {
            if (bedtime.consumeGoodMorning()) {
                banner.show(BedtimeBanner.Kind.GOOD_MORNING);
            }
        });
        ui.main().addDebugLine(() -> "Bedtime: " + (bedtime.isBedtime() ? "yes" : "no"));
    }
}
