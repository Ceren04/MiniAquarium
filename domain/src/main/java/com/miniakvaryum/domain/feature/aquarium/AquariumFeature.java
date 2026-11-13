package com.miniakvaryum.domain.feature.aquarium;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.util.RandomSource;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishBrain;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.food.FishFed;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodService;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aquarium feature: the fish, the pellets and the simulation that moves them.
 *
 * <p>Provides: {@link AquariumWorld}, {@link FoodService}. Requires: {@link Wallet} (economy).
 * Publishes: {@link FishFed}.
 *
 * <p>The wiring in this class is complete: it creates or loads the fish, credits offline time
 * to their age and saves them. The rules live in the classes under {@code api}, which are
 * skeletons.
 */
public final class AquariumFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "aquarium";

    private static final int SECTION_VERSION = 1;

    private final AquariumConfig config;
    private final Gson gson = new Gson();
    private AquariumWorld world;

    public AquariumFeature(AquariumConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        List<FishData> data = ctx.savedSection().map(this::readSection).orElseGet(() -> newGameFish(ctx));

        // Positions are never saved: every fish gets a fresh random position at start-up.
        List<FishEntity> fish = new ArrayList<>();
        for (FishData one : data) {
            FishEntity entity = new FishEntity(one, randomPosition(ctx.random()));
            // Time that passed while the app was closed counts as time in the aquarium.
            entity.addAquariumMillis(ctx.creditedGap().millis());
            fish.add(entity);
        }

        FoodField field = new FoodField(config);
        FoodService foodService = new FoodService(field, ctx.require(Wallet.class), ctx.time(), ctx.ids(),
                config, ctx.events());
        world = new AquariumWorld(config, fish, field, foodService, new FishBrain(config, ctx.random()));

        ctx.provide(AquariumWorld.class, world);
        ctx.provide(FoodService.class, foodService);
    }

    @Override
    public void tick(double dtSeconds) {
        world.update(dtSeconds);
    }

    @Override
    public Optional<SaveSection> save() {
        List<FishData> data = world.fish().stream().map(FishEntity::toData).toList();
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(new Section(data))));
    }

    private List<FishData> newGameFish(FeatureContext ctx) {
        List<FishData> fish = new ArrayList<>();
        for (int i = 0; i < config.fish().initialFishCount(); i++) {
            fish.add(new FishData(ctx.ids().next(), null, 0, 0));
        }
        return fish;
    }

    /** A random point inside the area fish may use. Draws x first, then y. */
    private Vec2 randomPosition(RandomSource random) {
        WorldConfig w = config.world();
        double x = w.minFishX() + random.nextDouble() * (w.maxFishX() - w.minFishX());
        double y = w.minFishY() + random.nextDouble() * (w.maxFishY() - w.minFishY());
        return new Vec2(x, y);
    }

    private List<FishData> readSection(SaveSection section) {
        Section parsed = gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(section.version(), section.data()),
                Section.class);
        if (parsed == null || parsed.fish() == null) {
            throw new IllegalStateException("The '" + ID + "' section of the save file has no fish list");
        }
        return parsed.fish();
    }

    /** Shape of the save section: {@code { "fish": [ ... ] }}. */
    private record Section(List<FishData> fish) {
    }
}
