package com.miniakvaryum.domain.feature.bedtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.time.LocalClock;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeService;
import com.miniakvaryum.domain.feature.bedtime.api.BedtimeStarted;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Wiring of the bedtime feature: save of the greetings, events through the session. */
class BedtimeFeatureTest {

    private final TestGame game = new TestGame();

    private GameSession start() {
        return game.start(new BedtimeFeature(BedtimeConfig.defaults(), game.localClock));
    }

    @Test
    void greetingsAreKeptInTheSave() {
        game.localClock.set(LocalDateTime.of(2026, 11, 24, 22, 0));
        GameSession session = start();
        assertThat(session.get(BedtimeService.class).consumeGoodnight()).isTrue();
        session.saveNow();

        assertThat(start().get(BedtimeService.class).consumeGoodnight()).isFalse();

        game.localClock.set(LocalDateTime.of(2026, 11, 25, 8, 0));
        GameSession morning = start();
        assertThat(morning.get(BedtimeService.class).consumeGoodMorning()).isTrue();
        morning.saveNow();
        assertThat(start().get(BedtimeService.class).consumeGoodMorning()).isFalse();
    }

    @Test
    void tickPublishesTheStartOfBedtime() {
        game.localClock.set(LocalDateTime.of(2026, 11, 24, 20, 59));
        GameSession session = start();
        List<BedtimeStarted> started = new ArrayList<>();
        session.events().subscribe(BedtimeStarted.class, started::add);

        session.tick(0.1);
        game.localClock.set(LocalDateTime.of(2026, 11, 24, 21, 0));
        session.tick(0.1);

        assertThat(started).hasSize(1);
        assertThat(session.get(BedtimeService.class).isBedtime()).isTrue();
    }

    @Test
    void theClockIsOfferedForTheDeveloperTools() {
        assertThat(start().get(LocalClock.class)).isSameAs(game.localClock);
    }

    @Test
    void theFullGameHasBedtime() {
        assertThat(game.startFullGame().get(BedtimeService.class).isBedtime()).isFalse();
    }
}
