package com.miniakvaryum.game.feature.bedtime;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeService;
import com.miniakvaryum.game.core.ui.UiSkin;

/**
 * A night-blue layer over the water and the sand during bedtime (not over the top bar). It fades in and out over two seconds,
 * so 21:00 and 07:00 look like dusk and dawn rather than a switch.
 */
final class NightOverlay extends Image {

    private static final Color NIGHT = Color.valueOf("0B2944");
    private static final float NIGHT_ALPHA = 0.45f;
    private static final float FADE_PER_SECOND = NIGHT_ALPHA / 2f;

    private final BedtimeService bedtime;

    NightOverlay(Skin skin, BedtimeService bedtime, float width, float height) {
        super(skin.newDrawable(UiSkin.WHITE, NIGHT));
        this.bedtime = bedtime;
        setBounds(0, 0, width, height);
        setTouchable(Touchable.disabled);
        // At start-up the right state is shown at once, without fading.
        getColor().a = bedtime.isBedtime() ? NIGHT_ALPHA : 0f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        float target = bedtime.isBedtime() ? NIGHT_ALPHA : 0f;
        float alpha = getColor().a;
        if (alpha < target) {
            alpha = Math.min(target, alpha + FADE_PER_SECOND * delta);
        } else if (alpha > target) {
            alpha = Math.max(target, alpha - FADE_PER_SECOND * delta);
        }
        getColor().a = alpha;
        setVisible(alpha > 0f);
    }
}
