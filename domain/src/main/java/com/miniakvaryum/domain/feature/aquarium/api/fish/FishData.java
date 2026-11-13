package com.miniakvaryum.domain.feature.aquarium.api.fish;

/**
 * The part of a fish that is saved. Position and current action are runtime state and are
 * never saved; level is derived from these values and is not saved either.
 *
 * @param id             unique id
 * @param name           name given by the child, or null if the fish has no name
 * @param feedCount      how many pellets this fish has eaten
 * @param aquariumMillis how long this fish has lived in the aquarium (game time, offline time included)
 */
public record FishData(String id, String name, int feedCount, long aquariumMillis) {
}
