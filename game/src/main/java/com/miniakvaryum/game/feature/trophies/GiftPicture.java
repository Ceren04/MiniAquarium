package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;

/**
 * A closed present made of two pictures, the open box and its lid, so the lid can fly off when
 * the present opens. The lid is a bit wider than the box, like a real lid.
 */
final class GiftPicture extends WidgetGroup {

    /** Where the lid's bottom edge sits, as a part of the box height measured from the top. */
    private static final float LID_COVERS = 0.36f;

    private final Image box;
    private final Image lid;
    private final float boxRatio;
    private final float lidRatio;
    private final float lidWidthFactor;

    GiftPicture(Texture openBox, Texture lidTexture) {
        this.box = new Image(openBox);
        this.lid = new Image(lidTexture);
        this.boxRatio = openBox.getHeight() / (float) openBox.getWidth();
        this.lidRatio = lidTexture.getHeight() / (float) lidTexture.getWidth();
        this.lidWidthFactor = lidTexture.getWidth() / (float) openBox.getWidth();
        setTouchable(Touchable.disabled);
        addActor(box);
        addActor(lid);
    }

    /** The lid, to animate it. */
    Image lid() {
        return lid;
    }

    /** The box, to animate it. */
    Image box() {
        return box;
    }

    /** Height of the whole present for a given width. */
    float heightFor(float width) {
        float boxHeight = width * boxRatio;
        return boxHeight * (1f - LID_COVERS) + width * lidWidthFactor * lidRatio;
    }

    @Override
    public void layout() {
        // Fit into the given size, keep the shape, stand on the bottom, centered.
        float width = Math.min(getWidth(), getHeight() / (heightFor(1f)));
        float boxHeight = width * boxRatio;
        float x = (getWidth() - width) / 2f;
        box.setBounds(x, 0f, width, boxHeight);
        float lidWidth = width * lidWidthFactor;
        lid.setBounds(x + (width - lidWidth) / 2f, boxHeight * (1f - LID_COVERS), lidWidth, lidWidth * lidRatio);
        box.setOrigin(width / 2f, 0f);
        lid.setOrigin(lidWidth / 2f, 0f);
    }

    @Override
    public float getPrefWidth() {
        return 100f;
    }

    @Override
    public float getPrefHeight() {
        return heightFor(100f);
    }
}
