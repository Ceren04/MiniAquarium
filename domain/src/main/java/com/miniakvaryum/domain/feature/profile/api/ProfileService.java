package com.miniakvaryum.domain.feature.profile.api;

import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The part of the profile that lives in the game save: the favorite fish. Names, birthday and
 * picture belong to the account ({@code AccountService}).
 */
public final class ProfileService {

    private final Supplier<List<FishData>> fish;
    private final Runnable onChange;
    private String favoriteFishId;

    /**
     * @param favoriteFishId saved choice, or empty
     * @param fish           the fish in the aquarium now
     * @param onChange       asks for a save
     */
    public ProfileService(String favoriteFishId, Supplier<List<FishData>> fish, Runnable onChange) {
        this.favoriteFishId = favoriteFishId == null ? "" : favoriteFishId;
        this.fish = Objects.requireNonNull(fish, "fish");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /**
     * The favorite fish: the chosen one if it is still in the aquarium, otherwise the fish that
     * has lived there longest. Empty only with an empty aquarium.
     *
     * <p>When fish levels exist (V10), the fallback should become "highest level, then oldest".
     */
    public Optional<FishData> favorite() {
        List<FishData> all = fish.get();
        Optional<FishData> chosen = all.stream().filter(f -> f.id().equals(favoriteFishId)).findFirst();
        if (chosen.isPresent()) {
            return chosen;
        }
        return all.stream().max(Comparator.comparingLong(FishData::aquariumMillis));
    }

    /** Returns true if the child picked the favorite (not the automatic choice). */
    public boolean isChosen() {
        return fish.get().stream().anyMatch(f -> f.id().equals(favoriteFishId));
    }

    /**
     * Picks the favorite fish.
     *
     * @throws IllegalArgumentException if no fish with this id is in the aquarium
     */
    public void choose(String fishId) {
        if (fish.get().stream().noneMatch(f -> f.id().equals(fishId))) {
            throw new IllegalArgumentException("No fish with id " + fishId);
        }
        if (!fishId.equals(favoriteFishId)) {
            favoriteFishId = fishId;
            onChange.run();
        }
    }

    /** The saved choice, for the save; may name a fish that is gone. */
    public String favoriteFishId() {
        return favoriteFishId;
    }
}
