package com.miniakvaryum.domain.feature.security;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.security.api.PinState;
import java.util.Objects;
import java.util.Optional;

/**
 * Security feature: the parent PIN and the "parent area is open" flag.
 *
 * <p>Provides: {@link PinService}, {@link ParentSession}. Requires: nothing. Publishes: nothing.
 *
 * <p>The wiring in this class is complete. The rules live in {@link PinService} and
 * {@link ParentSession}, which are skeletons.
 */
public final class SecurityFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "security";

    private static final int SECTION_VERSION = 1;

    private final SecurityConfig config;
    private final Gson gson = new Gson();
    private PinService pins;

    public SecurityFeature(SecurityConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        PinState saved = ctx.savedSection().map(this::readSection).orElse(null);
        pins = new PinService(saved, ctx.time(), config, ctx::requestSave);
        ctx.provide(PinService.class, pins);
        ctx.provide(ParentSession.class, new ParentSession(pins));
    }

    @Override
    public Optional<SaveSection> save() {
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(new Section(pins.state().orElse(null)))));
    }

    private PinState readSection(SaveSection section) {
        Section parsed = gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(section.version(), section.data()),
                Section.class);
        return parsed == null ? null : parsed.pin();
    }

    /** Shape of the save section: {@code { "pin": { ... } }}, or {@code {}} while no PIN is set. */
    private record Section(PinState pin) {
    }
}
