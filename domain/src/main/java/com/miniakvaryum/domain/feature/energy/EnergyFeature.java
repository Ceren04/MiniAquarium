package com.miniakvaryum.domain.feature.energy;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.energy.api.EnergyService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Energy feature: energy 0 to 100 that fills with time and is spent on minigames.
 *
 * <p>Provides: {@link EnergyService}. Requires: {@link AquariumWorld} (install after the
 * aquarium feature).
 *
 * <p>Offline energy comes from the credited gap, so it is limited to 24 hours, and changing the
 * device date gives nothing. For the offline time the fish in the aquarium when the game was
 * closed count: when this feature is installed, the aquarium has just loaded exactly those fish.
 */
public final class EnergyFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "energy";

    private static final int SECTION_VERSION = 1;

    private final EnergyConfig config;
    private final FishLevels levels;
    private final Gson gson = new Gson();
    private EnergyService service;

    /**
     * @param config rules
     * @param levels level of a fish; {@link FishLevels#ALL_LEVEL_ONE} until fish levels exist
     */
    public EnergyFeature(EnergyConfig config, FishLevels levels) {
        this.config = Objects.requireNonNull(config, "config");
        this.levels = Objects.requireNonNull(levels, "levels");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        Section saved = ctx.savedSection().map(this::readSection)
                .orElse(new Section(config.startValue(), List.of()));
        AquariumWorld world = ctx.require(AquariumWorld.class);
        service = new EnergyService(saved.value(), saved.spent(), config,
                () -> world.fish().stream().map(FishEntity::toData).toList(), levels, ctx::requestSave);
        service.advance(ctx.creditedGap().millis());
        ctx.provide(EnergyService.class, service);
    }

    @Override
    public void tick(double dtSeconds) {
        service.advanceSeconds(dtSeconds);
    }

    @Override
    public Optional<SaveSection> save() {
        Section section = new Section(service.exactValue(), service.spentRefIds());
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(section)));
    }

    private Section readSection(SaveSection section) {
        Section parsed = gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(section.version(), section.data()),
                Section.class);
        if (parsed == null) {
            throw new IllegalStateException("The '" + ID + "' section of the save file is empty");
        }
        return new Section(parsed.value(), parsed.spent() == null ? List.of() : parsed.spent());
    }

    /** Shape of the save section: {@code { "value": 63.25, "spent": ["memory:id-4", ...] }}. */
    private record Section(double value, List<String> spent) {
    }
}
