package com.miniakvaryum.game.feature.profile;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.miniakvaryum.game.core.ui.ShapeTextures;

/**
 * A fish drawn like the fish in the aquarium (body, head, tail, fin; the same image files when
 * they exist), for the profile. With {@code swim} it swims slowly from side to side inside its
 * box and turns at the edges; otherwise it stays still (the small fish in the choice strip).
 */
final class FishPortrait extends Group {

    private static final Color BODY = new Color(1.00f, 0.60f, 0.15f, 1f);
    private static final Color HEAD = new Color(0.90f, 0.45f, 0.10f, 1f);
    private static final Color FIN = new Color(0.95f, 0.50f, 0.10f, 1f);
    /** Size of the drawn fish before scaling. */
    private static final float FISH_WIDTH = 176f;
    private static final float FISH_HEIGHT = 84f;
    private static final float SPEED = 40f;
    private static final float BOB = 6f;

    private final Group fish = new Group();
    private final boolean swim;
    private final float scale;
    private float time;
    private int direction = 1;

    /**
     * @param width  box width
     * @param height box height
     * @param scale  size of the fish (1 = as in the aquarium)
     * @param swim   true: moves; false: stands in the middle
     */
    FishPortrait(ShapeTextures shapes, float width, float height, float scale, boolean swim) {
        this.swim = swim;
        this.scale = scale;
        setSize(width, height);
        setTouchable(Touchable.disabled);
        add(shapes.fileOr("fish/tail.png", () -> shapes.triangle(48, 56, FIN, ShapeTextures.Tip.RIGHT)), 0, 14, 48, 56);
        add(shapes.fileOr("fish/body.png", () -> shapes.ellipse(120, 64, BODY)), 30, 10, 120, 64);
        add(shapes.fileOr("fish/fin.png", () -> shapes.triangle(36, 24, FIN, ShapeTextures.Tip.UP)), 75, 60, 36, 24);
        add(shapes.fileOr("fish/head.png", () -> shapes.circle(44, HEAD)), 128, 20, 44, 44);
        fish.setSize(FISH_WIDTH, FISH_HEIGHT);
        fish.setOrigin(FISH_WIDTH / 2f, FISH_HEIGHT / 2f);
        fish.setScale(scale);
        fish.setPosition((width - FISH_WIDTH) / 2f, (height - FISH_HEIGHT) / 2f);
        addActor(fish);
    }

    private void add(Texture texture, float x, float y, float w, float h) {
        Image image = new Image(texture);
        image.setBounds(x, y, w, h);
        fish.addActor(image);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!swim) {
            return;
        }
        time += delta;
        float half = FISH_WIDTH * scale / 2f;
        float centerX = fish.getX() + FISH_WIDTH / 2f + direction * SPEED * delta;
        if (centerX + half > getWidth() - 10f) {
            direction = -1;
        } else if (centerX - half < 10f) {
            direction = 1;
        }
        fish.setX(centerX - FISH_WIDTH / 2f);
        fish.setY((getHeight() - FISH_HEIGHT) / 2f + MathUtils.sin(time * 2f) * BOB);
        fish.setScaleX(direction * scale);
    }
}
