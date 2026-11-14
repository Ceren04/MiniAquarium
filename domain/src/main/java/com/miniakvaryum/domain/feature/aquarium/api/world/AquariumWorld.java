package com.miniakvaryum.domain.feature.aquarium.api.world;

import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishBrain;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEvent;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The simulated aquarium: all fish and all pellets, advanced together once per frame.
 * The renderer reads {@link #fish()} and {@link #food()} every frame.
 *
 * <p>Tests: {@code AquariumWorldTest}.
 */
public final class AquariumWorld {

    /** Longest time step the simulation accepts; longer frames are cut down to this. */
    public static final double MAX_STEP_SECONDS = 1.0 / 15.0;

    private final AquariumConfig config;
    private final List<FishEntity> fish;
    private final FoodField food;
    private final FoodService foodService;
    private final FishBrain brain;

    /** Counts frames; only used to rotate which fish is visited first. */
    private long tickCounter;

    public AquariumWorld(AquariumConfig config, List<FishEntity> fish, FoodField food,
            FoodService foodService, FishBrain brain) {
        this.config = Objects.requireNonNull(config, "config");
        this.fish = List.copyOf(fish);
        this.food = Objects.requireNonNull(food, "food");
        this.foodService = Objects.requireNonNull(foodService, "foodService");
        this.brain = Objects.requireNonNull(brain, "brain");
    }

    /** Returns the configuration this world was built with (the renderer needs the world size). */
    public AquariumConfig config() {
        return config;
    }

    /** Returns all fish. The list cannot be modified. */
    public List<FishEntity> fish() {
        return fish;
    }

    /** Returns the pellets in the water. */
    public FoodField food() {
        return food;
    }

    /**
     * Advances the aquarium by one frame: pellets first, then every fish exactly once.
     *
     * <p>Frames longer than {@link #MAX_STEP_SECONDS} are cut down. The fish are visited
     * starting at a different index every frame, so that no fish always gets the first chance at
     * a new pellet.
     */
    public void update(double dtSeconds) {
        double dt = Math.min(dtSeconds, MAX_STEP_SECONDS);
        tickCounter++;
        food.update(dt);

        int count = fish.size();
        if (count == 0) {
            return;
        }
        int start = (int) (tickCounter % count);
        for (int i = 0; i < count; i++) {
            advance(fish.get((start + i) % count), dt);
        }
    }

    private void advance(FishEntity one, double dt) {
        String claimedPelletId = one.state().claimedPelletId();
        FishEvent event = brain.update(one, food, dt);
        if (event == FishEvent.FINISHED_EATING) {
            foodService.completeEating(one, claimedPelletId);
        }
        one.addAquariumMillis(Math.round(dt * 1000));
    }

    /**
     * The child drops a pellet at the given x.
     *
     * @return the id of the new pellet, or empty if no food is available
     */
    public Optional<String> dropPellet(double x) {
        return foodService.drop(x);
    }
}