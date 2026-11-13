package com.miniakvaryum.domain.feature.tasks;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskApproved;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tasks feature: parents create tasks, the child reports them, parents approve, rewards are paid.
 *
 * <p>Provides: {@link TaskService}. Requires: {@link Wallet} (economy), {@link ParentSession}
 * (security). Publishes: {@link TaskApproved}.
 *
 * <p>The wiring in this class is complete. The rules live in {@link TaskService}, which is a
 * skeleton.
 */
public final class TasksFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "tasks";

    private static final int SECTION_VERSION = 1;

    private final TasksConfig config;
    private final Gson gson = new Gson();
    private TaskService service;

    public TasksFeature(TasksConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        List<Task> saved = ctx.savedSection().map(this::readSection).orElse(List.of());
        service = new TaskService(saved, ctx.require(Wallet.class), ctx.require(ParentSession.class),
                ctx.time(), ctx.ids(), config, ctx.events(), ctx::requestSave);
        ctx.provide(TaskService.class, service);
    }

    @Override
    public Optional<SaveSection> save() {
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(new Section(service.list()))));
    }

    private List<Task> readSection(SaveSection section) {
        Section parsed = gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(section.version(), section.data()),
                Section.class);
        if (parsed == null || parsed.tasks() == null) {
            throw new IllegalStateException("The '" + ID + "' section of the save file has no task list");
        }
        return parsed.tasks();
    }

    /** Shape of the save section: {@code { "tasks": [ ... ] }}. */
    private record Section(List<Task> tasks) {
    }
}
