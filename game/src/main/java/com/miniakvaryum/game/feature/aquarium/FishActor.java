package com.miniakvaryum.game.feature.aquarium;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;

/**
 * Draws one fish. The actor never decides anything: every frame it copies position and facing
 * from the domain object and draws.
 *
 * <p>The actor is a group whose own position is the <b>center of the body</b>. All parts are
 * placed relative to that center for a fish facing right. Turning the fish is then one line:
 * {@code setScaleX(facing)} mirrors the whole group around its origin, and no offset has to be
 * recalculated.
 *
 * <p><b>SKELETON:</b> body and head are drawn and follow the fish. To implement
 * (specification section 8.4):
 * <ul>
 *   <li>Add the missing parts. Order of {@code addActor} calls is the drawing order, back to
 *       front: tail, body, fin, head.</li>
 *   <li>Offsets of the part centers from the body center, and pivots inside the part:
 *       <pre>
 *   part   size       offset      pivot (origin of rotation)
 *   tail   48 x 56    (-62, 0)    front edge center (48, 28)
 *   body   120 x 64   (0, 0)      center
 *   fin    36 x 24    (-6, 34)    base center (18, 0)
 *   head   44 x 44    (52, 0)     center
 *       </pre>
 *       Set the pivot with {@code image.setOrigin(x, y)} and rotate with
 *       {@code image.setRotation(degrees)}.</li>
 *   <li>Animate in {@link #act}: keep a running time {@code t += delta} per actor and use
 *       {@link FishAnimationMath} for the tail and fin angles, with the frequency of the
 *       fish's current action.</li>
 * </ul>
 */
final class FishActor extends Group {

    private final FishEntity fish;

    FishActor(FishEntity fish, AquariumTextures textures) {
        this.fish = fish;

        addCentered(new Image(textures.body), 0, 0);
        addCentered(new Image(textures.head), 52, 0);

        syncWithDomain();
    }

    /** Adds a part so that its center sits at the given offset from the body center. */
    private void addCentered(Image part, float offsetX, float offsetY) {
        part.setPosition(offsetX - part.getWidth() / 2, offsetY - part.getHeight() / 2);
        addActor(part);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        syncWithDomain();
    }

    private void syncWithDomain() {
        Vec2 position = fish.state().position();
        setPosition((float) position.x(), (float) position.y());
        // +1 or -1: mirrors the group around its origin (the body center).
        setScaleX(fish.state().facing());
    }
}
