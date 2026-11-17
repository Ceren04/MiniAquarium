package com.miniakvaryum.game.feature.dirt;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.miniakvaryum.domain.feature.dirt.api.DirtService;
import com.miniakvaryum.domain.feature.dirt.api.DirtSpot;

/**
 * Draws the dirt spots on the glass, in front of the fish. Like the pellet layer it draws
 * whatever the domain lists right now; a spot's strength is its opacity, so wiping fades it.
 */
final class DirtLayer extends Actor {

    /** Even a fresh spot lets a little of the aquarium shine through. */
    private static final float MAX_OPACITY = 0.9f;

    private final DirtService dirt;
    private final DirtTextures textures;

    DirtLayer(DirtService dirt, DirtTextures textures) {
        this.dirt = dirt;
        this.textures = textures;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        for (DirtSpot spot : dirt.spots()) {
            TextureRegion region = textures.region(spot.variant());
            float size = (float) spot.radius() * 2f;
            float half = size / 2f;
            batch.setColor(1f, 1f, 1f, (float) spot.strength() * MAX_OPACITY * parentAlpha);
            batch.draw(region, (float) spot.x() - half, (float) spot.y() - half, half, half, size, size,
                    1f, 1f, (float) spot.rotationDeg());
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
