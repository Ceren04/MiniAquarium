package com.miniakvaryum.domain.feature.aquarium;

/**
 * Size of the aquarium in world units. The origin is the bottom-left corner and y points up.
 *
 * @param worldWidth  width of the world
 * @param worldHeight height of the world
 * @param waterTop    highest y a fish or pellet may use
 * @param waterFloor  lowest y; below it is the sand
 * @param fishMargin  fish centers keep this distance from the left and right edge, and half of
 *                    it from {@code waterTop} and {@code waterFloor}
 */
public record WorldConfig(double worldWidth, double worldHeight, double waterTop, double waterFloor,
        double fishMargin) {

    /** The values from the specification. */
    public static WorldConfig defaults() {
        return new WorldConfig(1280, 720, 640, 80, 70);
    }

    /** Smallest x a fish center may have. */
    public double minFishX() {
        return fishMargin;
    }

    /** Largest x a fish center may have. */
    public double maxFishX() {
        return worldWidth - fishMargin;
    }

    /** Smallest y a fish center may have. */
    public double minFishY() {
        return waterFloor + fishMargin / 2;
    }

    /** Largest y a fish center may have. */
    public double maxFishY() {
        return waterTop - fishMargin / 2;
    }
}
