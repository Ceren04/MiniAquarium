package com.miniakvaryum.domain.feature.aquarium.api.food;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import org.junit.jupiter.api.Test;

/**
 * Tests for pellets and claims (specification section 9.4). This class is in the same package
 * as {@link FoodField}, so it can call {@code field.add(id, position)} directly.
 */
class FoodFieldTest {

    private final AquariumConfig config = AquariumConfig.defaults();
    private final FoodField field = new FoodField(config);

    private Pellet pellet(String id) {
        return field.pellets().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void nearestFreeReturnsTheClosestFreePellet() {
        field.add("far", new Vec2(900, 300));
        field.add("near", new Vec2(520, 300));
        field.add("mid", new Vec2(700, 300));

        Pellet nearest = field.nearestFree(new Vec2(500, 300)).orElseThrow();

        assertThat(nearest.id()).isEqualTo("near");
    }

    @Test
    void nearestFreeIgnoresClaimedPellets() {
        field.add("near", new Vec2(520, 300));
        field.add("far", new Vec2(900, 300));
        field.tryClaim("near", "fish-1");

        Pellet nearest = field.nearestFree(new Vec2(500, 300)).orElseThrow();

        assertThat(nearest.id()).isEqualTo("far");
    }

    @Test
    void nearestFreeIsEmptyWithoutFreePellets() {
        assertThat(field.nearestFree(Vec2.ZERO)).isEmpty();

        field.add("only", new Vec2(100, 100));
        field.tryClaim("only", "fish-1");

        assertThat(field.nearestFree(Vec2.ZERO)).isEmpty();
    }

    @Test
    void firstClaimSucceedsAndSecondClaimFails() {
        field.add("p", new Vec2(100, 100));

        assertThat(field.tryClaim("p", "fish-1")).isTrue();
        assertThat(field.tryClaim("p", "fish-2")).isFalse();

        assertThat(pellet("p").state()).isEqualTo(PelletState.CLAIMED);
        assertThat(pellet("p").claimedBy()).isEqualTo("fish-1");
    }

    @Test
    void claimOfUnknownPelletFails() {
        assertThat(field.tryClaim("does-not-exist", "fish-1")).isFalse();
    }

    @Test
    void releaseByTheOwnerFreesThePellet() {
        field.add("p", new Vec2(100, 100));
        field.tryClaim("p", "fish-1");

        field.release("p", "fish-1");

        assertThat(pellet("p").state()).isEqualTo(PelletState.FREE);
        assertThat(pellet("p").claimedBy()).isNull();
        assertThat(pellet("p").claimAgeS()).isZero();
    }

    @Test
    void releaseBySomeoneElseDoesNothing() {
        field.add("p", new Vec2(100, 100));
        field.tryClaim("p", "fish-1");

        field.release("p", "fish-2");

        assertThat(pellet("p").state()).isEqualTo(PelletState.CLAIMED);
        assertThat(pellet("p").claimedBy()).isEqualTo("fish-1");
    }

    @Test
    void removeTakesThePelletOutOfTheWater() {
        field.add("a", new Vec2(100, 100));
        field.add("b", new Vec2(200, 100));

        field.remove("a");
        field.remove("unknown-ids-are-ignored");

        assertThat(field.pellets()).extracting(Pellet::id).containsExactly("b");
    }

    @Test
    void pelletSinksAndStopsAboveTheFloor() {
        field.add("p", new Vec2(100, 620));

        field.update(1.0);
        assertThat(pellet("p").position().y()).isEqualTo(620 - config.food().pelletSinkSpeed());

        // Far more sinking time than the way to the floor, but less than the lifetime.
        for (int i = 0; i < 20; i++) {
            field.update(1.0);
        }

        assertThat(pellet("p").position().y()).isEqualTo(config.world().waterFloor() + 10);
        assertThat(pellet("p").position().x()).isEqualTo(100);
    }

    @Test
    void uneatenPelletDissolvesAfterItsLifetime() {
        double lifetime = config.food().pelletLifetimeS();
        field.add("p", new Vec2(100, 300));

        field.update(lifetime - 1);
        assertThat(field.pellets()).hasSize(1);

        field.update(2);
        assertThat(field.pellets()).isEmpty();
    }

    @Test
    void claimedPelletDoesNotDissolve() {
        double lifetime = config.food().pelletLifetimeS();
        field.add("p", new Vec2(100, 300));
        field.update(lifetime - 1);
        field.tryClaim("p", "fish-1");

        // The pellet is now older than its lifetime, but the claim is only 2 seconds old.
        field.update(2);

        assertThat(field.pellets()).hasSize(1);
        assertThat(pellet("p").state()).isEqualTo(PelletState.CLAIMED);
    }

    @Test
    void claimTimeoutMakesThePelletFreeAgain() {
        double timeout = config.food().claimTimeoutS();
        field.add("p", new Vec2(100, 300));
        field.tryClaim("p", "fish-1");

        field.update(timeout - 0.1);
        assertThat(pellet("p").state()).isEqualTo(PelletState.CLAIMED);

        field.update(0.2);
        assertThat(pellet("p").state()).isEqualTo(PelletState.FREE);
        assertThat(pellet("p").claimedBy()).isNull();
        assertThat(pellet("p").claimAgeS()).isZero();
    }
}