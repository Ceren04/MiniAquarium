package com.miniakvaryum.domain.core.save;

import java.util.Optional;

/**
 * Loads and stores the save data. The domain only knows this interface, so tests can use
 * an in-memory store and other platforms can store the data elsewhere.
 */
public interface SaveStore {

    /**
     * Loads the last saved state.
     *
     * @return the saved data, or empty if there is no usable save (new game)
     * @throws UnsupportedSaveVersionException if the save was written by a newer version of the game
     */
    Optional<SaveData> load();

    /**
     * Stores the given state, replacing the previous one.
     *
     * @throws java.io.UncheckedIOException if the data could not be written
     */
    void save(SaveData data);
}
