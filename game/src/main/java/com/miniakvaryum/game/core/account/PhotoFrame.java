package com.miniakvaryum.game.core.account;

/**
 * The math of "Adjust your photo", without any libGDX type so it can be tested: a photo is
 * shown behind a round frame of diameter {@code frame}; the child moves and zooms it.
 *
 * <p>Rules: at the start the photo is centered and just big enough to fill the circle; it can
 * never be smaller than that, and never moved so far that the circle shows anything but photo;
 * it can be zoomed up to {@link #MAX_ZOOM} times. {@link #crop()} gives the square of the photo
 * that is inside the circle.
 *
 * <p>Units: {@code offsetX}/{@code offsetY} are screen pixels from the circle center to the photo
 * center (y up); the crop is in photo pixels (y down, as in image files).
 */
public final class PhotoFrame {

    /** Largest zoom compared with the start. */
    public static final float MAX_ZOOM = 4f;

    private final int photoWidth;
    private final int photoHeight;
    private final float frame;
    private final float minScale;
    private float scale;
    private float offsetX;
    private float offsetY;

    /** A square in photo pixels. */
    public record Crop(int x, int y, int size) {
    }

    /**
     * @param photoWidth  width of the photo in pixels
     * @param photoHeight height of the photo in pixels
     * @param frame       diameter of the round frame on screen
     */
    public PhotoFrame(int photoWidth, int photoHeight, float frame) {
        if (photoWidth < 1 || photoHeight < 1 || frame <= 0) {
            throw new IllegalArgumentException("photo and frame must have a size");
        }
        this.photoWidth = photoWidth;
        this.photoHeight = photoHeight;
        this.frame = frame;
        this.minScale = frame / Math.min(photoWidth, photoHeight);
        this.scale = minScale;
    }

    /** Screen pixels per photo pixel. */
    public float scale() {
        return scale;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    /** Width of the photo on screen. */
    public float shownWidth() {
        return photoWidth * scale;
    }

    /** Height of the photo on screen. */
    public float shownHeight() {
        return photoHeight * scale;
    }

    /** Moves the photo by a drag, in screen pixels (y up). */
    public void move(float dx, float dy) {
        offsetX += dx;
        offsetY += dy;
        clamp();
    }

    /** Zooms by a factor (more than 1 = bigger), keeping the circle center on the same spot of the photo. */
    public void zoom(float factor) {
        float next = Math.max(minScale, Math.min(minScale * MAX_ZOOM, scale * factor));
        float ratio = next / scale;
        offsetX *= ratio;
        offsetY *= ratio;
        scale = next;
        clamp();
    }

    private void clamp() {
        float maxX = (shownWidth() - frame) / 2f;
        float maxY = (shownHeight() - frame) / 2f;
        offsetX = Math.max(-maxX, Math.min(maxX, offsetX));
        offsetY = Math.max(-maxY, Math.min(maxY, offsetY));
    }

    /** The square of the photo inside the circle, in photo pixels. */
    public Crop crop() {
        float size = frame / scale;
        float centerX = photoWidth / 2f - offsetX / scale;
        float centerY = photoHeight / 2f + offsetY / scale;
        int side = Math.max(1, Math.min(Math.round(size), Math.min(photoWidth, photoHeight)));
        int x = Math.round(centerX - side / 2f);
        int y = Math.round(centerY - side / 2f);
        x = Math.max(0, Math.min(photoWidth - side, x));
        y = Math.max(0, Math.min(photoHeight - side, y));
        return new Crop(x, y, side);
    }
}
