package com.miniakvaryum.game.core.ui;

/**
 * Reacts to a finger (or the mouse with the button held) moving over the aquarium on the
 * main screen, for example to wipe the glass.
 */
public interface WorldDragHandler {

    /**
     * Called for every movement while the finger is down.
     *
     * @param fromX previous x of the finger in world units
     * @param fromY previous y of the finger in world units
     * @param toX   current x of the finger in world units
     * @param toY   current y of the finger in world units
     * @return true if this handler used the movement; later handlers are then not asked
     */
    boolean onDrag(float fromX, float fromY, float toX, float toY);
}
