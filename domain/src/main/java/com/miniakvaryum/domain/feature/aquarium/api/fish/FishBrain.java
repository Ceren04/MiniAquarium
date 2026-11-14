package com.miniakvaryum.domain.feature.aquarium.api.fish;

import com.miniakvaryum.domain.core.util.RandomSource;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.ActionWeights;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.FishConfig;
import com.miniakvaryum.domain.feature.aquarium.WorldConfig;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import com.miniakvaryum.domain.feature.aquarium.api.food.Pellet;
import java.util.Objects;
import java.util.Optional;

/**
 * Decides what a fish does: advances its current action and chooses the next one by a weighted
 * random draw. One instance serves all fish; all per-fish data lives in {@link FishState}.
 *
 * <p>This class is in the same package as {@link FishState} and may call its setters.
 *
 * <p>Random numbers are drawn in a fixed order (see the method comments), because tests script
 * them. Do not reorder the draws.
 */
public final class FishBrain {

    /** Chance that an idle fish turns around. Fixed by the specification, not yet in FishConfig. */
    private static final double LOOK_AROUND_CHANCE = 0.3;

    /** Horizontal direction components smaller than this do not change the facing. */
    private static final double MIN_TURN_DX = 0.001;

    private final AquariumConfig config;
    private final FishConfig fishConfig;
    private final RandomSource random;

    public FishBrain(AquariumConfig config, RandomSource random) {
        this.config = Objects.requireNonNull(config, "config");
        this.fishConfig = config.fish();
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * Advances one fish by one frame.
     *
     * <p>A fish with a running action keeps executing it. A fish without one chooses and starts
     * a new action; that frame returns {@link FishEvent#NONE} and does not move the fish.
     *
     * @param fish      the fish to advance
     * @param food      pellets in the water
     * @param dtSeconds frame time in seconds
     * @return {@link FishEvent#FINISHED_EATING} in the frame in which an EAT action ends
     */
    public FishEvent update(FishEntity fish, FoodField food, double dtSeconds) {
        FishState state = fish.state();
        if (state.remainingS() > 0) {
            return advanceRunningAction(state, dtSeconds);
        }
        startNewAction(fish, food);
        return FishEvent.NONE;
    }

    // ---------------------------------------------------------------- running an action

    private FishEvent advanceRunningAction(FishState state, double dtSeconds) {
        state.setPosition(clampToSwimArea(state.position().plus(state.velocity().scale(dtSeconds))));
        state.setRemainingS(state.remainingS() - dtSeconds);

        boolean ended = state.remainingS() <= 0;
        if (ended && state.action() == ActionType.EAT) {
            return FishEvent.FINISHED_EATING;
        }
        return FishEvent.NONE;
    }

    private Vec2 clampToSwimArea(Vec2 p) {
        WorldConfig world = config.world();
        return new Vec2(
                Math.max(world.minFishX(), Math.min(world.maxFishX(), p.x())),
                Math.max(world.minFishY(), Math.min(world.maxFishY(), p.y())));
    }

    // ---------------------------------------------------------------- choosing an action

    private void startNewAction(FishEntity fish, FoodField food) {
        FishState state = fish.state();
        Optional<Pellet> target = food.nearestFree(state.position());
        FishContext context = contextOf(state.position(), target);
        ActionType action = drawAction(context);

        switch (action) {
            case SLOW_MOVE -> startMove(state, action, fishConfig.slowSpeed(), fishConfig.slowDurationS(),
                    context, target);
            case DASH -> startMove(state, action, fishConfig.dashSpeed(), fishConfig.dashDurationS(),
                    context, target);
            case IDLE -> startIdle(state);
            case EAT -> startEat(fish, food, target);
        }
    }

    private FishContext contextOf(Vec2 position, Optional<Pellet> target) {
        if (target.isEmpty()) {
            return FishContext.NORMAL;
        }
        double distance = target.get().position().distanceTo(position);
        if (distance <= fishConfig.eatRange()) {
            return FishContext.FOOD_IN_EATING_RANGE;
        }
        if (distance <= fishConfig.detectRange()) {
            return FishContext.FOOD_DETECTED;
        }
        return FishContext.NORMAL;
    }

    /** One random draw (nextInt); walks the actions in declaration order, subtracting weights. */
    private ActionType drawAction(FishContext context) {
        ActionWeights weights = fishConfig.weights();
        int remaining = random.nextInt(weights.total(context));
        for (ActionType action : ActionType.values()) {
            int weight = weights.weight(context, action);
            if (remaining < weight) {
                return action;
            }
            remaining -= weight;
        }
        throw new IllegalStateException("weights do not add up for context " + context);
    }

    // ---------------------------------------------------------------- starting an action

    private void startMove(FishState state, ActionType action, double speed, double durationS,
                           FishContext context, Optional<Pellet> target) {
        Vec2 direction = (context == FishContext.NORMAL || target.isEmpty())
                ? randomDirection()
                : directionToward(state.position(), target.get());
        direction = mirrorIfLeavingArea(state.position(), direction, speed * durationS);

        state.setAction(action);
        state.setVelocity(direction.scale(speed));
        state.setRemainingS(durationS);
        faceDirectionOf(state, direction.x());
    }

    /** One random draw (nextDouble): a random angle. */
    private Vec2 randomDirection() {
        double angle = random.nextDouble() * 2 * Math.PI;
        return new Vec2(Math.cos(angle), Math.sin(angle));
    }

    private static Vec2 directionToward(Vec2 from, Pellet pellet) {
        return pellet.position().minus(from).normalized();
    }

    /** If the end point would be outside the swim area, mirror the offending component. */
    private Vec2 mirrorIfLeavingArea(Vec2 position, Vec2 direction, double distance) {
        WorldConfig world = config.world();
        Vec2 end = position.plus(direction.scale(distance));
        double dx = (end.x() < world.minFishX() || end.x() > world.maxFishX()) ? -direction.x() : direction.x();
        double dy = (end.y() < world.minFishY() || end.y() > world.maxFishY()) ? -direction.y() : direction.y();
        return new Vec2(dx, dy);
    }

    /** Two random draws, in this order: duration (nextDouble), then look-around chance (nextDouble). */
    private void startIdle(FishState state) {
        double duration = fishConfig.idleMinS()
                + random.nextDouble() * (fishConfig.idleMaxS() - fishConfig.idleMinS());
        state.setAction(ActionType.IDLE);
        state.setVelocity(Vec2.ZERO);
        state.setRemainingS(duration);
        if (random.nextDouble() < LOOK_AROUND_CHANCE) {
            state.setFacing(-state.facing());
        }
    }

    /** No random draw. If another fish was faster, the fish idles instead. */
    private void startEat(FishEntity fish, FoodField food, Optional<Pellet> target) {
        FishState state = fish.state();
        boolean claimed = target.isPresent() && food.tryClaim(target.get().id(), fish.id());
        if (!claimed) {
            startIdle(state);
            return;
        }
        Pellet pellet = target.get();
        state.setClaimedPelletId(pellet.id());
        state.setAction(ActionType.EAT);
        state.setVelocity(Vec2.ZERO);
        state.setRemainingS(fishConfig.eatDurationS());
        faceDirectionOf(state, pellet.position().x() - state.position().x());
    }

    private static void faceDirectionOf(FishState state, double dx) {
        if (Math.abs(dx) > MIN_TURN_DX) {
            state.setFacing(dx > 0 ? 1 : -1);
        }
    }
}