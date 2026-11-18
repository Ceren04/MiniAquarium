package com.miniakvaryum.domain.feature.dirt.api;

/**
 * One dirt spot on the glass. Immutable: wiping produces a new spot with a lower strength.
 *
 * @param id          unique id
 * @param x           center x in world units
 * @param y           center y in world units
 * @param radius      size in world units
 * @param variant     which of the UI's spot shapes to draw (0 to {@code variants - 1})
 * @param rotationDeg rotation of the shape, so equal variants look different
 * @param strength    how dirty: 1 is fresh, 0 is gone; the UI uses it as opacity
 */
public record DirtSpot(String id, double x, double y, double radius, int variant, double rotationDeg,
        double strength) {

    /** Returns a copy of this spot with another strength. */
    public DirtSpot withStrength(double newStrength) {
        return new DirtSpot(id, x, y, radius, variant, rotationDeg, newStrength);
    }
}
