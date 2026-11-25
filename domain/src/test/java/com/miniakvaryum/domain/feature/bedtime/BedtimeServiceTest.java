package com.miniakvaryum.domain.feature.bedtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeEnded;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeService;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeStarted;
import com.miniakvaryum.domain.testsupport.FixedLocalClock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BedtimeServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);

    private final FixedLocalClock clock = new FixedLocalClock(DAY.atTime(15, 0));
    private final EventBus events = new EventBus();
    private final List<Object> published = new ArrayList<>();
    private int saves;

    private BedtimeService service() {
        events.subscribe(BedtimeStarted.class, published::add);
        events.subscribe(BedtimeEnded.class, published::add);
        return new BedtimeService(BedtimeConfig.defaults(), clock, events, () -> saves++, null, null);
    }

    private boolean bedtimeAt(int hour, int minute) {
        clock.set(DAY.atTime(hour, minute));
        return service().isBedtime();
    }

    // ---------------------------------------------------------------- 6. hours

    @Test
    void bedtimeIsFromNineInTheEveningToSevenInTheMorning() {
        assertThat(bedtimeAt(20, 59)).isFalse();
        assertThat(bedtimeAt(21, 0)).isTrue();
        assertThat(bedtimeAt(23, 59)).isTrue();
        assertThat(bedtimeAt(0, 0)).isTrue();
        assertThat(bedtimeAt(6, 59)).isTrue();
        assertThat(bedtimeAt(7, 0)).isFalse();
        assertThat(bedtimeAt(12, 0)).isFalse();
    }

    @Test
    void bedtimeEndsAtTheNextSevenOClock() {
        BedtimeService bedtime = service();
        clock.set(DAY.atTime(22, 30));
        assertThat(bedtime.bedtimeEndsAt()).contains(DAY.plusDays(1).atTime(7, 0));
        clock.set(DAY.atTime(3, 0));
        assertThat(bedtime.bedtimeEndsAt()).contains(DAY.atTime(7, 0));
        clock.set(DAY.atTime(10, 0));
        assertThat(bedtime.bedtimeEndsAt()).isEmpty();
        assertThat(bedtime.endTime()).isEqualTo(LocalTime.of(7, 0));
    }

    // ---------------------------------------------------------------- 7. events

    @Test
    void startAndEndArePublishedOnceEach() {
        clock.set(DAY.atTime(20, 59));
        BedtimeService bedtime = service();
        bedtime.checkTransition();
        assertThat(published).isEmpty();

        clock.advance(Duration.ofMinutes(1));
        bedtime.checkTransition();
        bedtime.checkTransition();
        assertThat(published).containsExactly(new BedtimeStarted());

        clock.set(DAY.plusDays(1).atTime(7, 0));
        bedtime.checkTransition();
        bedtime.checkTransition();
        assertThat(published).containsExactly(new BedtimeStarted(), new BedtimeEnded());
    }

    @Test
    void startingAtNightPublishesNothing() {
        clock.set(DAY.atTime(23, 0));
        BedtimeService bedtime = service();
        bedtime.checkTransition();
        assertThat(published).isEmpty();
    }

    // ---------------------------------------------------------------- 8. good morning

    @Test
    void goodMorningOncePerDayBeforeNoon() {
        clock.set(DAY.atTime(7, 30));
        BedtimeService bedtime = service();
        assertThat(bedtime.consumeGoodMorning()).isTrue();
        assertThat(saves).isEqualTo(1);
        assertThat(bedtime.consumeGoodMorning()).isFalse();
        assertThat(bedtime.goodMorningShownFor()).contains(DAY);

        clock.set(DAY.plusDays(1).atTime(12, 30));
        assertThat(bedtime.consumeGoodMorning()).isFalse();
        clock.set(DAY.plusDays(1).atTime(6, 59));
        assertThat(bedtime.consumeGoodMorning()).isFalse();
        clock.set(DAY.plusDays(1).atTime(7, 0));
        assertThat(bedtime.consumeGoodMorning()).isTrue();
    }

    @Test
    void goodMorningIsRememberedAcrossARestart() {
        clock.set(DAY.atTime(8, 0));
        BedtimeService restarted = new BedtimeService(BedtimeConfig.defaults(), clock, events, () -> {
        }, null, DAY);
        assertThat(restarted.consumeGoodMorning()).isFalse();
    }

    // ---------------------------------------------------------------- 9. goodnight

    @Test
    void goodnightOncePerNightAlsoAfterMidnight() {
        clock.set(DAY.atTime(21, 5));
        BedtimeService bedtime = service();
        assertThat(bedtime.consumeGoodnight()).isTrue();
        assertThat(bedtime.consumeGoodnight()).isFalse();
        // 01:00 still belongs to the night that started on DAY.
        clock.set(DAY.plusDays(1).atTime(1, 0));
        assertThat(bedtime.consumeGoodnight()).isFalse();
        assertThat(bedtime.goodnightShownFor()).contains(DAY);
        // The next night.
        clock.set(DAY.plusDays(1).atTime(21, 0));
        assertThat(bedtime.consumeGoodnight()).isTrue();
    }

    @Test
    void openingAfterMidnightGreetsThePreviousEveningsNight() {
        clock.set(DAY.atTime(2, 0));
        BedtimeService bedtime = service();
        assertThat(bedtime.consumeGoodnight()).isTrue();
        assertThat(bedtime.goodnightShownFor()).contains(DAY.minusDays(1));
    }

    @Test
    void noGoodnightDuringTheDay() {
        clock.set(DAY.atTime(15, 0));
        BedtimeService bedtime = service();
        assertThat(bedtime.consumeGoodnight()).isFalse();
        assertThat(bedtime.goodnightShownFor()).isEmpty();
        assertThat(saves).isZero();
    }

    @Test
    void configChecksItsTimes() {
        assertThatThrownBy(() -> new BedtimeConfig(LocalTime.of(7, 0), LocalTime.of(21, 0), LocalTime.of(12, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BedtimeConfig(LocalTime.of(21, 0), LocalTime.of(7, 0), LocalTime.of(22, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BedtimeConfig(LocalTime.of(21, 0), LocalTime.of(7, 0), LocalTime.of(6, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void localDateTimeHelperIsStable() {
        assertThat(clock.now()).isEqualTo(LocalDateTime.of(2026, 11, 24, 15, 0));
    }

    @Test
    void developerReplayShowsTheGreetingAgainAtOnce() {
        clock.set(DAY.atTime(21, 0));
        BedtimeService bedtime = service();
        assertThat(bedtime.consumeGoodnight()).isTrue();
        bedtime.checkTransition();
        assertThat(published).isEmpty();

        bedtime.replayGreetingForDevelopment();
        bedtime.checkTransition();
        assertThat(published).hasSize(1).first().isInstanceOf(BedtimeStarted.class);
        assertThat(bedtime.consumeGoodnight()).isTrue();

        clock.set(DAY.plusDays(1).atTime(7, 0));
        bedtime.checkTransition();
        assertThat(bedtime.consumeGoodMorning()).isTrue();
        bedtime.replayGreetingForDevelopment();
        bedtime.checkTransition();
        assertThat(published).hasSize(3).last().isInstanceOf(BedtimeEnded.class);
        assertThat(bedtime.consumeGoodMorning()).isTrue();
    }
}
