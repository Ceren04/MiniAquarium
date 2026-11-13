package com.miniakvaryum.game.feature.aquarium;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import com.miniakvaryum.domain.feature.aquarium.api.food.Pellet;

/**
 * Draws all pellets that are in the water. Pellets appear and disappear all the time, so
 * instead of one actor per pellet this single actor draws whatever the domain lists right now.
 */
final class PelletLayer extends Actor {

    private final FoodField field;
    private final Texture texture;

    PelletLayer(FoodField field, Texture texture) {
        this.field = field;
        this.texture = texture;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(1, 1, 1, parentAlpha);
        float half = texture.getWidth() / 2f;
        for (Pellet pellet : field.pellets()) {
            Vec2 position = pellet.position();
            batch.draw(texture, (float) position.x() - half, (float) position.y() - half);
        }
    }
}
