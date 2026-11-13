package com.miniakvaryum.domain.feature.aquarium;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.api.fish.ActionType;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodService;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests the wiring of the feature (install, save, load). The simulation rules have their own tests. */
class AquariumFeatureTest {

    private static final long HOUR = 3_600_000L;

    private final TestGame game = new TestGame();

    /** Starts economy + aquarium. Scripts the random start positions: x then y for each fish. */
    private GameSession start(double... positionDraws) {
        game.random.doubles(positionDraws);
        return game.start(new EconomyFeature(EconomyConfig.defaults()),
                new AquariumFeature(AquariumConfig.defaults()));
    }

    @Test
    void newGameCreatesThreeUnnamedFishInsideTheSwimArea() {
        GameSession session = start(0.0, 0.0, 0.5, 0.5, 1.0, 1.0);

        List<FishEntity> fish = session.get(AquariumWorld.class).fish();

        assertThat(fish).extracting(FishEntity::toData).containsExactly(
                new FishData("id-1", null, 0, 0),
                new FishData("id-2", null, 0, 0),
                new FishData("id-3", null, 0, 0));
        // Random 0 / 0.5 / 1 map to the lower-left corner, the middle and the upper-right corner.
        assertThat(fish.get(0).state().position()).isEqualTo(new Vec2(70, 115));
        assertThat(fish.get(1).state().position()).isEqualTo(new Vec2(640, 360));
        assertThat(fish.get(2).state().position()).isEqualTo(new Vec2(1210, 605));
    }

    @Test
    void newFishStartIdleFacingRightWithoutAClaim() {
        FishEntity fish = start(0.5, 0.5, 0.5, 0.5, 0.5, 0.5).get(AquariumWorld.class).fish().get(0);

        assertThat(fish.state().action()).isEqualTo(ActionType.IDLE);
        assertThat(fish.state().facing()).isEqualTo(1);
        assertThat(fish.state().velocity()).isEqualTo(Vec2.ZERO);
        assertThat(fish.state().remainingS()).isZero();
        assertThat(fish.state().claimedPelletId()).isNull();
    }

    @Test
    void featureProvidesWorldAndFoodService() {
        GameSession session = start(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);

        assertThat(session.get(AquariumWorld.class).food().pellets()).isEmpty();
        assertThat(session.find(FoodService.class)).isPresent();
    }

    @Test
    void fishFactsSurviveARestartButPositionsAreNotSaved() {
        GameSession session = start(0.0, 0.0, 0.5, 0.5, 1.0, 1.0);
        FishEntity first = session.get(AquariumWorld.class).fish().get(0);
        first.recordFeeding();
        first.recordFeeding();
        first.addAquariumMillis(5 * HOUR);
        session.saveNow();

        List<FishEntity> reloaded = start(0.5, 0.5, 0.5, 0.5, 0.5, 0.5).get(AquariumWorld.class).fish();

        assertThat(reloaded).extracting(FishEntity::id).containsExactly("id-1", "id-2", "id-3");
        assertThat(reloaded.get(0).feedCount()).isEqualTo(2);
        assertThat(reloaded.get(0).aquariumMillis()).isEqualTo(5 * HOUR);
        assertThat(reloaded.get(0).state().position()).isEqualTo(new Vec2(640, 360));
    }

    @Test
    void creditedOfflineTimeIsAddedToTheAgeOfEveryFish() {
        GameSession session = start(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        session.saveNow();

        game.clock.advanceMillis(2 * HOUR);
        List<FishEntity> reloaded = start(0.5, 0.5, 0.5, 0.5, 0.5, 0.5).get(AquariumWorld.class).fish();

        assertThat(reloaded).extracting(FishEntity::aquariumMillis).containsExactly(2 * HOUR, 2 * HOUR, 2 * HOUR);
    }

    @Test
    void saveSectionContainsOnlyTheFacts() {
        GameSession session = start(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);
        session.saveNow();

        JsonObject first = game.store.lastSaved().sections().get("aquarium").data()
                .getAsJsonObject().getAsJsonArray("fish").get(0).getAsJsonObject();

        assertThat(first.keySet()).containsExactlyInAnyOrder("id", "feedCount", "aquariumMillis");
        assertThat(first.get("id").getAsString()).isEqualTo("id-1");
    }

    @Test
    void gameLoopRunsEvenWhileTheSimulationIsASkeleton() {
        GameSession session = start(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);

        session.tick(0.016);
        session.tick(0.016);
    }
}
