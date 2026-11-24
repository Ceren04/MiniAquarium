package com.miniakvaryum.game.feature.profile;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;

/**
 * The child's picture with the birthday frame ({@code assets/profile/birthday-frame.png}:
 * balloons, a present and a fish around a round hole) drawn on top. The frame is wider than the
 * picture, so this widget asks for the size of the whole frame and puts the picture in its hole.
 */
final class BirthdayFrame extends WidgetGroup {

    /** Path of the frame image below the assets folder. */
    static final String FILE = "profile/birthday-frame.png";
    /** The same frame, small, for the top bar (shrinking the big one that much looks jagged). */
    static final String SMALL_FILE = "profile/birthday-frame-small.png";

    /** Center of the hole, as a part of the image width and height (measured from the top left). */
    private static final float HOLE_X = 0.5217f;
    private static final float HOLE_Y = 0.4393f;
    /** Diameter of the hole as a part of the image width. */
    private static final float HOLE_SIZE = 0.5473f;

    private final Actor picture;
    private final Image frame;
    private final float pictureSize;
    private final float width;
    private final float height;

    BirthdayFrame(Actor picture, float pictureSize, Texture frameTexture) {
        this.picture = picture;
        this.pictureSize = pictureSize;
        frameTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.frame = new Image(frameTexture);
        this.frame.setTouchable(Touchable.disabled);
        this.width = pictureSize / HOLE_SIZE;
        this.height = width * frameTexture.getHeight() / frameTexture.getWidth();
        addActor(picture);
        addActor(frame);
        setSize(width, height);
    }

    @Override
    public void layout() {
        frame.setBounds(0, 0, width, height);
        float centerX = width * HOLE_X;
        float centerY = height * (1f - HOLE_Y);
        picture.setBounds(centerX - pictureSize / 2f, centerY - pictureSize / 2f, pictureSize, pictureSize);
    }

    @Override
    public float getPrefWidth() {
        return width;
    }

    @Override
    public float getPrefHeight() {
        return height;
    }
}
