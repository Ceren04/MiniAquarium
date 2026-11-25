package com.miniakvaryum.domain.feature.trophies;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskApproved;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.feature.tasks.api.TaskStatus;
import com.miniakvaryum.domain.feature.trophies.api.TrophyService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Trophies feature: the trophy road ("Pokalstrasse").
 *
 * <p>Provides: {@link TrophyService}. Requires: {@link TaskService}, {@link Wallet}. Listens to:
 * {@link TaskApproved}.
 *
 * <p>When the save has no trophies section yet (first start with this feature), the tasks that
 * were approved before are counted: approved tasks cannot be deleted, so that list is complete.
 * Trophies reached this way are ready to be collected like any other.
 */
public final class TrophiesFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "trophies";

    private static final int SECTION_VERSION = 1;

    private final TrophyConfig config;
    private final Gson gson = new Gson();
    private TrophyService service;

    public TrophiesFeature(TrophyConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        TaskService tasks = ctx.require(TaskService.class);
        Section saved = ctx.savedSection()
                .map(s -> gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(s.version(), s.data()), Section.class))
                .orElseGet(() -> new Section(tasks.list(TaskStatus.APPROVED).stream().map(Task::id).toList(), List.of()));
        service = new TrophyService(config.milestones(), ctx.require(Wallet.class), ctx.time()::nowMillis,
                ctx.events(), orEmpty(saved.countedTaskIds()), orEmpty(saved.claimed()), ctx::requestSave);
        ctx.events().subscribe(TaskApproved.class, e -> service.countApproved(e.task().id()));
        ctx.provide(TrophyService.class, service);
    }

    @Override
    public Optional<SaveSection> save() {
        Section section = new Section(service.savedCountedTaskIds(), service.savedClaimed());
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(section)));
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    /** Shape of the save section: {@code { "countedTaskIds": ["t-1", "t-2"], "claimed": [1] }}. */
    private record Section(List<String> countedTaskIds, List<Integer> claimed) {
    }
}
