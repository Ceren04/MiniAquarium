package com.miniakvaryum.domain.feature.dirt;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.dirt.api.AquariumCleaned;
import com.miniakvaryum.domain.feature.dirt.api.DirtService;
import com.miniakvaryum.domain.feature.dirt.api.DirtSpot;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dirt feature: spots appear on the aquarium glass over time, also while the app is closed,
 * and the child wipes them away.
 *
 * <p>Provides: {@link DirtService}. Requires: nothing. Publishes: {@link AquariumCleaned}.
 *
 * <p>Offline dirt comes from the credited gap, so it is limited to 24 hours like every other
 * offline progress, and changing the device date cannot make the aquarium dirtier.
 */
public final class DirtFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "dirt";

    private static final int SECTION_VERSION = 1;
    private static final long MILLIS_PER_SECOND = 1000L;

    private final DirtConfig config;
    private final Gson gson = new Gson();
    private DirtService service;
    private double runningSeconds;
    private long passedMillis;

    public DirtFeature(DirtConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        Section saved = ctx.savedSection().map(this::readSection).orElse(new Section(List.of(), 0));
        service = new DirtService(saved.spots(), saved.progressMillis(), config, ctx.random(), ctx.ids(),
                ctx.events(), ctx::requestSave);
        // The aquarium got dirty while the app was closed.
        service.advance(ctx.creditedGap().millis());
        ctx.provide(DirtService.class, service);
    }

    @Override
    public void tick(double dtSeconds) {
        // Frames last a fraction of a millisecond too much or too little; counting the total
        // running time and passing on only the new whole milliseconds loses nothing over hours.
        runningSeconds += dtSeconds;
        long total = (long) (runningSeconds * MILLIS_PER_SECOND);
        if (total > passedMillis) {
            service.advance(total - passedMillis);
            passedMillis = total;
        }
    }

    @Override
    public Optional<SaveSection> save() {
        Section section = new Section(service.spots(), service.progressMillis());
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(section)));
    }

    private Section readSection(SaveSection section) {
        Section parsed = gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(section.version(), section.data()),
                Section.class);
        if (parsed == null || parsed.spots() == null) {
            throw new IllegalStateException("The '" + ID + "' section of the save file has no spot list");
        }
        return parsed;
    }

    /** Shape of the save section: {@code { "spots": [ ... ], "progressMillis": 0 }}. */
    private record Section(List<DirtSpot> spots, long progressMillis) {
    }
}
