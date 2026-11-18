package com.miniakvaryum.domain.feature.dirt;

/**
 * Tunable numbers of the dirt feature.
 *
 * @param maxSpots          most dirt spots on the glass at the same time; this is 100 % dirt
 * @param spotIntervalMillis game time between two new spots, also while the app is closed
 * @param minX              smallest x of a spot center (world units)
 * @param maxX              largest x of a spot center
 * @param minY              smallest y of a spot center
 * @param maxY              largest y of a spot center
 * @param minRadius         smallest spot radius
 * @param maxRadius         largest spot radius
 * @param variants          number of different spot shapes the UI can draw
 * @param fingerRadius      a wipe reaches every spot whose edge is within this distance of the finger
 * @param rubPerSpot        rubbing distance (world units) that removes one spot completely
 * @param tapRub            rubbing distance that a single tap counts as
 */
public record DirtConfig(int maxSpots, long spotIntervalMillis,
        double minX, double maxX, double minY, double maxY,
        double minRadius, double maxRadius, int variants,
        double fingerRadius, double rubPerSpot, double tapRub) {

    public DirtConfig {
        if (maxSpots < 1 || spotIntervalMillis < 1 || variants < 1) {
            throw new IllegalArgumentException("maxSpots, spotIntervalMillis and variants must be >= 1");
        }
        if (minX > maxX || minY > maxY || minRadius <= 0 || minRadius > maxRadius) {
            throw new IllegalArgumentException("invalid spot area or radius range");
        }
        if (fingerRadius < 0 || rubPerSpot <= 0 || tapRub < 0) {
            throw new IllegalArgumentException("invalid wiping values");
        }
    }

    /**
     * Defaults: a new spot every 2 hours and at most 12 spots, so an aquarium left alone for
     * 24 hours (the longest credited offline time) is completely dirty. Spots stay inside the
     * water area of the 1280 x 720 world. Five taps or about 300 units of rubbing clean a spot.
     */
    public static DirtConfig defaults() {
        return new DirtConfig(12, 2 * 3_600_000L, 90, 1190, 140, 580, 28, 56, 3, 24, 300, 60);
    }
}
