package com.miniakvaryum.domain.feature.bedtime;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.time.LocalClock;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeService;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Bedtime feature: 21:00 to 07:00 is bedtime.
 *
 * <p>Provides: {@link BedtimeService} and the {@link LocalClock} it uses. Requires: nothing. Publishes:
 * {@code BedtimeStarted}, {@code BedtimeEnded}. Installed before the aquarium, so the aquarium
 * (V10) can ask {@code ctx.require(BedtimeService.class)} to let the fish sleep and to close
 * feeding at night.
 */
public final class BedtimeFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "bedtime";

    private static final int SECTION_VERSION = 1;

    private final BedtimeConfig config;
    private final LocalClock clock;
    private final Gson gson = new Gson();
    private BedtimeService service;

    /**
     * @param config times
     * @param clock  local time of the device
     */
    public BedtimeFeature(BedtimeConfig config, LocalClock clock) {
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        Section saved = ctx.savedSection()
                .map(s -> gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(s.version(), s.data()), Section.class))
                .orElse(new Section(null, null));
        service = new BedtimeService(config, clock, ctx.events(), ctx::requestSave,
                parse(saved.goodnightShownFor()), parse(saved.goodMorningShownFor()));
        ctx.provide(BedtimeService.class, service);
        // Also offered on its own, so the developer tools can shift it (AdjustableLocalClock).
        ctx.provide(LocalClock.class, clock);
    }

    @Override
    public void tick(double dtSeconds) {
        service.checkTransition();
    }

    @Override
    public Optional<SaveSection> save() {
        Section section = new Section(service.goodnightShownFor().map(LocalDate::toString).orElse(null),
                service.goodMorningShownFor().map(LocalDate::toString).orElse(null));
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(section)));
    }

    private static LocalDate parse(String date) {
        return date == null || date.isEmpty() ? null : LocalDate.parse(date);
    }

    /** Shape of the save section: {@code { "goodnightShownFor": "2026-11-19", "goodMorningShownFor": "2026-11-20" }}. */
    private record Section(String goodnightShownFor, String goodMorningShownFor) {
    }
}
