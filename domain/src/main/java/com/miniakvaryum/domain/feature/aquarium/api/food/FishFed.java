package com.miniakvaryum.domain.feature.aquarium.api.food;

/**
 * Event: a fish finished eating a pellet. Published once per eaten pellet.
 * Later features (mood, statistics, level-up message) subscribe to this.
 *
 * @param fishId    the fish that ate
 * @param feedCount its feed count after this feeding
 */
public record FishFed(String fishId, int feedCount) {
}
