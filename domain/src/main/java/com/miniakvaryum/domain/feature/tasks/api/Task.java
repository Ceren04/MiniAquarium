package com.miniakvaryum.domain.feature.tasks.api;

/**
 * A real-life task a parent created for the child. Immutable: a change produces a new Task
 * with the same id.
 *
 * @param id         unique id
 * @param title      what to do, for example "Tidy your room"
 * @param coinReward coins paid on approval (0 or more)
 * @param foodReward food paid on approval (0 or more)
 * @param status     where the task is in its life cycle
 */
public record Task(String id, String title, int coinReward, int foodReward, TaskStatus status) {

    /** Returns a copy of this task with another status. */
    public Task withStatus(TaskStatus newStatus) {
        return new Task(id, title, coinReward, foodReward, newStatus);
    }
}
