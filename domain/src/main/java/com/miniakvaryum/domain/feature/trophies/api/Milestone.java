package com.miniakvaryum.domain.feature.trophies.api;

import java.util.Objects;

/**
 * One trophy on the road.
 *
 * @param threshold number of approved tasks that unlocks it; also its id in the save
 */
public record Milestone(int threshold, Section section, Reward reward) {

    public Milestone {
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be positive: " + threshold);
        }
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(reward, "reward");
    }
}
