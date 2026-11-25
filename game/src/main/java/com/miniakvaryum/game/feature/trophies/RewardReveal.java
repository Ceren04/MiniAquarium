package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

/**
 * Shows what a collected trophy gave. Only a picture: the reward is already written when this is
 * shown, so closing the app during the animation loses nothing.
 *
 * <p>{@link AnimatedReveal} plays the opening of the plan (shaking present, lid, light rays,
 * confetti, reward rising). Another version can replace it without touching the trophy logic.
 */
interface RewardReveal {

    /** What the card shows: the reward's (moving) picture and texts; name may be null. */
    record Content(Actor picture, String title, String name, String detail) {
    }

    /**
     * Shows the card on top of the stage.
     *
     * @param onClose called once when the child taps "Super!"
     */
    void show(Stage stage, Content content, Runnable onClose);
}
