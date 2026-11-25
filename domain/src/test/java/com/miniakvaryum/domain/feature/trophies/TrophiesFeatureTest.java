package com.miniakvaryum.domain.feature.trophies;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.DomainFeatures;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.feature.trophies.api.ClaimResult;
import com.miniakvaryum.domain.feature.trophies.api.TrophyService;
import com.miniakvaryum.domain.feature.trophies.api.TrophyState;
import com.miniakvaryum.domain.feature.trophies.api.TrophyUnlocked;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The trophy road in the full game (plan tests 1, 7, 8 and the TC-8 steps). */
class TrophiesFeatureTest {

    private final TestGame game = new TestGame();

    private static TaskService parentTasks(GameSession session) {
        PinService pins = session.get(PinService.class);
        if (!pins.isPinSet()) {
            pins.setPin("1234");
        }
        session.get(ParentSession.class).unlock("1234");
        return session.get(TaskService.class);
    }

    private static Task doneTask(TaskService tasks, String title) {
        Task task = tasks.create(title, 1, 0);
        tasks.reportDone(task.id());
        return task;
    }

    @Test
    void onlyAnApprovalCountsNotDoneOrRejected() {
        GameSession session = game.startFullGame();
        TaskService tasks = parentTasks(session);
        TrophyService trophies = session.get(TrophyService.class);
        List<TrophyUnlocked> unlocked = new ArrayList<>();
        session.events().subscribe(TrophyUnlocked.class, unlocked::add);

        Task rejected = doneTask(tasks, "Rejected");
        tasks.reject(rejected.id());
        Task waiting = doneTask(tasks, "Waiting");
        assertThat(trophies.approvedCount()).isZero();

        tasks.approve(waiting.id());
        tasks.approve(waiting.id());
        assertThat(trophies.approvedCount()).isEqualTo(1);
        assertThat(unlocked).hasSize(1);
        assertThat(trophies.stateOf(1)).isEqualTo(TrophyState.READY);
    }

    @Test
    void countAndCollectedTrophiesSurviveARestart() {
        GameSession session = game.startFullGame();
        TaskService tasks = parentTasks(session);
        for (int i = 0; i < 3; i++) {
            tasks.approve(doneTask(tasks, "Task " + i).id());
        }
        TrophyService trophies = session.get(TrophyService.class);
        assertThat(trophies.readyCount()).isEqualTo(2);
        assertThat(trophies.claim(1)).isEqualTo(ClaimResult.OK);
        assertThat(trophies.claim(1)).isEqualTo(ClaimResult.ALREADY_CLAIMED);
        assertThat(trophies.claim(3)).isEqualTo(ClaimResult.OK);
        long food = session.get(Wallet.class).balance(Currency.FOOD);
        long coins = session.get(Wallet.class).balance(Currency.COIN);
        session.saveNow();

        GameSession restarted = game.startFullGame();
        TrophyService again = restarted.get(TrophyService.class);
        assertThat(again.approvedCount()).isEqualTo(3);
        assertThat(again.stateOf(1)).isEqualTo(TrophyState.CLAIMED);
        assertThat(again.stateOf(3)).isEqualTo(TrophyState.CLAIMED);
        assertThat(again.readyCount()).isZero();
        assertThat(restarted.get(Wallet.class).balance(Currency.FOOD)).isEqualTo(food);
        assertThat(restarted.get(Wallet.class).balance(Currency.COIN)).isEqualTo(coins);
    }

    @Test
    void anOldSaveWithoutTrophiesCountsTheTasksApprovedBefore() {
        // A game played before the trophy road existed: two approved tasks, no trophies section.
        GameSession old = game.start(withoutTrophies());
        TaskService tasks = parentTasks(old);
        tasks.approve(doneTask(tasks, "One").id());
        tasks.approve(doneTask(tasks, "Two").id());
        doneTask(tasks, "Not approved yet");
        old.saveNow();

        TrophyService trophies = game.startFullGame().get(TrophyService.class);
        assertThat(trophies.approvedCount()).isEqualTo(2);
        assertThat(trophies.stateOf(1)).isEqualTo(TrophyState.READY);
        assertThat(trophies.stateOf(3)).isEqualTo(TrophyState.LOCKED);
    }

    /** All features of the full game except the trophies. */
    private DomainFeature[] withoutTrophies() {
        int fishCount = AquariumConfig.defaults().fish().initialFishCount();
        for (int i = 0; i < fishCount * 2; i++) {
            game.random.doubles(0.5);
        }
        return DomainFeatures.all(game.localClock).stream()
                .filter(f -> !f.id().equals(TrophiesFeature.ID))
                .toArray(DomainFeature[]::new);
    }
}
