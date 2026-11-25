package com.miniakvaryum.domain.feature.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.dirt.api.AquariumCleaned;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.stats.api.Stats;
import com.miniakvaryum.domain.feature.stats.api.StatsService;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Stats in the full game: read from the existing save, counted cleanings and days. */
class StatsServiceTest {

    private final TestGame game = new TestGame();

    @Test
    void aNewGameStartsWithItsFishAndNothingElse() {
        Stats stats = game.startFullGame().get(StatsService.class).snapshot();
        assertThat(stats.tasksDone()).isZero();
        assertThat(stats.foodGiven()).isZero();
        assertThat(stats.coinsEarned()).isZero();
        assertThat(stats.cleanings()).isZero();
        assertThat(stats.fishCount()).isEqualTo(3);
        assertThat(stats.bestLevel()).isEqualTo(1);
        assertThat(stats.daysPlayed()).isZero();
    }

    @Test
    void approvedTasksAndEarnedCoinsAreRead() {
        GameSession session = game.startFullGame();
        session.get(PinService.class).setPin("1234");
        session.get(ParentSession.class).unlock("1234");
        TaskService tasks = session.get(TaskService.class);
        Task task = tasks.create("Tidy room", 10, 2);
        tasks.reportDone(task.id());
        tasks.approve(task.id());
        Wallet wallet = session.get(Wallet.class);
        wallet.debit(Currency.COIN, 4, "shop:1", "test", 0L);

        Stats stats = session.get(StatsService.class).snapshot();
        assertThat(stats.tasksDone()).isEqualTo(1);
        // Spending does not lower "coins earned".
        assertThat(stats.coinsEarned()).isEqualTo(10);
    }

    @Test
    void cleaningsAndDaysAreCountedAndSaved() {
        GameSession session = game.startFullGame();
        StatsService stats = session.get(StatsService.class);
        session.events().publish(new AquariumCleaned());
        session.events().publish(new AquariumCleaned());
        LocalDate day = LocalDate.of(2026, 11, 24);
        stats.dayPlayed(day);
        stats.dayPlayed(day);
        stats.dayPlayed(day.plusDays(1));
        session.saveNow();

        Stats after = game.startFullGame().get(StatsService.class).snapshot();
        assertThat(after.cleanings()).isEqualTo(2);
        assertThat(after.daysPlayed()).isEqualTo(2);
    }

    @Test
    void theSameDayAfterARestartCountsOnce() {
        GameSession session = game.startFullGame();
        session.get(StatsService.class).dayPlayed(LocalDate.of(2026, 11, 24));
        session.saveNow();
        StatsService restarted = game.startFullGame().get(StatsService.class);
        restarted.dayPlayed(LocalDate.of(2026, 11, 24));
        assertThat(restarted.snapshot().daysPlayed()).isEqualTo(1);
    }

    @Test
    void anEarlierDayCountsNothingSoTurningTheClockDoesNotHelp() {
        StatsService stats = game.startFullGame().get(StatsService.class);
        LocalDate day = LocalDate.of(2026, 11, 24);
        stats.dayPlayed(day);
        stats.dayPlayed(day.minusDays(1));        // clock set back
        stats.dayPlayed(day);
        stats.dayPlayed(day.minusDays(1));        // back and forth
        stats.dayPlayed(day);
        assertThat(stats.snapshot().daysPlayed()).isEqualTo(1);

        stats.dayPlayed(day.plusDays(10));       // clock set forward: counts once
        stats.dayPlayed(day.plusDays(1));        // real date again: waits until after day + 10
        stats.dayPlayed(day.plusDays(5));
        assertThat(stats.snapshot().daysPlayed()).isEqualTo(2);
        stats.dayPlayed(day.plusDays(11));
        assertThat(stats.snapshot().daysPlayed()).isEqualTo(3);
    }
}
