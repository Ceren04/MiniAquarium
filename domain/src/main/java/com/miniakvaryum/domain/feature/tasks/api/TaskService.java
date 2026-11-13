package com.miniakvaryum.domain.feature.tasks.api;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.core.util.IdGenerator;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.tasks.TasksConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates tasks, moves them through their life cycle and pays the reward exactly once.
 *
 * <p><b>SKELETON:</b> the constructor and the two {@code list} methods work; the methods marked
 * TODO must be implemented (specification section 7.6). Tests: {@code TaskServiceTest}.
 *
 * <p>Rules for the implementation:
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
     * <p>TODO: guard, validate, create with {@code ids.next()}, add, {@code onChange}.
     *
     * @return the created task
     */
    public Task create(String title, int coins, int food) {
        throw new UnsupportedOperationException("TODO: TaskService.create is not implemented yet");
    }

    /**
     * Parent only. Changes title and rewards of an ACTIVE task.
     *
     * <p>TODO: guard, find, check ACTIVE, validate, replace, {@code onChange}.
     *
     * @return the updated task
     */
    public Task update(String id, String title, int coins, int food) {
        throw new UnsupportedOperationException("TODO: TaskService.update is not implemented yet");
    }

    /**
     * Parent only. Removes an ACTIVE task.
     *
     * <p>TODO: guard, find, check ACTIVE, remove, {@code onChange}.
     */
    public void delete(String id) {
        throw new UnsupportedOperationException("TODO: TaskService.delete is not implemented yet");
    }

    /**
     * Child. Reports a task as done: ACTIVE becomes PENDING. No reward is paid yet.
     *
     * <p>TODO: find, check ACTIVE, replace, {@code onChange}. No parent guard here.
     */
    public void reportDone(String id) {
        throw new UnsupportedOperationException("TODO: TaskService.reportDone is not implemented yet");
    }

    /**
     * Parent only. Confirms a PENDING task and pays its reward.
     *
     * <p>TODO:
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
        throw new UnsupportedOperationException("TODO: TaskService.approve is not implemented yet");
    }

    /**
     * Parent only. Sends a PENDING task back to ACTIVE; no reward.
     *
     * <p>TODO: guard, find, check PENDING, replace, {@code onChange}.
     */
    public void reject(String id) {
        throw new UnsupportedOperationException("TODO: TaskService.reject is not implemented yet");
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
}
