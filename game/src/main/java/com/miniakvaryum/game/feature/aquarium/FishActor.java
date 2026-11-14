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
 * <p>The fish has four parts, added back to front: tail, body, fin, head. Tail and fin swing
 * around a pivot; the swing speed depends on the fish's current action.
 */
final class FishActor extends Group {

    // Offset of each part's center from the body center (fish facing right), in world units.
    private static final float TAIL_OFFSET_X = -62f;
    private static final float TAIL_OFFSET_Y = 0f;
    private static final float FIN_OFFSET_X = -6f;
    private static final float FIN_OFFSET_Y = 34f;
    private static final float HEAD_OFFSET_X = 52f;
    private static final float HEAD_OFFSET_Y = 0f;

    // Pivot (origin of rotation) inside the part, measured from the part's lower-left corner.
    private static final float TAIL_PIVOT_X = 48f; // front edge, where the tail joins the body
    private static final float TAIL_PIVOT_Y = 28f;
    private static final float FIN_PIVOT_X = 18f; // base of the fin
    private static final float FIN_PIVOT_Y = 0f;

    private final FishEntity fish;
    private final Image tail;
    private final Image fin;

    /** Running time of this actor's animation in seconds. */
    private float animationTime;

    FishActor(FishEntity fish, AquariumTextures textures) {
        this.fish = fish;

        tail = new Image(textures.tail);
        Image body = new Image(textures.body);
        fin = new Image(textures.fin);
        Image head = new Image(textures.head);

        // Order of addActor is the drawing order, back to front.
        addCentered(tail, TAIL_OFFSET_X, TAIL_OFFSET_Y);
        addCentered(body, 0, 0);
        addCentered(fin, FIN_OFFSET_X, FIN_OFFSET_Y);
        addCentered(head, HEAD_OFFSET_X, HEAD_OFFSET_Y);

        tail.setOrigin(TAIL_PIVOT_X, TAIL_PIVOT_Y);
        fin.setOrigin(FIN_PIVOT_X, FIN_PIVOT_Y);

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
        animate(delta);
    }

    private void syncWithDomain() {
        Vec2 position = fish.state().position();
        setPosition((float) position.x(), (float) position.y());
        // +1 or -1: mirrors the group around its origin (the body center).
        setScaleX(fish.state().facing());
    }

    private void animate(float delta) {
        animationTime += delta;
        float frequencyHz = FishAnimationMath.swimFrequencyHz(fish.state().action());
        tail.setRotation(FishAnimationMath.tailAngleDeg(frequencyHz, animationTime));
        fin.setRotation(FishAnimationMath.finAngleDeg(frequencyHz, animationTime));
    }
}