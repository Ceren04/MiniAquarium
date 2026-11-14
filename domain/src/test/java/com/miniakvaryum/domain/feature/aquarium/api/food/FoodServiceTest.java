package com.miniakvaryum.domain.feature.aquarium.api.food;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.AquariumFeature;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for feeding and food accounting (specification section 9.4). A new game has 5 food and
 * three fish; nothing in these tests ticks the game, so the fish never move.
 */
class FoodServiceTest {

    private final AquariumConfig config = AquariumConfig.defaults();
    private final TestGame game = new TestGame();

    private GameSession session;
    private FoodService food;
    private AquariumWorld world;
    private Wallet wallet;

    @BeforeEach
    void startGame() {
        game.random.doubles(0.5, 0.5, 0.5, 0.5, 0.5, 0.5); // start positions of the three fish
        session = game.start(new EconomyFeature(EconomyConfig.defaults()), new AquariumFeature(config));
        food = session.get(FoodService.class);
        world = session.get(AquariumWorld.class);
        wallet = session.get(Wallet.class);
    }

    private FishEntity fish(int index) {
        return world.fish().get(index);
    }

    private String dropPellet(double x) {
        return food.drop(x).orElseThrow();
    }

    private Pellet pellet(String id) {
        return world.food().pellets().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    private long stock() {
        return wallet.balance(Currency.FOOD);
    }

    @Test
    void availableIsStockMinusPelletsInTheWater() {
        assertThat(food.available()).isEqualTo(5);

        String first = dropPellet(300);
        dropPellet(600);
        world.food().tryClaim(first, fish(0).id());

        assertThat(food.available()).isEqualTo(3);
    }

    @Test
    void dropCreatesAFreePelletAtTheStartHeight() {
        String id = dropPellet(500);

        assertThat(pellet(id).state()).isEqualTo(PelletState.FREE);
        assertThat(pellet(id).position()).isEqualTo(new Vec2(500, config.food().pelletStartY()));
    }

    @Test
    void dropClampsXIntoTheSwimArea() {
        String left = dropPellet(-100);
        String right = dropPellet(5000);

        assertThat(pellet(left).position().x()).isEqualTo(config.world().minFishX());
        assertThat(pellet(right).position().x()).isEqualTo(config.world().maxFishX());
    }

    @Test
    void dropDoesNotChangeTheWallet() {
        int entriesBefore = wallet.entries().size();

        dropPellet(500);

        assertThat(stock()).isEqualTo(5);
        assertThat(wallet.entries()).hasSize(entriesBefore);
    }

    @Test
    void dropIsRefusedWhenNothingIsAvailable() {
        // 5 food and 5 pellets in the water: the whole stock is reserved.
        for (int i = 0; i < 5; i++) {
            dropPellet(100 + i * 100);
        }

        assertThat(food.drop(700)).isEmpty();
        assertThat(world.food().pellets()).hasSize(5);
        assertThat(stock()).isEqualTo(5);
    }

    @Test
    void stockDecreasesOnlyOnCompleteEating() {
        String id = dropPellet(500);
        assertThat(stock()).isEqualTo(5);

        world.food().tryClaim(id, fish(0).id());
        assertThat(stock()).isEqualTo(5);

        food.completeEating(fish(0), id);
        assertThat(stock()).isEqualTo(4);
    }

    @Test
    void completeEatingCountsTheFeedingAndRemovesThePellet() {
        String id = dropPellet(500);
        world.food().tryClaim(id, fish(0).id());

        EatResult result = food.completeEating(fish(0), id);

        assertThat(result).isEqualTo(EatResult.OK);
        assertThat(fish(0).feedCount()).isEqualTo(1);
        assertThat(world.food().pellets()).isEmpty();
    }

    @Test
    void completeEatingTwicePaysOnce() {
        String id = dropPellet(500);
        world.food().tryClaim(id, fish(0).id());

        EatResult first = food.completeEating(fish(0), id);
        EatResult second = food.completeEating(fish(0), id);

        assertThat(first).isEqualTo(EatResult.OK);
        assertThat(second).isEqualTo(EatResult.CLAIM_LOST);
        assertThat(stock()).isEqualTo(4);
        assertThat(fish(0).feedCount()).isEqualTo(1);
    }

    @Test
    void completeEatingWithoutClaimChangesNothing() {
        String id = dropPellet(500);

        // Nobody claimed it.
        assertThat(food.completeEating(fish(0), id)).isEqualTo(EatResult.CLAIM_LOST);

        // Another fish claimed it.
        world.food().tryClaim(id, fish(1).id());
        assertThat(food.completeEating(fish(0), id)).isEqualTo(EatResult.CLAIM_LOST);

        assertThat(stock()).isEqualTo(5);
        assertThat(fish(0).feedCount()).isZero();
        assertThat(pellet(id).claimedBy()).isEqualTo(fish(1).id());
    }

    @Test
    void completeEatingWithEmptyStockReleasesThePellet() {
        String id = dropPellet(500);
        world.food().tryClaim(id, fish(0).id());
        // Spend the whole stock behind the pellet's back.
        TxResult drained = wallet.debit(Currency.FOOD, 5, "test", "test:drain", session.time().nowMillis());
        assertThat(drained).isEqualTo(TxResult.OK);

        EatResult result = food.completeEating(fish(0), id);

        assertThat(result).isEqualTo(EatResult.INSUFFICIENT_FOOD);
        assertThat(fish(0).feedCount()).isZero();
        assertThat(pellet(id).state()).isEqualTo(PelletState.FREE);
        assertThat(pellet(id).claimedBy()).isNull();
    }

    @Test
    void uneatenPelletDissolvesWithoutCost() {
        dropPellet(500);

        world.food().update(config.food().pelletLifetimeS() + 1);

        assertThat(world.food().pellets()).isEmpty();
        assertThat(stock()).isEqualTo(5);
    }

    @Test
    void threeFishAndOnePelletGiveExactlyOneEater() {
        String id = dropPellet(500);

        int claims = 0;
        for (FishEntity fish : world.fish()) {
            if (world.food().tryClaim(id, fish.id())) {
                claims++;
            }
        }
        assertThat(claims).isEqualTo(1);

        // All three fish try to finish eating: only the owner of the claim is paid.
        int paid = 0;
        for (FishEntity fish : world.fish()) {
            if (food.completeEating(fish, id) == EatResult.OK) {
                paid++;
            }
        }
        assertThat(paid).isEqualTo(1);
        assertThat(stock()).isEqualTo(4);
        assertThat(world.fish().stream().mapToInt(FishEntity::feedCount).sum()).isEqualTo(1);
    }

    @Test
    void completeEatingPublishesFishFed() {
        List<FishFed> events = new ArrayList<>();
        session.events().subscribe(FishFed.class, events::add);
        String id = dropPellet(500);
        world.food().tryClaim(id, fish(0).id());

        food.completeEating(fish(0), id);

        assertThat(events).containsExactly(new FishFed(fish(0).id(), 1));
    }
}