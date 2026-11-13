package com.miniakvaryum.domain.feature.tasks;

import static org.assertj.core.api.Assertions.fail;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import com.miniakvaryum.domain.feature.security.SecurityFeature;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.ParentSessions;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.testsupport.TestGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Required tests for the task rules (specification section 7.6).
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test. The fixture below gives you a running game with an
 * already unlocked parent area, so these tests do not depend on the PIN logic. Call
 * {@code parent.lock()} in the tests that check the guard.
 */
@Disabled("TODO: enable when TaskService is implemented")
class TaskServiceTest {

    private final TestGame game = new TestGame();
    private GameSession session;
    private TaskService tasks;
    private Wallet wallet;
    private ParentSession parent;

    @BeforeEach
    void setUp() {
        session = game.start(new EconomyFeature(EconomyConfig.defaults()),
                new SecurityFeature(SecurityConfig.defaults()),
                new TasksFeature(TasksConfig.defaults()));
        tasks = session.get(TaskService.class);
        wallet = session.get(Wallet.class);
        parent = session.get(ParentSession.class);
        ParentSessions.forceUnlock(parent);
    }

    @Test
    void fullHappyPathPaysTheReward() {
        // create (10 coins, 2 food) -> reportDone -> approve: status APPROVED, coins +10, food +2.
        fail("TODO: write this test");
    }

    @Test
    void reportDoneDoesNotPayYet() {
        fail("TODO: write this test");
    }

    @Test
    void approveTwicePaysOnce() {
        // Second approve returns ALREADY_APPROVED and the balances do not change.
        fail("TODO: write this test");
    }

    @Test
    void rejectReturnsToActiveAndALaterApprovePaysOnce() {
        fail("TODO: write this test");
    }

    @Test
    void parentOnlyMethodsThrowWhenLocked() {
        // create, update, delete, approve, reject: ParentAccessException after parent.lock().
        fail("TODO: write this test");
    }

    @Test
    void reportDoneWorksWhileTheParentAreaIsLocked() {
        fail("TODO: write this test");
    }

    @Test
    void titleIsTrimmedAndValidated() {
        // "  Tidy room  " is stored as "Tidy room"; "", "   " and 61 characters are rejected; 60 is accepted.
        fail("TODO: write this test");
    }

    @Test
    void rewardsAreValidated() {
        // -1 and 1000 are rejected for coins and for food; 0 and 999 are accepted.
        fail("TODO: write this test");
    }

    @Test
    void updateAndDeleteWorkOnlyWhileActive() {
        fail("TODO: write this test");
    }

    @Test
    void unknownIdIsRejected() {
        fail("TODO: write this test");
    }

    @Test
    void approvingAnActiveTaskIsIllegal() {
        fail("TODO: write this test");
    }

    @Test
    void rewardOfZeroCreatesNoLedgerEntry() {
        fail("TODO: write this test");
    }

    @Test
    void approvalPublishesTaskApprovedExactlyOnce() {
        // session.events().subscribe(TaskApproved.class, ...): one event, also after a second approve.
        fail("TODO: write this test");
    }

    @Test
    void tasksAndRewardsSurviveARestart() {
        // saveNow, game.start(...) again: same tasks, same balances, approve still pays only once.
        fail("TODO: write this test");
    }
}
