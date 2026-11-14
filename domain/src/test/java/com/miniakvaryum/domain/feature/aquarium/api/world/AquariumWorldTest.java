package com.miniakvaryum.domain.feature.aquarium.api.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.util.JavaRandomSource;
import com.miniakvaryum.domain.core.util.RandomSource;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.api.fish.ActionType;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishBrain;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodService;
import com.miniakvaryum.domain.feature.aquarium.api.food.Pellet;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for the simulation loop (specification section 9.4).
 *
 * <p>The world is put together by hand from real parts, so that each test chooses the fish
 * positions and the random source. A new game has 5 food. {@code game.random} is the scripted
 * source (it fails when it runs out of values); {@code JavaRandomSource} is a seeded real one.
 */
class AquariumWorldTest {

    private static final double FRAME = 1.0 / 60;
    private static final double TOLERANCE = 1e-9;

    private final AquariumConfig config = AquariumConfig.defaults();
    private final TestGame game = new TestGame();

    private GameSession session;
    private Wallet wallet;
    private FoodField field;
    private List<FishEntity> fish;
    private AquariumWorld world;

    /** Builds a world with one fish per position, named fish-1, fish-2 ... */
    private void startWorld(RandomSource random, Vec2... positions) {
        session = game.start(new EconomyFeature(EconomyConfig.defaults()));
        wallet = session.get(Wallet.class);
        field = new FoodField(config);
        FoodService foodService = new FoodService(field, wallet, session.time(), game.ids, config,
                session.events());
        fish = new ArrayList<>();
        for (int i = 0; i < positions.length; i++) {
            fish.add(new FishEntity(new FishData("fish-" + (i + 1), null, 0, 0), positions[i]));
        }
        world = new AquariumWorld(config, fish, field, foodService, new FishBrain(config, random));
    }

    private long stock() {
        return wallet.balance(Currency.FOOD);
    }

    @Test
    void updateAdvancesPelletsAndFish() {
        startWorld(new JavaRandomSource(1), new Vec2(200, 300));
        world.dropPellet(900).orElseThrow();
        Pellet pellet = field.pellets().get(0);
        double startY = pellet.position().y();

        world.update(FRAME);

        assertThat(pellet.position().y()).isLessThan(startY);
        // The idle fish chose an action, which always lasts for some time.
        assertThat(fish.get(0).state().remainingS()).isGreaterThan(0);
    }

    @Test
    void longFramesAreCutToTheMaximumStep() {
        startWorld(new JavaRandomSource(1), new Vec2(200, 300));
        world.dropPellet(900).orElseThrow();
        Pellet pellet = field.pellets().get(0);
        double startY = pellet.position().y();

        world.update(1.0);

        assertThat(fish.get(0).aquariumMillis()).isEqualTo(Math.round(AquariumWorld.MAX_STEP_SECONDS * 1000));
        assertThat(pellet.position().y())
                .isCloseTo(startY - config.food().pelletSinkSpeed() * AquariumWorld.MAX_STEP_SECONDS,
                        within(TOLERANCE));
    }

    @Test
    void updateOrderRotatesSoEveryFishGetsTheFirstTurn() {
        // Draws: the fish processed first picks EAT (int 4); the other two find no free pellet,
        // so they pick IDLE (int 3, then a duration and a look-around double each).
        game.random.ints(4, 3, 3);
        game.random.doubles(0.5, 0.5, 0.5, 0.5);
        startWorld(game.random, new Vec2(500, 600), new Vec2(520, 600), new Vec2(480, 600));
        String pelletId = world.dropPellet(500).orElseThrow();

        world.update(FRAME);

        // The first frame starts at index 1 (tick 1 % 3 fish), not at index 0.
        assertThat(fish.get(1).state().action()).isEqualTo(ActionType.EAT);
        assertThat(fish.get(1).state().claimedPelletId()).isEqualTo(pelletId);
        assertThat(fish.get(0).state().claimedPelletId()).isNull();
        assertThat(fish.get(2).state().claimedPelletId()).isNull();
        assertThat(fish.get(0).state().action()).isEqualTo(ActionType.IDLE);
        assertThat(fish.get(2).state().action()).isEqualTo(ActionType.IDLE);
    }

    @Test
    void everyFishGetsOlderByTheFrameTime() {
        startWorld(new JavaRandomSource(7), new Vec2(200, 300), new Vec2(600, 300), new Vec2(1000, 300));

        world.update(0.05);

        assertThat(fish).extracting(FishEntity::aquariumMillis).containsExactly(50L, 50L, 50L);
    }

    @Test
    void threeFishAndOnePelletEndWithExactlyOneFeeding() {
        startWorld(new JavaRandomSource(42), new Vec2(500, 600), new Vec2(520, 600), new Vec2(480, 600));
        world.dropPellet(500).orElseThrow();

        // At most 50 seconds, which is less than the lifetime after which an uneaten pellet dissolves.
        for (int frame = 0; frame < 3000 && !field.pellets().isEmpty(); frame++) {
            world.update(FRAME);
        }

        assertThat(field.pellets()).as("the pellet was eaten, not left in the water").isEmpty();
        assertThat(fish.stream().mapToInt(FishEntity::feedCount).sum()).isEqualTo(1);
        assertThat(stock()).isEqualTo(4);
    }

    @Test
    void dropPelletPutsAPelletIntoTheWater() {
        startWorld(new JavaRandomSource(1), new Vec2(200, 300));

        String id = world.dropPellet(500).orElseThrow();

        assertThat(field.pellets()).extracting(Pellet::id).containsExactly(id);
    }

    @Test
    void dropPelletWithoutFoodReturnsEmpty() {
        startWorld(new JavaRandomSource(1), new Vec2(200, 300));
        TxResult drained = wallet.debit(Currency.FOOD, 5, "test", "test:drain", session.time().nowMillis());
        assertThat(drained).isEqualTo(TxResult.OK);

        assertThat(world.dropPellet(500)).isEmpty();
        assertThat(field.pellets()).isEmpty();
    }
}