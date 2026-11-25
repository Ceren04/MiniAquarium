package com.miniakvaryum.domain.feature.trophies.api;

/** The child collected a trophy; its reward was given. For stats, profile and later the inventory. */
public record TrophyClaimed(Milestone milestone) {
}
