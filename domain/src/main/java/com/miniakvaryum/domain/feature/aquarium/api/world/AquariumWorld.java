package com.miniakvaryum.domain.feature.aquarium.api.world;

import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishBrain;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The simulated aquarium: all fish and all pellets, advanced together once per frame.
 * The renderer reads {@link #fish()} and {@link #food()} every frame.
 *
 * <p><b>SKELETON:</b> the getters work; {@link #update} and {@link #dropPellet} must be
 * implemented (specification section 7.9). Tests: {@code AquariumWorldTest}.
 */
public final class AquariumWorld {

    /** Longest time step the simulation accepts; longer frames are cut down to this. */
    public static final double MAX_STEP_SECONDS = 1.0 / 15.0;

    private final AquariumConfig config;
    private final List<FishEntity> fish;
    private final FoodField food;
    private final FoodService foodService;
    private final FishBrain brain;

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
     * Advances the aquarium by one frame.
     *
     * <p>TODO:
     * <ol>
     *   <li>{@code dt = min(dtSeconds, MAX_STEP_SECONDS)}.</li>
     *   <li>Increment a tick counter (a new field).</li>
     *   <li>{@code food.update(dt)}.</li>
     *   <li>Visit every fish once, starting at index {@code tickCounter % fish.size()} and
     *       wrapping around. The rotating start makes it fair: no fish always gets the first
     *       chance at a pellet.</li>
     *   <li>For each fish: remember its claimed pellet id, call {@code brain.update(fish, food, dt)};
     *       on {@code FINISHED_EATING} call {@code foodService.completeEating(fish, pelletId)};
     *       then {@code fish.addAquariumMillis(round(dt * 1000))}.</li>
     * </ol>
     *
     * <p>Until it is implemented this method does nothing, so that the game can run with fish
     * that stand still. It is called every frame and therefore must not throw.
     */
    public void update(double dtSeconds) {
        // TODO: implement the steps described above.
    }

    /**
     * The child drops a pellet at the given x.
     *
     * <p>TODO: delegate to {@code foodService.drop(x)}.
     *
     * @return the id of the new pellet, or empty if no food is available
     */
    public Optional<String> dropPellet(double x) {
        throw new UnsupportedOperationException("TODO: AquariumWorld.dropPellet is not implemented yet");
    }
}
