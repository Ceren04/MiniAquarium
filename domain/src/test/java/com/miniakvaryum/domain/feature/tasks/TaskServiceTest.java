package com.miniakvaryum.domain.feature.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.LedgerEntry;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import com.miniakvaryum.domain.feature.security.SecurityFeature;
import com.miniakvaryum.domain.feature.security.api.ParentAccessException;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.ParentSessions;
import com.miniakvaryum.domain.feature.tasks.api.ApprovalResult;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskApproved;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.feature.tasks.api.TaskStatus;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Required tests for the task rules (specification section 9.3).
 *
 * <p>The fixture gives a running game with an already unlocked parent area, so these tests do
 * not depend on the PIN logic. A new game starts with 0 coins and 5 food.
 */
class TaskServiceTest {

    private static final long START_COINS = 0;
    private static final long START_FOOD = 5;

    private final TestGame game = new TestGame();
    private GameSession session;
    private TaskService tasks;
    private Wallet wallet;
    private ParentSession parent;

    @BeforeEach
    void setUp() {
        session = start();
        tasks = session.get(TaskService.class);
        wallet = session.get(Wallet.class);
        parent = session.get(ParentSession.class);
        ParentSessions.forceUnlock(parent);
    }

    private GameSession start() {
        return game.start(new EconomyFeature(EconomyConfig.defaults()),
                new SecurityFeature(SecurityConfig.defaults()),
                new TasksFeature(TasksConfig.defaults()));
    }

    @Test
    void fullHappyPathPaysTheReward() {
        Task task = tasks.create("Tidy your room", 10, 2);
        assertThat(task.status()).isEqualTo(TaskStatus.ACTIVE);

        tasks.reportDone(task.id());
        assertThat(tasks.approve(task.id())).isEqualTo(ApprovalResult.APPROVED);

        assertThat(tasks.list()).containsExactly(new Task(task.id(), "Tidy your room", 10, 2, TaskStatus.APPROVED));
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(START_COINS + 10);
        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(START_FOOD + 2);
    }

    @Test
    void reportDoneDoesNotPayYet() {
        Task task = tasks.create("Tidy your room", 10, 2);

        tasks.reportDone(task.id());

        assertThat(tasks.list(TaskStatus.PENDING)).extracting(Task::id).containsExactly(task.id());
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(START_COINS);
        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(START_FOOD);
    }

    @Test
    void approveTwicePaysOnce() {
        Task task = reportedTask(10, 2);
        tasks.approve(task.id());

        assertThat(tasks.approve(task.id())).isEqualTo(ApprovalResult.ALREADY_APPROVED);

        assertThat(wallet.balance(Currency.COIN)).isEqualTo(START_COINS + 10);
        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(START_FOOD + 2);
    }

    @Test
    void rejectReturnsToActiveAndALaterApprovePaysOnce() {
        Task task = reportedTask(10, 2);

        tasks.reject(task.id());
        assertThat(tasks.list(TaskStatus.ACTIVE)).extracting(Task::id).containsExactly(task.id());
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(START_COINS);

        tasks.reportDone(task.id());
        assertThat(tasks.approve(task.id())).isEqualTo(ApprovalResult.APPROVED);
        assertThat(tasks.approve(task.id())).isEqualTo(ApprovalResult.ALREADY_APPROVED);
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(START_COINS + 10);
        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(START_FOOD + 2);
    }

    @Test
    void parentOnlyMethodsThrowWhenLocked() {
        Task active = tasks.create("Water the plants", 1, 1);
        Task pending = reportedTask(1, 1);
        parent.lock();

        assertThatThrownBy(() -> tasks.create("New", 1, 1)).isInstanceOf(ParentAccessException.class);
        assertThatThrownBy(() -> tasks.update(active.id(), "Changed", 1, 1)).isInstanceOf(ParentAccessException.class);
        assertThatThrownBy(() -> tasks.delete(active.id())).isInstanceOf(ParentAccessException.class);
        assertThatThrownBy(() -> tasks.approve(pending.id())).isInstanceOf(ParentAccessException.class);
        assertThatThrownBy(() -> tasks.reject(pending.id())).isInstanceOf(ParentAccessException.class);

        assertThat(tasks.list()).hasSize(2);
        assertThat(tasks.list(TaskStatus.PENDING)).extracting(Task::id).containsExactly(pending.id());
    }

    @Test
    void reportDoneWorksWhileTheParentAreaIsLocked() {
        Task task = tasks.create("Tidy your room", 10, 2);
        parent.lock();

        tasks.reportDone(task.id());

        assertThat(tasks.list(TaskStatus.PENDING)).extracting(Task::id).containsExactly(task.id());
    }

    @Test
    void titleIsTrimmedAndValidated() {
        assertThat(tasks.create("  Tidy room  ", 1, 1).title()).isEqualTo("Tidy room");

        for (String bad : new String[] {"", "   ", null, "x".repeat(61)}) {
            assertThatThrownBy(() -> tasks.create(bad, 1, 1)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(tasks.create("x".repeat(60), 1, 1).title()).hasSize(60);
        // Trimming happens before the length check.
        assertThat(tasks.create(" " + "y".repeat(60) + " ", 1, 1).title()).hasSize(60);
        assertThat(tasks.list()).hasSize(3);
    }

    @Test
    void rewardsAreValidated() {
        for (int bad : new int[] {-1, 1000}) {
            assertThatThrownBy(() -> tasks.create("Task", bad, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> tasks.create("Task", 0, bad)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(tasks.create("Task", 0, 0).coinReward()).isZero();
        Task max = tasks.create("Task", 999, 999);
        assertThat(max.coinReward()).isEqualTo(999);
        assertThat(max.foodReward()).isEqualTo(999);

        assertThatThrownBy(() -> tasks.update(max.id(), "Task", 1000, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(tasks.list()).hasSize(2);
    }

    @Test
    void updateAndDeleteWorkOnlyWhileActive() {
        Task task = tasks.create("Tidy your room", 10, 2);
        Task updated = tasks.update(task.id(), "  Tidy the kitchen ", 5, 1);
        assertThat(updated).isEqualTo(new Task(task.id(), "Tidy the kitchen", 5, 1, TaskStatus.ACTIVE));
        assertThat(tasks.list()).containsExactly(updated);

        tasks.reportDone(task.id());
        assertThatThrownBy(() -> tasks.update(task.id(), "Other", 1, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tasks.delete(task.id())).isInstanceOf(IllegalStateException.class);

        tasks.approve(task.id());
        assertThatThrownBy(() -> tasks.update(task.id(), "Other", 1, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tasks.delete(task.id())).isInstanceOf(IllegalStateException.class);

        Task other = tasks.create("Feed the cat", 1, 0);
        tasks.delete(other.id());
        assertThat(tasks.list()).extracting(Task::id).containsExactly(task.id());
    }

    @Test
    void unknownIdIsRejected() {
        assertThatThrownBy(() -> tasks.update("nope", "Title", 1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tasks.delete("nope")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tasks.reportDone("nope")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tasks.approve("nope")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tasks.reject("nope")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approvingAnActiveTaskIsIllegal() {
        Task task = tasks.create("Tidy your room", 10, 2);

        assertThatThrownBy(() -> tasks.approve(task.id())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tasks.reject(task.id())).isInstanceOf(IllegalStateException.class);
        tasks.reportDone(task.id());
        assertThatThrownBy(() -> tasks.reportDone(task.id())).isInstanceOf(IllegalStateException.class);

        assertThat(wallet.balance(Currency.COIN)).isEqualTo(START_COINS);
    }

    @Test
    void rewardOfZeroCreatesNoLedgerEntry() {
        int entriesBefore = wallet.entries().size();
        Task task = reportedTask(10, 0);

        tasks.approve(task.id());

        List<LedgerEntry> added = wallet.entries().subList(entriesBefore, wallet.entries().size());
        assertThat(added).extracting(LedgerEntry::refId).containsExactly("task:" + task.id() + ":coin");
        assertThat(added).extracting(LedgerEntry::reason).containsExactly("task");
        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(START_FOOD);
    }

    @Test
    void approvalPublishesTaskApprovedExactlyOnce() {
        List<TaskApproved> received = new ArrayList<>();
        session.events().subscribe(TaskApproved.class, received::add);
        Task task = reportedTask(10, 2);

        tasks.approve(task.id());
        tasks.approve(task.id());

        assertThat(received).hasSize(1);
        assertThat(received.get(0).task().id()).isEqualTo(task.id());
        assertThat(received.get(0).task().status()).isEqualTo(TaskStatus.APPROVED);
    }

    @Test
    void tasksAndRewardsSurviveARestart() {
        Task approved = reportedTask(10, 2);
        tasks.approve(approved.id());
        Task pending = reportedTask(3, 0);
        Task active = tasks.create("Feed the cat", 1, 1);
        List<Task> before = tasks.list();
        session.tick(0.016);    // the changes requested a save; the session writes it now
        session.saveNow();

        GameSession later = start();
        TaskService laterTasks = later.get(TaskService.class);
        Wallet laterWallet = later.get(Wallet.class);
        ParentSession laterParent = later.get(ParentSession.class);

        assertThat(laterTasks.list()).isEqualTo(before);
        assertThat(laterWallet.balance(Currency.COIN)).isEqualTo(START_COINS + 10);
        assertThat(laterWallet.balance(Currency.FOOD)).isEqualTo(START_FOOD + 2);
        assertThat(laterParent.isUnlocked()).isFalse();

        ParentSessions.forceUnlock(laterParent);
        assertThat(laterTasks.approve(approved.id())).isEqualTo(ApprovalResult.ALREADY_APPROVED);
        laterTasks.approve(pending.id());
        assertThat(laterWallet.balance(Currency.COIN)).isEqualTo(START_COINS + 13);
        assertThat(laterTasks.list(TaskStatus.ACTIVE)).extracting(Task::id).containsExactly(active.id());
    }

    /** Creates a task and reports it as done, so it is PENDING. */
    private Task reportedTask(int coins, int food) {
        Task task = tasks.create("Task " + (tasks.list().size() + 1), coins, food);
        tasks.reportDone(task.id());
        return task;
    }
}
