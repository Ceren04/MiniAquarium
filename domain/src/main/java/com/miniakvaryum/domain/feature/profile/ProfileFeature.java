package com.miniakvaryum.domain.feature.profile;

import com.google.gson.Gson;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.profile.api.ProfileService;
import java.util.Optional;

/**
 * Profile feature: remembers the favorite fish.
 *
 * <p>Provides: {@link ProfileService}. Requires: {@link AquariumWorld}.
 */
public final class ProfileFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "profile";

    private static final int SECTION_VERSION = 1;

    private final Gson gson = new Gson();
    private ProfileService service;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        String favorite = ctx.savedSection()
                .map(s -> gson.fromJson(new SaveMigrator(SECTION_VERSION).migrate(s.version(), s.data()), Section.class))
                .map(Section::favoriteFishId)
                .orElse("");
        AquariumWorld world = ctx.require(AquariumWorld.class);
        service = new ProfileService(favorite, () -> world.fish().stream().map(FishEntity::toData).toList(),
                ctx::requestSave);
        ctx.provide(ProfileService.class, service);
    }

    @Override
    public Optional<SaveSection> save() {
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(new Section(service.favoriteFishId()))));
    }

    /** Shape of the save section: {@code { "favoriteFishId": "f-2" }}. */
    private record Section(String favoriteFishId) {
    }
}
