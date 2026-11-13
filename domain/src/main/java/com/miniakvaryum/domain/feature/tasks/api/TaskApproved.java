package com.miniakvaryum.domain.feature.tasks.api;

/**
 * Event: a parent approved a task and its reward was paid. Published once per task.
 * Later features (trophy road, statistics) subscribe to this.
 *
 * @param task the task in its APPROVED state
 */
public record TaskApproved(Task task) {
}
