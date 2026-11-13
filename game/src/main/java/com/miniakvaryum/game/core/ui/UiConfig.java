package com.miniakvaryum.game.core.ui;

/**
 * Sizes shared by all screens. Everything on screen is measured in these virtual units, never
 * in pixels; the viewport scales them to the real window or tablet.
 */
public final class UiConfig {

    /** Width of every screen in virtual units. */
    public static final float VIEW_WIDTH = 1280f;

    /** Height of every screen in virtual units. */
    public static final float VIEW_HEIGHT = 720f;

    /** Smallest allowed width and height of anything a child should touch. */
    public static final float MIN_TOUCH_SIZE = 64f;

    /** Space between neighbouring controls. */
    public static final float GAP = 12f;

    private UiConfig() {
    }
}
