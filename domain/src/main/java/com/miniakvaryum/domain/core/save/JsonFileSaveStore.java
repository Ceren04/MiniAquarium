package com.miniakvaryum.domain.core.save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miniakvaryum.domain.core.time.TimeAnchor;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link SaveStore} that keeps the save as a JSON file in one directory.
 *
 * <p>Files in the directory:
 * <ul>
 *   <li>{@code save.json}: the current save</li>
 *   <li>{@code save.json.bak}: the save before the last write, used if the current one is damaged</li>
 *   <li>{@code save.json.tmp}: exists only while a write is in progress</li>
 *   <li>{@code save.json.corrupt-<millis>}: a damaged save, kept for inspection, never deleted</li>
 * </ul>
 *
 * <p><b>Why writing is done in three steps.</b> If the game crashed while writing directly into
 * {@code save.json}, the file would be half old and half new: unusable. Instead the new content
 * goes into a temporary file first. Only when it is completely on disk is it renamed to
 * {@code save.json}, and a rename either happens fully or not at all. Whatever the moment of a
 * crash, {@code save.json} is always a complete file.
 */
public final class JsonFileSaveStore implements SaveStore {

    /** System property that overrides the save directory (used by tests and for debugging). */
    public static final String HOME_PROPERTY = "akvaryum.home";

    private static final Logger LOG = Logger.getLogger(JsonFileSaveStore.class.getName());
    private static final String FILE_NAME = "save.json";

    private final Path file;
    private final Path backup;
    private final Path temp;
    private final SaveMigrator frameMigrator;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    /** Set when a save from a newer game version was found; blocks {@link #save}. */
    private boolean newerSaveFound;

    /** Creates a store for the current frame version, which needs no migration steps yet. */
    public JsonFileSaveStore(Path directory) {
        this(directory, new SaveMigrator(SaveData.SCHEMA_VERSION));
    }

    /**
     * @param directory     folder that holds the save files; created on the first save
     * @param frameMigrator upgrades the file frame from older schema versions
     */
    public JsonFileSaveStore(Path directory, SaveMigrator frameMigrator) {
        Objects.requireNonNull(directory, "directory");
        this.file = directory.resolve(FILE_NAME);
        this.backup = directory.resolve(FILE_NAME + ".bak");
        this.temp = directory.resolve(FILE_NAME + ".tmp");
        this.frameMigrator = Objects.requireNonNull(frameMigrator, "frameMigrator");
    }

    /**
     * Returns the standard save directory: the value of the system property
     * {@value #HOME_PROPERTY} if set, otherwise {@code .mini-akvaryum} in the user's home folder.
     */
    public static Path defaultDirectory() {
        String override = System.getProperty(HOME_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home"), ".mini-akvaryum");
    }

    // ---------------------------------------------------------------- load

    @Override
    public Optional<SaveData> load() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(read(file));
        } catch (CorruptSaveException e) {
            LOG.log(Level.WARNING, "Save file is damaged, trying the backup: " + file, e);
            keepCorruptFile();
        }
        if (!Files.exists(backup)) {
            LOG.warning("No backup available, starting a new game");
            return Optional.empty();
        }
        try {
            return Optional.of(read(backup));
        } catch (CorruptSaveException e) {
            LOG.log(Level.WARNING, "Backup is damaged too, starting a new game: " + backup, e);
            return Optional.empty();
        }
    }

    /**
     * Reads and decodes one file.
     *
     * @throws CorruptSaveException            if the file cannot be read or is not a valid save
     * @throws UnsupportedSaveVersionException if the file was written by a newer game version
     */
    private SaveData read(Path path) {
        JsonObject root;
        int version;
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            root = JsonParser.parseString(text).getAsJsonObject();
            version = root.get("schemaVersion").getAsInt();
        } catch (IOException | RuntimeException e) {
            throw new CorruptSaveException(e);
        }
        if (version < 1) {
            throw new CorruptSaveException(new IllegalArgumentException("schemaVersion " + version));
        }
        if (version > frameMigrator.currentVersion()) {
            newerSaveFound = true;
            throw new UnsupportedSaveVersionException(version, frameMigrator.currentVersion());
        }
        // Deliberately outside the try blocks: a missing migration step is a bug in our code,
        // not a damaged file, and must not be hidden as "corrupt".
        JsonElement migrated = frameMigrator.migrate(version, root);
        try {
            return decode(migrated.getAsJsonObject());
        } catch (RuntimeException e) {
            throw new CorruptSaveException(e);
        }
    }

    private SaveData decode(JsonObject root) {
        TimeAnchor time = gson.fromJson(root.getAsJsonObject("time"), TimeAnchor.class);
        if (time == null || time.bootId() == null) {
            throw new IllegalArgumentException("time anchor is missing or incomplete");
        }
        Map<String, SaveSection> sections = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("sections").entrySet()) {
            JsonObject section = entry.getValue().getAsJsonObject();
            sections.put(entry.getKey(),
                    new SaveSection(section.get("version").getAsInt(), section.get("data")));
        }
        return new SaveData(frameMigrator.currentVersion(), time, sections);
    }

    /** Moves the damaged save aside so that it is neither lost nor read again. */
    private void keepCorruptFile() {
        try {
            long millis = Files.getLastModifiedTime(file).toMillis();
            Path target = file.resolveSibling(FILE_NAME + ".corrupt-" + millis);
            for (int n = 1; Files.exists(target); n++) {
                target = file.resolveSibling(FILE_NAME + ".corrupt-" + millis + "-" + n);
            }
            Files.move(file, target);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not move the damaged save file aside: " + file, e);
        }
    }

    // ---------------------------------------------------------------- save

    @Override
    public void save(SaveData data) {
        Objects.requireNonNull(data, "data");
        if (newerSaveFound) {
            throw new IllegalStateException("Refusing to overwrite a save from a newer game version");
        }
        if (data.schemaVersion() != frameMigrator.currentVersion()) {
            throw new IllegalArgumentException("Can only write schema version "
                    + frameMigrator.currentVersion() + " but got " + data.schemaVersion());
        }
        byte[] bytes = gson.toJson(encode(data)).getBytes(StandardCharsets.UTF_8);
        try {
            Files.createDirectories(file.getParent());
            // 1. Write the new content into the temporary file and force it onto the disk.
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            // 2. Keep the previous save as the backup.
            if (Files.exists(file)) {
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            // 3. Swap the new file in with a single rename.
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                LOG.log(Level.WARNING, "Atomic move is not supported here, using a normal move", e);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the save file " + file, e);
        }
    }

    private JsonObject encode(SaveData data) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", data.schemaVersion());
        root.add("time", gson.toJsonTree(data.time()));
        JsonObject sections = new JsonObject();
        data.sections().forEach((id, section) -> {
            JsonObject json = new JsonObject();
            json.addProperty("version", section.version());
            json.add("data", section.data());
            sections.add(id, json);
        });
        root.add("sections", sections);
        return root;
    }

    /** Internal signal: this file cannot be used as a save. */
    private static final class CorruptSaveException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CorruptSaveException(Throwable cause) {
            super(cause);
        }
    }
}
