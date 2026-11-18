package com.miniakvaryum.game.feature.dirt;

import com.miniakvaryum.domain.feature.dirt.api.AquariumCleaned;
import com.miniakvaryum.domain.feature.dirt.api.DirtService;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * UI of the dirt feature: dirt spots on the glass that the child taps and rubs away.
 *
 * <p>Drawn at z 20, in front of the fish (10), because the dirt is on the glass. Register this
 * feature <b>after</b> the aquarium in {@code GameFeatures}: while feed mode is on, a tap drops
 * a pellet; otherwise a tap on a spot wipes it. Rubbing always wipes.
 */
public final class DirtGameFeature implements GameFeature {

    private static final int LAYER_Z = 20;
    private static final float CLEAN_MESSAGE_SECONDS = 2.5f;

    @Override
    public String id() {
        return "dirt";
    }

    @Override
    public void install(UiContext ui) {
        DirtService dirt = ui.session().get(DirtService.class);
        DirtTextures textures = new DirtTextures(ui.shapes());

        ui.main().addWorldLayer(LAYER_Z, new DirtLayer(dirt, textures));
        ui.main().addWorldTapHandler((x, y) -> dirt.hasSpotAt(x, y) && dirt.tap(x, y));
        ui.main().addWorldDragHandler((fromX, fromY, toX, toY) ->
                dirt.wipe(toX, toY, Math.hypot(toX - fromX, toY - fromY)));

        ui.session().events().subscribe(AquariumCleaned.class,
                event -> ui.main().showMessage(DirtStrings.ALL_CLEAN, CLEAN_MESSAGE_SECONDS));
        ui.main().addDebugLine(() -> "Dirt: " + dirt.spots().size() + " spots, " + dirt.levelPercent() + " %");
    }
}
