package com.miniakvaryum.game.feature.devtools;

import com.miniakvaryum.domain.core.save.JsonFileSaveStore;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.time.TimeAnchor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Developer time travel: makes the game behave as if the app had been closed for some hours,
 * and goes back to the moment before the first jump.
 *
 * <p><b>How a jump works.</b> The game is saved, the time anchor in the save file is moved into
 * the past (and its boot id cleared, so the wall clock is used), and the game is rebuilt from
 * the file. The normal start-up then credits the hours as offline time, so every feature reacts
 * exactly as it would after a real break: dirt appears, fish age, and so on. The 24 hour cap
 * applies to each jump, just as in the real game.
 *
 * <p><b>Undo.</b> Before the first jump the current save is copied into a checkpoint folder
 * ({@code dev-checkpoint} next to the save). Further jumps keep that checkpoint. Undo copies it
 * back, deletes it and rebuilds the game. The checkpoint survives closing the game.
 */
final class TimeTravel {

    private static final String CHECKPOINT_FOLDER = "dev-checkpoint";

    private final GameSession session;
    private final Runnable restartFromSave;
    private final JsonFileSaveStore store;
    private final Path checkpointFolder;
    private final JsonFileSaveStore checkpointStore;
    /** Game time of the checkpoint, read once; empty if there is no checkpoint. */
    private final OptionalLong checkpointGameMillis;

    /**
     * @param saveFolder the save folder of the signed-in account
     */
    TimeTravel(GameSession session, Path saveFolder, Runnable restartFromSave) {
        this.session = Objects.requireNonNull(session, "session");
        this.restartFromSave = Objects.requireNonNull(restartFromSave, "restartFromSave");
        Objects.requireNonNull(saveFolder, "saveFolder");
        this.store = new JsonFileSaveStore(saveFolder);
        this.checkpointFolder = saveFolder.resolve(CHECKPOINT_FOLDER);
        this.checkpointStore = new JsonFileSaveStore(checkpointFolder);
        this.checkpointGameMillis = checkpointStore.load()
                .map(data -> OptionalLong.of(data.time().gameMillis()))
                .orElse(OptionalLong.empty());
    }

    /** Returns true if there is a moment to go back to. */
    boolean hasCheckpoint() {
        return checkpointGameMillis.isPresent();
    }

    /**
     * Returns how far the game is ahead of the checkpoint in game milliseconds (jumps plus the
     * time played since), or empty if there is no checkpoint.
     */
    OptionalLong millisSinceCheckpoint() {
        if (checkpointGameMillis.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(session.time().nowMillis() - checkpointGameMillis.getAsLong());
    }

    /**
     * Jumps forward: the game restarts as if it had been closed for {@code millis}.
     *
     * @throws UncheckedIOException if the save could not be written
     */
    void jump(long millis) {
        if (millis <= 0) {
            throw new IllegalArgumentException("millis must be > 0 but was " + millis);
        }
        session.saveNow();
        SaveData now = store.load().orElseThrow(() -> new IllegalStateException("The game was just saved"));
        if (!hasCheckpoint()) {
            checkpointStore.save(now);
        }
        TimeAnchor t = now.time();
        TimeAnchor earlier = new TimeAnchor(t.gameMillis(), t.monotonicNanos(), t.wallMillis() - millis, "");
        store.save(SaveData.of(earlier, now.sections()));
        restartFromSave.run();
    }

    /**
     * Goes back to the checkpoint and removes it.
     *
     * @return false if there was no checkpoint (nothing happened)
     * @throws UncheckedIOException if the save could not be written
     */
    boolean undo() {
        Optional<SaveData> checkpoint = checkpointStore.load();
        if (checkpoint.isEmpty()) {
            return false;
        }
        store.save(checkpoint.get());
        try {
            for (String name : new String[] {"save.json", "save.json.bak", "save.json.tmp"}) {
                Files.deleteIfExists(checkpointFolder.resolve(name));
            }
            Files.deleteIfExists(checkpointFolder);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete the time travel checkpoint", e);
        }
        restartFromSave.run();
        return true;
    }
}
