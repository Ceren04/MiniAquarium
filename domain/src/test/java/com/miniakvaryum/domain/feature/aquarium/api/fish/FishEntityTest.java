package com.miniakvaryum.domain.feature.aquarium.api.fish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.core.util.Vec2;
import org.junit.jupiter.api.Test;

class FishEntityTest {

    @Test
    void factsComeFromTheDataAndGoBackIntoIt() {
        FishData data = new FishData("a1", "Bubbles", 3, 7_200_000);

        FishEntity fish = Fishes.of(data, 100, 200);

        assertThat(fish.id()).isEqualTo("a1");
        assertThat(fish.name()).isEqualTo("Bubbles");
        assertThat(fish.feedCount()).isEqualTo(3);
        assertThat(fish.aquariumMillis()).isEqualTo(7_200_000);
        assertThat(fish.state().position()).isEqualTo(new Vec2(100, 200));
        assertThat(fish.toData()).isEqualTo(data);
    }

    @Test
    void recordFeedingCountsAndReleasesTheClaim() {
        FishEntity fish = Fishes.eating(Fishes.at("a1", 0, 0), "pellet-1", 0.5);
        assertThat(fish.state().claimedPelletId()).isEqualTo("pellet-1");

        fish.recordFeeding();

        assertThat(fish.feedCount()).isEqualTo(1);
        assertThat(fish.state().claimedPelletId()).isNull();
    }

    @Test
    void ageOnlyGrows() {
        FishEntity fish = Fishes.at("a1", 0, 0);

        fish.addAquariumMillis(1_000);
        fish.addAquariumMillis(0);

        assertThat(fish.aquariumMillis()).isEqualTo(1_000);
        assertThatThrownBy(() -> fish.addAquariumMillis(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void facingMustBePlusOrMinusOne() {
        FishEntity fish = Fishes.at("a1", 0, 0);

        assertThat(Fishes.facing(fish, -1).state().facing()).isEqualTo(-1);
        assertThatThrownBy(() -> Fishes.facing(fish, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
