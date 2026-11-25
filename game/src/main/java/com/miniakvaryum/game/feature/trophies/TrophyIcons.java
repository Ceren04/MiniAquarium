package com.miniakvaryum.game.feature.trophies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.trophies.api.CurrencyReward;
import com.miniakvaryum.domain.feature.trophies.api.ItemReward;
import com.miniakvaryum.domain.feature.trophies.api.Reward;
import com.miniakvaryum.domain.feature.trophies.api.TrophyState;
import com.miniakvaryum.game.core.ui.MovingPicture;
import com.miniakvaryum.game.core.ui.MovingPicture.Motion;
import com.miniakvaryum.game.core.ui.ShapeTextures;
import java.util.HashMap;
import java.util.Map;

/**
 * The pictures of the road in {@code assets/trophies/}: gold trophy, gray trophy, the present
 * (open box and lid), the three tile frames, pearl and bubble of the road, food, coins, and one
 * picture per item in {@code items/<item id>.png}; light rays and confetti are drawn in code. Every reward picture moves a
 * little ({@link MovingPicture}): plants bend, things float, accessories tilt, fish swim, the
 * chest opens and closes, bubbles rise from the castle. A missing file becomes a colored circle.
 */
final class TrophyIcons {

    private static final int PLACEHOLDER = 128;
    private static final int BUBBLE = 32;
    private static final int RAYS_SIZE = 512;
    private static final int RAY_COUNT = 16;
    private static final Color RAYS_COLOR = new Color(1f, 0.9f, 0.5f, 0.9f);
    private static final int CONFETTI_W = 12;
    private static final int CONFETTI_H = 7;
    private static final Color BUBBLE_COLOR = new Color(0.85f, 0.95f, 1f, 1f);
    /** Where the castle's window with the bubbles is, as a part of the picture. */
    private static final float CASTLE_WINDOW_X = 0.79f;
    private static final float CASTLE_WINDOW_Y = 0.5f;

    private final ShapeTextures shapes;
    private final Texture trophy;
    private final Texture locked;
    private final Texture lid;
    private final Texture openBox;
    private final Texture frameLocked;
    private final Texture frameReady;
    private final Texture frameClaimed;
    private final Texture pearl;
    private final Texture roadBubble;
    private final Texture rays;
    private final Texture confetti;
    private final Texture food;
    private final Texture coins;
    private final Texture bubble;
    private final Map<String, Texture> items = new HashMap<>();

    TrophyIcons(ShapeTextures shapes) {
        this.shapes = shapes;
        this.trophy = load("trophies/trophy.png", Color.GOLD);
        this.locked = load("trophies/trophy-locked.png", Color.GRAY);
        this.lid = load("trophies/gift-lid.png", Color.PINK);
        this.openBox = load("trophies/gift-box-open.png", Color.PINK);
        this.frameLocked = load("trophies/frame-locked.png", Color.SLATE);
        this.frameReady = load("trophies/frame-ready.png", Color.ORANGE);
        this.frameClaimed = load("trophies/frame-claimed.png", Color.PINK);
        this.pearl = load("trophies/pearl.png", Color.WHITE);
        this.roadBubble = load("trophies/bubble.png", Color.SKY);
        this.rays = shapes.sunburst(RAYS_SIZE, RAYS_COLOR, RAY_COUNT);
        this.confetti = shapes.rect(CONFETTI_W, CONFETTI_H, Color.WHITE);
        this.food = load("trophies/food.png", Color.ORANGE);
        this.coins = load("trophies/coins.png", Color.GOLD);
        this.bubble = shapes.circle(BUBBLE, BUBBLE_COLOR);
    }

    Texture trophy() {
        return trophy;
    }

    Texture locked() {
        return locked;
    }

    /** A new closed present made of its open box and its lid; the reveal lets the lid fly off. */
    GiftPicture gift() {
        return new GiftPicture(openBox, lid);
    }

    /** The frame of a tile: stone when locked, golden coral when ready, shells when collected. */
    Texture frame(TrophyState state) {
        return switch (state) {
            case LOCKED -> frameLocked;
            case READY -> frameReady;
            case CLAIMED -> frameClaimed;
        };
    }

    /** A pearl of the road (the part the child has done). */
    Texture pearl() {
        return pearl;
    }

    /** A bubble of the road (the part still to do). */
    Texture roadBubble() {
        return roadBubble;
    }

    /** Light rays behind an opening present. */
    Texture rays() {
        return rays;
    }

    /** A small white piece of confetti, colored when drawn. */
    Texture confetti() {
        return confetti;
    }

    /** A new moving picture of the reward (each actor can only stand in one place). */
    MovingPicture picture(Reward reward) {
        if (reward instanceof CurrencyReward money) {
            return new MovingPicture(money.currency() == Currency.FOOD ? food : coins, Motion.BOB);
        }
        String id = ((ItemReward) reward).itemId();
        MovingPicture picture = new MovingPicture(item(id), motionOf(id));
        if (id.equals("deco.chest")) {
            picture.alternateWith(item("deco.chest-closed"));
        } else if (id.equals("deco.castle")) {
            picture.bubbles(bubble, CASTLE_WINDOW_X, CASTLE_WINDOW_Y);
        }
        return picture;
    }

    /** Plants bend, accessories tilt, fish swim, everything else floats. */
    static Motion motionOf(String itemId) {
        if (itemId.equals("deco.plant") || itemId.equals("deco.coral")) {
            return Motion.BEND;
        }
        if (itemId.startsWith("acc.")) {
            return Motion.TILT;
        }
        if (itemId.startsWith("fish.")) {
            return Motion.SWIM;
        }
        return Motion.BOB;
    }

    private Texture item(String id) {
        return items.computeIfAbsent(id, key -> load("trophies/items/" + key + ".png", Color.LIGHT_GRAY));
    }

    private Texture load(String path, Color placeholder) {
        return shapes.smoothFileOr(path, () -> shapes.circle(PLACEHOLDER, placeholder));
    }
}
