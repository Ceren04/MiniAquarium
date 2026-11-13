package com.miniakvaryum.game.core.ui;

/**
 * Reacts to a tap (or mouse click) into the aquarium on the main screen.
 */
public interface WorldTapHandler {

    /**
     * @param worldX x of the tap in world units
     * @param worldY y of the tap in world units
     * @return true if this handler used the tap; later handlers are then not asked
     */
    boolean onTap(float worldX, float worldY);
}
