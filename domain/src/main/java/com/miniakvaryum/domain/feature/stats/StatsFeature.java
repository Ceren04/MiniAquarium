package com.miniakvaryum.domain.feature.stats;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.dirt.api.AquariumCleaned;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.stats.api.StatsService;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Stats feature: the numbers of "My stats" on the profile.
 *
 * <p>Provides: {@link StatsService}. Requires: {@link Wallet}, {@link TaskService},
 * {@link AquariumWorld}. Listens to: {@link AquariumCleaned}.
 */
public final class StatsFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "stats";

    private static final int SECTION_VERSION = 1;

    private final Gson gson = new Gson();
    private StatsService service;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        Section saved = ctx.savedSection()
                .map(s -> gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(s.version(), s.data()), Section.class))
                .orElse(new Section(0, 0, null));
        AquariumWorld world = ctx.require(AquariumWorld.class);
        service = new StatsService(ctx.require(TaskService.class), ctx.require(Wallet.class),
                () -> world.fish().stream().map(FishEntity::toData).toList(), saved.cleanings(), saved.daysPlayed(),
                saved.lastDayPlayed() == null ? null : LocalDate.parse(saved.lastDayPlayed()), ctx::requestSave);
        ctx.events().subscribe(AquariumCleaned.class, e -> service.countCleaning());
        ctx.provide(StatsService.class, service);
    }

    @Override
    public Optional<SaveSection> save() {
        LocalDate last = service.savedLastDayPlayed();
        Section section = new Section(service.savedCleanings(), service.savedDaysPlayed(),
                last == null ? null : last.toString());
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(section)));
    }

    /** Shape of the save section: {@code { "cleanings": 3, "daysPlayed": 5, "lastDayPlayed": "2026-11-24" }}. */
    private record Section(int cleanings, int daysPlayed, String lastDayPlayed) {
    }
}
