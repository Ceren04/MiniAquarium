package com.miniakvaryum.domain.core.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.testsupport.FakeBootId;
import com.miniakvaryum.domain.testsupport.FakeClock;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Every test plays two sessions: the first one runs for a while and produces an anchor
 * (as if the game saved and closed), then something happens to the device, then a second
 * GameTime starts from that anchor.
 */
class GameTimeTest {

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;
    private static final long MAX_GAP = DAY;

    private final FakeClock clock = new FakeClock(1_700_000_000_000L, 5_000_000_000L);
    private final FakeBootId boot = FakeBootId.of("boot-A");

    /** First session: starts a new game, plays one hour, returns what would be saved. */
    private TimeAnchor playOneHourAndSave() {
        GameTime first = new GameTime(clock, boot, MAX_GAP);
        first.startSession(Optional.empty());
        clock.advanceMillis(HOUR);
        return first.anchor();
    }

    private GameTime newSession() {
        return new GameTime(clock, boot, MAX_GAP);
    }

    @Test
    void firstRunStartsAtZeroWithNoGap() {
        GameTime time = newSession();

        CreditedGap gap = time.startSession(Optional.empty());

        assertThat(gap).isEqualTo(new CreditedGap(0, GapSource.FIRST_RUN, false));
        assertThat(time.nowMillis()).isZero();
    }

    @Test
    void sameBootCreditsMonotonicTime() {
        TimeAnchor saved = playOneHourAndSave();
        clock.advanceMillis(2 * HOUR);

        GameTime time = newSession();
        CreditedGap gap = time.startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(2 * HOUR, GapSource.MONOTONIC, false));
        assertThat(time.nowMillis()).isEqualTo(3 * HOUR);
    }

    @Test
    void sameBootIgnoresWallClockSetBack() {
        TimeAnchor saved = playOneHourAndSave();
        clock.advanceMillis(2 * HOUR);
        clock.shiftWallMillis(-10 * DAY);

        CreditedGap gap = newSession().startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(2 * HOUR, GapSource.MONOTONIC, false));
    }

    @Test
    void sameBootIgnoresWallClockSetForward() {
        TimeAnchor saved = playOneHourAndSave();
        clock.advanceMillis(2 * HOUR);
        clock.shiftWallMillis(3 * DAY);

        CreditedGap gap = newSession().startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(2 * HOUR, GapSource.MONOTONIC, false));
    }

    @Test
    void sameBootLongGapIsCappedToo() {
        TimeAnchor saved = playOneHourAndSave();
        clock.advanceMillis(3 * DAY);

        CreditedGap gap = newSession().startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(DAY, GapSource.MONOTONIC, true));
    }

    @Test
    void differentBootWallForwardIsCappedTo24Hours() {
        TimeAnchor saved = playOneHourAndSave();
        boot.set("boot-B");
        clock.setMonotonicNanos(1_000L);
        clock.shiftWallMillis(3 * DAY);

        GameTime time = newSession();
        CreditedGap gap = time.startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(DAY, GapSource.WALL, true));
        assertThat(time.nowMillis()).isEqualTo(HOUR + DAY);
    }

    @Test
    void differentBootWallBackwardCreditsNothing() {
        TimeAnchor saved = playOneHourAndSave();
        boot.set("boot-B");
        clock.setMonotonicNanos(1_000L);
        clock.shiftWallMillis(-HOUR);

        GameTime time = newSession();
        CreditedGap gap = time.startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(0, GapSource.WALL_NEGATIVE_CLAMPED, false));
        assertThat(time.nowMillis()).isEqualTo(HOUR);
    }

    @Test
    void differentBootShortWallGapIsCreditedInFull() {
        TimeAnchor saved = playOneHourAndSave();
        boot.set("boot-B");
        clock.setMonotonicNanos(1_000L);
        clock.shiftWallMillis(5 * HOUR);

        CreditedGap gap = newSession().startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(5 * HOUR, GapSource.WALL, false));
    }

    @Test
    void unknownBootIdUsesWallClock() {
        FakeBootId unknown = FakeBootId.unknown();
        GameTime first = new GameTime(clock, unknown, MAX_GAP);
        first.startSession(Optional.empty());
        TimeAnchor saved = first.anchor();
        assertThat(saved.bootId()).isEmpty();

        // Monotonic time says 2 h, but without a boot id it cannot be trusted.
        clock.advanceMillis(2 * HOUR);
        clock.shiftWallMillis(HOUR);

        CreditedGap gap = new GameTime(clock, unknown, MAX_GAP).startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(3 * HOUR, GapSource.WALL, false));
    }

    @Test
    void monotonicSmallerThanSavedUsesWallClock() {
        TimeAnchor saved = playOneHourAndSave();
        // Same boot id reported, but the counter is behind the saved reading: do not trust it.
        clock.setMonotonicNanos(saved.monotonicNanos() - 1);
        clock.shiftWallMillis(4 * HOUR);

        CreditedGap gap = newSession().startSession(Optional.of(saved));

        assertThat(gap).isEqualTo(new CreditedGap(4 * HOUR, GapSource.WALL, false));
    }

    @Test
    void nowMillisFollowsMonotonicClockOnly() {
        GameTime time = newSession();
        time.startSession(Optional.empty());

        clock.shiftWallMillis(5 * DAY);
        assertThat(time.nowMillis()).isZero();

        clock.advanceMillis(1_500);
        assertThat(time.nowMillis()).isEqualTo(1_500);

        clock.shiftWallMillis(-9 * DAY);
        assertThat(time.nowMillis()).isEqualTo(1_500);
    }

    @Test
    void anchorCapturesGameTimeAndAllClocks() {
        GameTime time = newSession();
        time.startSession(Optional.empty());
        clock.advanceMillis(HOUR);

        TimeAnchor anchor = time.anchor();

        assertThat(anchor).isEqualTo(
                new TimeAnchor(HOUR, clock.monotonicNanos(), clock.wallMillis(), "boot-A"));
    }

    @Test
    void startSessionTwiceIsAProgrammingError() {
        GameTime time = newSession();
        time.startSession(Optional.empty());

        assertThatThrownBy(() -> time.startSession(Optional.empty()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readingTimeBeforeStartIsAProgrammingError() {
        GameTime time = newSession();

        assertThatThrownBy(time::nowMillis).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(time::anchor).isInstanceOf(IllegalStateException.class);
    }
}
