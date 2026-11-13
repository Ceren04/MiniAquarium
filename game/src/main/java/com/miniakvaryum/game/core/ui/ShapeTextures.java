package com.miniakvaryum.game.core.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Creates simple placeholder images in code, so the game runs without any artwork, and loads a
 * real image instead when the file exists in the {@code assets} folder.
 *
 * <p>All textures created here are owned by this object and disposed with it; callers must not
 * dispose them.
 */
public final class ShapeTextures implements Disposable {

    /** Where the tip of a triangle points. */
    public enum Tip { UP, RIGHT }

    private final List<Texture> owned = new ArrayList<>();

    /**
     * Loads {@code assets/<path>} if that file exists, otherwise uses the placeholder.
     * A missing file is never an error.
     *
     * @param path        path below the assets folder, for example {@code "fish/body.png"}
     * @param placeholder creates the stand-in image, normally one of the shape methods below
     */
    public Texture fileOr(String path, Supplier<Texture> placeholder) {
        FileHandle file = Gdx.files.internal(path);
        if (file.exists()) {
            return own(new Texture(file));
        }
        return placeholder.get();
    }

    /** A filled rectangle. */
    public Texture rect(int width, int height, Color color) {
        Pixmap pixmap = newPixmap(width, height);
        pixmap.setColor(color);
        pixmap.fill();
        return toTexture(pixmap);
    }

    /** A filled circle that touches all four edges of a square image. */
    public Texture circle(int diameter, Color color) {
        return ellipse(diameter, diameter, color);
    }

    /** A filled ellipse that touches all four edges of the image. */
    public Texture ellipse(int width, int height, Color color) {
        Pixmap pixmap = newPixmap(width, height);
        pixmap.setColor(color);
        double a = width / 2.0;
        double b = height / 2.0;
        // One horizontal line per pixel row; its half-length follows the ellipse equation.
        for (int row = 0; row < height; row++) {
            double dy = (row + 0.5 - b) / b;
            int half = (int) Math.round(a * Math.sqrt(Math.max(0, 1 - dy * dy)));
            pixmap.fillRectangle((int) Math.round(a) - half, row, half * 2, 1);
        }
        return toTexture(pixmap);
    }

    /**
     * A filled triangle. With {@link Tip#UP} the base is the bottom edge and the tip is at the
     * top center; with {@link Tip#RIGHT} the base is the left edge and the tip is at the right
     * center.
     */
    public Texture triangle(int width, int height, Color color, Tip tip) {
        Pixmap pixmap = newPixmap(width, height);
        pixmap.setColor(color);
        // Pixmap coordinates start at the TOP-left corner, y grows downwards.
        if (tip == Tip.UP) {
            pixmap.fillTriangle(0, height - 1, width - 1, height - 1, width / 2, 0);
        } else {
            pixmap.fillTriangle(0, 0, 0, height - 1, width - 1, height / 2);
        }
        return toTexture(pixmap);
    }

    private static Pixmap newPixmap(int width, int height) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        return pixmap;
    }

    private Texture toTexture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return own(texture);
    }

    private Texture own(Texture texture) {
        owned.add(texture);
        return texture;
    }

    @Override
    public void dispose() {
        owned.forEach(Texture::dispose);
        owned.clear();
    }
}
