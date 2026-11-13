package com.miniakvaryum.domain.core.util;

/**
 * An immutable 2D point or direction in world units (origin bottom-left, y points up).
 *
 * <p>Immutable means: no method changes this object, every operation returns a new one.
 * That makes a Vec2 safe to hand to other code (for example the renderer) without copying.
 *
 * @param x horizontal component
 * @param y vertical component
 */
public record Vec2(double x, double y) {

    /** The vector (0, 0). */
    public static final Vec2 ZERO = new Vec2(0, 0);

    /** Returns this + o. */
    public Vec2 plus(Vec2 o) {
        return new Vec2(x + o.x, y + o.y);
    }

    /** Returns this - o, the vector that points from {@code o} to this. */
    public Vec2 minus(Vec2 o) {
        return new Vec2(x - o.x, y - o.y);
    }

    /** Returns this vector with both components multiplied by {@code k}. */
    public Vec2 scale(double k) {
        return new Vec2(x * k, y * k);
    }

    /** Returns the length of this vector. */
    public double length() {
        return Math.hypot(x, y);
    }

    /**
     * Returns a vector with the same direction and length 1.
     * The zero vector has no direction, so it is returned unchanged.
     */
    public Vec2 normalized() {
        double len = length();
        return len == 0 ? ZERO : new Vec2(x / len, y / len);
    }

    /** Returns the distance between this point and {@code o}. */
    public double distanceTo(Vec2 o) {
        return minus(o).length();
    }
}
