package com.miniakvaryum.domain.testsupport;

import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveStore;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

/**
 * A {@link SaveStore} that keeps the save in memory. Use the same instance for two sessions
 * to test "save, restart, load" without touching the disk.
 */
public final class InMemorySaveStore implements SaveStore {

    private SaveData data;
    private int saveCount;
    private boolean failing;

    /** Starts with the given content, as if a save file already existed. */
    public static InMemorySaveStore containing(SaveData data) {
        InMemorySaveStore store = new InMemorySaveStore();
        store.data = data;
        return store;
    }

    @Override
    public Optional<SaveData> load() {
        return Optional.ofNullable(data);
    }

    @Override
    public void save(SaveData newData) {
        if (failing) {
            throw new UncheckedIOException(new IOException("simulated disk failure"));
        }
        data = newData;
        saveCount++;
    }

    /** While true, every save fails like a full or broken disk. */
    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    /** How many saves have succeeded. */
    public int saveCount() {
        return saveCount;
    }

    /** The last saved content, or null if nothing was saved. */
    public SaveData lastSaved() {
        return data;
    }
}
