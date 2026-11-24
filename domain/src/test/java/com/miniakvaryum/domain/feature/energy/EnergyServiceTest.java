package com.miniakvaryum.domain.feature.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.energy.api.EnergyService;
import com.miniakvaryum.domain.feature.energy.api.SpendResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnergyServiceTest {

    private static final long HOUR = 3_600_000L;

    private final List<FishData> fish = new ArrayList<>(List.of(fish("a"), fish("b"), fish("c")));
    private int saves;

    private static FishData fish(String id) {
        return new FishData(id, id, 0, 0L);
    }

    private EnergyService service(double value) {
        return new EnergyService(value, List.of(), EnergyConfig.defaults(), () -> fish, FishLevels.ALL_LEVEL_ONE,
                () -> saves++);
    }

    @Test
    void threeFishOfLevelOneGiveTenPerHour() {
        EnergyService energy = service(0);
        assertThat(energy.ratePerHour()).isEqualTo(10.0);
        energy.advance(HOUR);
        assertThat(energy.energy()).isEqualTo(10);
    }

    @Test
    void smallStepsAddUpWithoutLoss() {
        EnergyService energy = service(0);
        // One hour in frames of 1/60 s.
        for (int i = 0; i < 60 * 60 * 60; i++) {
            energy.advanceSeconds(1.0 / 60.0);
        }
        assertThat(energy.exactValue()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void energyStopsAtOneHundred() {
        EnergyService energy = service(95);
        energy.advance(24 * HOUR);
        assertThat(energy.energy()).isEqualTo(100);
        assertThat(energy.max()).isEqualTo(100);
        assertThat(service(250).energy()).isEqualTo(100);
        assertThat(service(-3).energy()).isZero();
        assertThat(service(Double.NaN).energy()).isZero();
    }

    @Test
    void negativeOrZeroTimeChangesNothing() {
        EnergyService energy = service(40);
        energy.advance(-HOUR);
        energy.advance(0);
        energy.advanceSeconds(-1);
        assertThat(energy.exactValue()).isEqualTo(40.0);
    }

    @Test
    void spendingTakesTheAmountOnce() {
        EnergyService energy = service(45);
        assertThat(energy.spend(20, "memory:1")).isEqualTo(SpendResult.OK);
        assertThat(energy.energy()).isEqualTo(25);
        assertThat(saves).isEqualTo(1);
        assertThat(energy.spend(20, "memory:1")).isEqualTo(SpendResult.ALREADY_APPLIED);
        assertThat(energy.energy()).isEqualTo(25);
        assertThat(energy.spentRefIds()).containsExactly("memory:1");
    }

    @Test
    void nineteenIsNotEnoughForAGame() {
        EnergyService energy = service(19.99);
        assertThat(energy.canSpend(energy.gameCost())).isFalse();
        assertThat(energy.spend(20, "memory:1")).isEqualTo(SpendResult.NOT_ENOUGH);
        assertThat(energy.exactValue()).isEqualTo(19.99);
        assertThat(saves).isZero();
        assertThatThrownBy(() -> energy.spend(0, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moreFishFillFaster() {
        EnergyService energy = service(0);
        fish.add(fish("d"));
        assertThat(energy.ratePerHour()).isEqualTo(13.0);
        fish.clear();
        assertThat(energy.ratePerHour()).isEqualTo(1.0);
    }

    @Test
    void levelsCount() {
        EnergyService energy = new EnergyService(0, List.of(), EnergyConfig.defaults(), () -> fish, f -> 3, () -> {
        });
        // 1 + 3 x (2 + 3)
        assertThat(energy.ratePerHour()).isEqualTo(16.0);
    }

    @Test
    void minutesUntilAGame() {
        EnergyService energy = service(15);
        // 5 missing at 10 per hour: 30 minutes.
        assertThat(energy.minutesUntil(20)).isEqualTo(30);
        assertThat(service(20).minutesUntil(20)).isZero();
        fish.clear();
        // 5 missing at 1 per hour.
        assertThat(energy.minutesUntil(20)).isEqualTo(300);
        EnergyService stopped = new EnergyService(0, List.of(), new EnergyConfig(0, 0, 0, 0, 100, 20), () -> fish,
                FishLevels.ALL_LEVEL_ONE, () -> {
                });
        assertThat(stopped.minutesUntil(20)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void onlyTheLastFiftyRefIdsAreKept() {
        EnergyService energy = service(100);
        for (int i = 0; i < EnergyService.REMEMBERED_REF_IDS + 5; i++) {
            energy.setForDevelopment(100);
            energy.spend(1, "r" + i);
        }
        assertThat(energy.spentRefIds()).hasSize(EnergyService.REMEMBERED_REF_IDS).startsWith("r5");
    }

    @Test
    void developerValueIsClamped() {
        EnergyService energy = service(50);
        energy.setForDevelopment(-5);
        assertThat(energy.energy()).isZero();
        energy.setForDevelopment(500);
        assertThat(energy.energy()).isEqualTo(100);
    }

    @Test
    void configChecksItsValues() {
        assertThatThrownBy(() -> new EnergyConfig(-1, 2, 1, 100, 100, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnergyConfig(1, 2, 1, 101, 100, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnergyConfig(1, 2, 1, 50, 100, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnergyConfig(1, 2, 1, 0, 0, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
