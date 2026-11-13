package com.miniakvaryum.game.feature.aquarium;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import com.miniakvaryum.game.core.ui.ShapeTextures.Tip;

/**
 * The images of the aquarium. Each one is a placeholder drawn in code, unless a file with the
 * given name exists in the assets folder; then the file is used. Sizes are in world units.
 *
 * <pre>
 *   part    size       placeholder             file
 *   body    120 x 64   orange ellipse          assets/fish/body.png
 *   head     44 x 44   darker orange circle    assets/fish/head.png
 *   tail     48 x 56   triangle, tip right     assets/fish/tail.png
 *   fin      36 x 24   triangle, tip up        assets/fish/fin.png
 *   pellet   12 x 12   brown circle            assets/pellet.png
 * </pre>
 * All fish images are drawn for a fish that faces RIGHT; facing left is done by mirroring.
 */
final class AquariumTextures {

    static final Color WATER = new Color(0.55f, 0.80f, 0.95f, 1f);
    static final Color SAND = new Color(0.87f, 0.77f, 0.55f, 1f);

    private static final Color BODY = new Color(1.00f, 0.60f, 0.15f, 1f);
    private static final Color HEAD = new Color(0.90f, 0.45f, 0.10f, 1f);
    private static final Color FIN = new Color(0.95f, 0.50f, 0.10f, 1f);
    private static final Color PELLET = new Color(0.45f, 0.28f, 0.12f, 1f);

    final Texture body;
    final Texture head;
    final Texture tail;
    final Texture fin;
    final Texture pellet;

    AquariumTextures(ShapeTextures shapes) {
        body = shapes.fileOr("fish/body.png", () -> shapes.ellipse(120, 64, BODY));
        head = shapes.fileOr("fish/head.png", () -> shapes.circle(44, HEAD));
        tail = shapes.fileOr("fish/tail.png", () -> shapes.triangle(48, 56, FIN, Tip.RIGHT));
        fin = shapes.fileOr("fish/fin.png", () -> shapes.triangle(36, 24, FIN, Tip.UP));
        pellet = shapes.fileOr("pellet.png", () -> shapes.circle(12, PELLET));
    }
}
