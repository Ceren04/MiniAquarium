package com.miniakvaryum.domain.core.time;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.testsupport.FakeClock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AdjustableLocalClockTest {

    /** 2026-11-24 12:00 UTC. */
    private final FakeClock wall = new FakeClock(1_795_521_600_000L, 0L);

    @Test
    void showsTheWallClockInTheZone() {
        AdjustableLocalClock clock = new AdjustableLocalClock(wall, ZoneOffset.ofHours(3));
        assertThat(clock.now()).isEqualTo(LocalDateTime.of(2026, 11, 24, 15, 0));
        assertThat(clock.isShifted()).isFalse();
    }

    @Test
    void canBeShiftedAndResetWithoutTouchingTheDevice() {
        AdjustableLocalClock clock = new AdjustableLocalClock(wall, ZoneOffset.UTC);
        clock.setTimeOfDayForDevelopment(LocalTime.of(20, 59));
        assertThat(clock.now()).isEqualTo(LocalDateTime.of(2026, 11, 24, 20, 59));
        assertThat(clock.isShifted()).isTrue();
        // Runs on from there.
        wall.advanceMillis(60_000L);
        assertThat(clock.now()).isEqualTo(LocalDateTime.of(2026, 11, 24, 21, 0));

        clock.resetForDevelopment();
        assertThat(clock.now()).isEqualTo(LocalDateTime.of(2026, 11, 24, 12, 1));
        assertThat(AdjustableLocalClock.ofSystemZone(wall).isShifted()).isFalse();
    }
}
