package com.miniakvaryum.domain.feature.tasks;

/**
 * Tunable numbers of the tasks feature.
 *
 * @param titleMaxLength longest allowed task title (after trimming)
 * @param rewardMax      highest allowed coin reward and food reward of one task
 */
public record TasksConfig(int titleMaxLength, int rewardMax) {

    public TasksConfig {
        if (titleMaxLength < 1 || rewardMax < 0) {
            throw new IllegalArgumentException("titleMaxLength must be >= 1 and rewardMax >= 0");
        }
    }

    /** The values from the specification: 60 characters, rewards up to 999. */
    public static TasksConfig defaults() {
        return new TasksConfig(60, 999);
    }
}
