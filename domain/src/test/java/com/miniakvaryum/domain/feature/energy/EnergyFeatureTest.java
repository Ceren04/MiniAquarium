package com.miniakvaryum.domain.feature.energy;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.AquariumFeature;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.energy.api.EnergyService;
import com.miniakvaryum.domain.testsupport.TestGame;
import org.junit.jupiter.api.Test;

/** Wiring of the energy feature in the full game: start value, save, offline time. */
class EnergyFeatureTest {

    private static final long HOUR = 3_600_000L;

    private final TestGame game = new TestGame();

    /** Economy, aquarium and energy with the given config; the fish start in the middle. */
    private GameSession startWith(EnergyConfig config) {
        int fishCount = AquariumConfig.defaults().fish().initialFishCount();
        for (int i = 0; i < fishCount * 2; i++) {
            game.random.doubles(0.5);
        }
        return game.start(new EconomyFeature(EconomyConfig.defaults()), new AquariumFeature(AquariumConfig.defaults()),
                new EnergyFeature(config, FishLevels.ALL_LEVEL_ONE));
    }

    private EnergyService energy(GameSession session) {
        return session.get(EnergyService.class);
    }

    @Test
    void newGameStartsFull() {
        assertThat(energy(game.startFullGame()).energy()).isEqualTo(100);
        assertThat(game.startFullGame().get(EnergyService.class).gameCost()).isEqualTo(20);
    }

    @Test
    void energyIsKeptAcrossARestart() {
        GameSession session = startWith(EnergyConfig.defaults());
        energy(session).setForDevelopment(37.5);
        session.saveNow();
        assertThat(energy(startWith(EnergyConfig.defaults())).exactValue()).isEqualTo(37.5);
    }

    @Test
    void offlineHoursFillTheEnergy() {
        GameSession session = startWith(EnergyConfig.defaults());
        energy(session).setForDevelopment(0);
        session.saveNow();
        game.clock.advanceMillis(HOUR);
        assertThat(energy(startWith(EnergyConfig.defaults())).energy()).isEqualTo(10);
    }

    @Test
    void sixHoursGiveSixty() {
        GameSession session = startWith(EnergyConfig.defaults());
        energy(session).setForDevelopment(0);
        session.saveNow();
        game.clock.advanceMillis(6 * HOUR);
        assertThat(energy(startWith(EnergyConfig.defaults())).energy()).isEqualTo(60);
    }

    @Test
    void atMostTwentyFourHoursCount() {
        // Slow config: 1 per hour, so the 24 hour limit is visible below 100.
        EnergyConfig slow = new EnergyConfig(1, 0, 0, 0, 100, 20);
        startWith(slow).saveNow();
        game.clock.advanceMillis(30 * HOUR);
        EnergyService energy = startWith(slow).get(EnergyService.class);
        assertThat(energy.energy()).isEqualTo(24);
    }

    @Test
    void settingTheDateBackGivesNothing() {
        GameSession session = startWith(EnergyConfig.defaults());
        energy(session).setForDevelopment(0);
        session.saveNow();
        game.boot.set("boot-B");
        game.clock.setMonotonicNanos(1_000L);
        game.clock.shiftWallMillis(-72 * HOUR);
        assertThat(energy(startWith(EnergyConfig.defaults())).energy()).isZero();
    }

    @Test
    void playingTimeFillsTheEnergy() {
        int fishCount = AquariumConfig.defaults().fish().initialFishCount();
        for (int i = 0; i < fishCount * 2; i++) {
            game.random.doubles(0.5);
        }
        EnergyFeature feature = new EnergyFeature(EnergyConfig.defaults(), FishLevels.ALL_LEVEL_ONE);
        GameSession session = game.start(new EconomyFeature(EconomyConfig.defaults()),
                new AquariumFeature(AquariumConfig.defaults()), feature);
        energy(session).setForDevelopment(0);
        // 30 minutes of frames of 0.1 s (only the energy feature ticks; fish would need dice).
        for (int i = 0; i < 30 * 60 * 10; i++) {
            feature.tick(0.1);
        }
        assertThat(energy(session).energy()).isEqualTo(5);
    }
}
