package com.miniakvaryum.domain.feature.aquarium.api.fish;

import com.miniakvaryum.domain.core.util.RandomSource;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import java.util.Objects;

/**
 * Decides what a fish does: advances its current action and chooses the next one by a weighted
 * random draw. One instance serves all fish; all per-fish data lives in {@link FishState}.
 *
 * <p><b>SKELETON</b> (specification section 7.7). Tests: {@code FishBrainTest}. This class is in
 * the same package as {@link FishState} and may call its setters.
 */
public final class FishBrain {

    private final AquariumConfig config;
    private final RandomSource random;

    public FishBrain(AquariumConfig config, RandomSource random) {
        this.config = Objects.requireNonNull(config, "config");
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Advances one fish by one frame.
     *
     * <p>TODO:
     * <ol>
     *   <li><b>Running action</b> ({@code remainingS > 0}): {@code position += velocity * dt},
     *       {@code remainingS -= dt}, clamp the position into
     *       {@code [minFishX, maxFishX] x [minFishY, maxFishY]} (see {@code WorldConfig}).
     *       If the action just ended and it was EAT, return {@link FishEvent#FINISHED_EATING};
     *       otherwise {@link FishEvent#NONE}.</li>
     *   <li><b>No running action</b>: choose and start a new one, return NONE.</li>
     * </ol>
     *
     * <p><b>Choosing.</b> Find the nearest FREE pellet with {@code food.nearestFree(position)}
     * (claimed pellets are never considered). Context: FOOD_IN_EATING_RANGE if its distance is
     * {@code <= eatRange}, else FOOD_DETECTED if {@code <= detectRange}, else NORMAL. Draw
     * {@code r = random.nextInt(weights.total(context))} and walk {@code ActionType.values()} in
     * order, subtracting each weight, until r falls into an action.
     *
     * <p><b>Starting.</b>
     * <ul>
     *   <li>SLOW_MOVE / DASH: speed and duration from the config. Direction: in NORMAL a random
     *       angle {@code random.nextDouble() * 2 * PI}; in FOOD_DETECTED toward the pellet. If
     *       the end point {@code position + dir * speed * duration} would leave the allowed
     *       area, mirror the offending component of the direction. Set
     *       {@code facing = sign(dir.x)} if {@code abs(dir.x) > 0.001}, otherwise keep it.</li>
     *   <li>IDLE: velocity 0, duration
     *       {@code idleMinS + random.nextDouble() * (idleMaxS - idleMinS)}. Then, if
     *       {@code random.nextDouble() < 0.3}, flip facing (looking around).</li>
     *   <li>EAT: {@code food.tryClaim(pelletId, fish.id())}. If it fails (another fish was
     *       faster), start IDLE instead. On success store the claimed pellet id, velocity 0,
     *       duration {@code eatDurationS}, face the pellet.</li>
     * </ul>
     * Facing changes only when an action starts. Draw random numbers exactly in the order
     * written above: tests script them.
     *
     * @param fish      the fish to advance
     * @param food      pellets in the water
     * @param dtSeconds frame time in seconds
     */
    public FishEvent update(FishEntity fish, FoodField food, double dtSeconds) {
        throw new UnsupportedOperationException("TODO: FishBrain.update is not implemented yet");
    }
}
