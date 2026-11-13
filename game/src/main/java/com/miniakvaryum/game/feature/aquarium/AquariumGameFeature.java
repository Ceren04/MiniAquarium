package com.miniakvaryum.game.feature.aquarium;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.miniakvaryum.domain.feature.aquarium.WorldConfig;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;
import com.miniakvaryum.game.core.ui.UiSkin;

/**
 * UI of the aquarium feature: water, sand, pellets and fish on the main screen, plus the feed
 * mode ("Feed" button, then tap into the water to drop a pellet).
 *
 * <p>Drawing order (z): water -100, sand -90, pellets 0, fish 10. Later features can slot
 * their own layers in between, for example decoration at -50 or dirt at 20.
 */
public final class AquariumGameFeature implements GameFeature {

    private static final float NO_FOOD_MESSAGE_SECONDS = 2f;

    private boolean feedMode;

    @Override
    public String id() {
        return "aquarium";
    }

    @Override
    public void install(UiContext ui) {
        AquariumWorld world = ui.session().get(AquariumWorld.class);
        WorldConfig size = world.config().world();
        AquariumTextures textures = new AquariumTextures(ui.shapes());

        ui.main().addWorldLayer(-100, rectangle(ui, AquariumTextures.WATER, 0, 0,
                (float) size.worldWidth(), (float) size.worldHeight()));
        ui.main().addWorldLayer(-90, rectangle(ui, AquariumTextures.SAND, 0, 0,
                (float) size.worldWidth(), (float) size.waterFloor()));
        ui.main().addWorldLayer(0, new PelletLayer(world.food(), textures.pellet));
        for (FishEntity fish : world.fish()) {
            ui.main().addWorldLayer(10, new FishActor(fish, textures));
        }

        ui.main().addToggle(AquariumStrings.FEED_BUTTON, on -> feedMode = on);
        ui.main().addWorldTapHandler((x, y) -> {
            if (!feedMode) {
                return false;
            }
            if (world.dropPellet(x).isEmpty()) {
                ui.main().showMessage(AquariumStrings.NO_FOOD_FREE, NO_FOOD_MESSAGE_SECONDS);
            }
            return true;
        });

        ui.main().addDebugLine(() -> "Fish: " + world.fish().size() + ", pellets: " + world.food().pellets().size());
    }

    private static Image rectangle(UiContext ui, Color color, float x, float y, float width, float height) {
        Image image = new Image(ui.skin().newDrawable(UiSkin.WHITE, color));
        image.setBounds(x, y, width, height);
        return image;
    }
}
