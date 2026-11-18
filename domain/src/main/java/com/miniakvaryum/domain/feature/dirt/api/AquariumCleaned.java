package com.miniakvaryum.domain.feature.dirt.api;

/**
 * Event: the child wiped away the last dirt spot; the glass is clean.
 * Published once each time the dirt level reaches 0 by wiping. Later features (trophy road,
 * mood, statistics) can subscribe to it.
 */
public record AquariumCleaned() {
}
