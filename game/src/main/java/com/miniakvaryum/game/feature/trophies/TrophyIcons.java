package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.miniakvaryum.game.core.ui.ShapeTextures;

/**
 * The pictures of the road: gold trophy, gray trophy and present. They are simple drawings in
 * {@code assets/trophies/}; nicer pictures (V4) can replace the files with the same names. A
 * missing file becomes a colored circle.
 */
record TrophyIcons(Texture trophy, Texture locked, Texture gift) {

    private static final int PLACEHOLDER = 128;

    static TrophyIcons load(ShapeTextures shapes) {
        return new TrophyIcons(
                smooth(shapes.fileOr("trophies/trophy.png", () -> shapes.circle(PLACEHOLDER, Color.GOLD))),
                smooth(shapes.fileOr("trophies/trophy-locked.png", () -> shapes.circle(PLACEHOLDER, Color.GRAY))),
                smooth(shapes.fileOr("trophies/gift.png", () -> shapes.circle(PLACEHOLDER, Color.PINK))));
    }

    private static Texture smooth(Texture texture) {
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }
}
