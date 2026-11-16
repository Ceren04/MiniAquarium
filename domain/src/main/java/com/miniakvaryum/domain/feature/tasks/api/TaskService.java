package com.miniakvaryum.domain.feature.tasks.api;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.core.util.IdGenerator;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.tasks.TasksConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates tasks, moves them through their life cycle and pays the reward exactly once.
 *
 * <p>Specification section 9.3. Tests: {@code TaskServiceTest}.
 *
 * <p>Rules:
 * <ul>
 *   <li><b>Parent-only</b> methods call {@code parent.requireUnlocked()} as their first line.</li>
 *   <li><b>Validation</b> (create, update): the title is trimmed and must then have 1 to
 *       {@code titleMaxLength} characters; each reward must be 0 to {@code rewardMax}.
 *       Violations throw {@link IllegalArgumentException}.</li>
 *   <li><b>Unknown id</b>: throw {@link IllegalArgumentException}.</li>
 *   <li><b>Wrong state</b> (for example {@code reportDone} on a PENDING task): throw
 *       {@link IllegalStateException}. The only exception is {@code approve} on an APPROVED
 *       task, which returns {@link ApprovalResult#ALREADY_APPROVED}.</li>
 *   <li>Tasks are immutable records: replace the list element with {@code task.withStatus(...)}.</li>
 *   <li>Call {@code onChange.run()} after every successful change, so that it gets saved.</li>
 * </ul>
 */
public final class TaskService {

    private static final String REWARD_REASON = "task";

    private final List<Task> tasks;
    private final Wallet wallet;
    private final ParentSession parent;
    private final GameTime time;
    private final IdGenerator ids;
    private final TasksConfig config;
    private final EventBus events;
    private final Runnable onChange;

    /**
     * @param initial  saved tasks (an empty list for a new game)
     * @param wallet   receives the rewards
     * @param parent   guards the parent-only methods
     * @param time     trusted game time, used as the time of the reward transactions
     * @param ids      creates ids for new tasks
     * @param config   validation limits
     * @param events   where {@link TaskApproved} is published
     * @param onChange called after every successful change
     */
    public TaskService(List<Task> initial, Wallet wallet, ParentSession parent, GameTime time,
            IdGenerator ids, TasksConfig config, EventBus events, Runnable onChange) {
        this.tasks = new ArrayList<>(Objects.requireNonNull(initial, "initial"));
        this.wallet = Objects.requireNonNull(wallet, "wallet");
        this.parent = Objects.requireNonNull(parent, "parent");
        this.time = Objects.requireNonNull(time, "time");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.config = Objects.requireNonNull(config, "config");
        this.events = Objects.requireNonNull(events, "events");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /**
     * Parent only. Creates a new ACTIVE task.
     *
     * <p>Guard, validate, create with {@code ids.next()}, add, {@code onChange}.
     *
     * @return the created task
     * @throws IllegalArgumentException if the title or a reward is invalid
     */
    public Task create(String title, int coins, int food) {
        parent.requireUnlocked();
        String cleanTitle = validTitle(title);
        validateRewards(coins, food);
        Task task = new Task(ids.next(), cleanTitle, coins, food, TaskStatus.ACTIVE);
        tasks.add(task);
        onChange.run();
        return task;
    }

    /**
     * Parent only. Changes title and rewards of an ACTIVE task.
     *
     * <p>Guard, find, check ACTIVE, validate, replace, {@code onChange}.
     *
     * @return the updated task
     * @throws IllegalArgumentException if the id is unknown or the title or a reward is invalid
     * @throws IllegalStateException    if the task is not ACTIVE
     */
    public Task update(String id, String title, int coins, int food) {
        parent.requireUnlocked();
        int index = indexOf(id);
        requireStatus(tasks.get(index), TaskStatus.ACTIVE, "update");
        String cleanTitle = validTitle(title);
        validateRewards(coins, food);
        Task updated = new Task(id, cleanTitle, coins, food, TaskStatus.ACTIVE);
        tasks.set(index, updated);
        onChange.run();
        return updated;
    }

    /**
     * Parent only. Removes an ACTIVE task.
     *
     * <p>Guard, find, check ACTIVE, remove, {@code onChange}.
     *
     * @throws IllegalArgumentException if the id is unknown
     * @throws IllegalStateException    if the task is not ACTIVE
     */
    public void delete(String id) {
        parent.requireUnlocked();
        int index = indexOf(id);
        requireStatus(tasks.get(index), TaskStatus.ACTIVE, "delete");
        tasks.remove(index);
        onChange.run();
    }

    /**
     * Child. Reports a task as done: ACTIVE becomes PENDING. No reward is paid yet.
     *
     * <p>Find, check ACTIVE, replace, {@code onChange}. No parent guard here.
     *
     * @throws IllegalArgumentException if the id is unknown
     * @throws IllegalStateException    if the task is not ACTIVE
     */
    public void reportDone(String id) {
        int index = indexOf(id);
        Task task = tasks.get(index);
        requireStatus(task, TaskStatus.ACTIVE, "report as done");
        tasks.set(index, task.withStatus(TaskStatus.PENDING));
        onChange.run();
    }

    /**
     * Parent only. Confirms a PENDING task and pays its reward.
     *
     * <ol>
     *   <li>Guard, find. APPROVED: return {@link ApprovalResult#ALREADY_APPROVED}. ACTIVE: throw
     *       {@link IllegalStateException}.</li>
     *   <li>Credit the coins with refId {@code "task:<id>:coin"} and the food with refId
     *       {@code "task:<id>:food"}, reason {@code "task"}, time {@code time.nowMillis()}.
     *       Skip a reward of 0. Both {@code OK} and {@code ALREADY_APPLIED} count as success:
     *       the refId is what guarantees that a reward is never paid twice, even if the game
     *       crashed between paying and saving the new status.</li>
     *   <li>Replace with status APPROVED, publish {@link TaskApproved}, {@code onChange}.</li>
     * </ol>
     */
    public ApprovalResult approve(String id) {
        parent.requireUnlocked();
        int index = indexOf(id);
        Task task = tasks.get(index);
        if (task.status() == TaskStatus.APPROVED) {
            return ApprovalResult.ALREADY_APPROVED;
        }
        requireStatus(task, TaskStatus.PENDING, "approve");
        long now = time.nowMillis();
        pay(Currency.COIN, task.coinReward(), "task:" + id + ":coin", now);
        pay(Currency.FOOD, task.foodReward(), "task:" + id + ":food", now);
        Task approved = task.withStatus(TaskStatus.APPROVED);
        tasks.set(index, approved);
        events.publish(new TaskApproved(approved));
        onChange.run();
        return ApprovalResult.APPROVED;
    }

    /**
     * Parent only. Sends a PENDING task back to ACTIVE; no reward.
     *
     * <p>Guard, find, check PENDING, replace, {@code onChange}.
     *
     * @throws IllegalArgumentException if the id is unknown
     * @throws IllegalStateException    if the task is not PENDING
     */
    public void reject(String id) {
        parent.requireUnlocked();
        int index = indexOf(id);
        Task task = tasks.get(index);
        requireStatus(task, TaskStatus.PENDING, "reject");
        tasks.set(index, task.withStatus(TaskStatus.ACTIVE));
        onChange.run();
    }

    /** Returns all tasks in creation order. The list is a copy. */
    public List<Task> list() {
        return List.copyOf(tasks);
    }

    /** Returns the tasks with the given status, in creation order. */
    public List<Task> list(TaskStatus status) {
        Objects.requireNonNull(status, "status");
        return tasks.stream().filter(task -> task.status() == status).toList();
    }

    // ------------------------------------------------------------ helpers

    /** Credits one reward; 0 is skipped. A refId that was already used counts as paid. */
    private void pay(Currency currency, int amount, String refId, long now) {
        if (amount == 0) {
            return;
        }
        TxResult result = wallet.credit(currency, amount, REWARD_REASON, refId, now);
        if (result != TxResult.OK && result != TxResult.ALREADY_APPLIED) {
            throw new IllegalStateException("Reward " + refId + " could not be paid: " + result);
        }
    }

    private int indexOf(String id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(id)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown task id: " + id);
    }

    private static void requireStatus(Task task, TaskStatus expected, String action) {
        if (task.status() != expected) {
            throw new IllegalStateException("Cannot " + action + " task " + task.id() + " in status "
                    + task.status() + "; it must be " + expected);
        }
    }

    private String validTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty() || trimmed.length() > config.titleMaxLength()) {
            throw new IllegalArgumentException("The title must have 1 to " + config.titleMaxLength()
                    + " characters");
        }
        return trimmed;
    }

    private void validateRewards(int coins, int food) {
        if (coins < 0 || coins > config.rewardMax() || food < 0 || food > config.rewardMax()) {
            throw new IllegalArgumentException("Rewards must be between 0 and " + config.rewardMax());
        }
    }
}
