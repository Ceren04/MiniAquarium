package com.miniakvaryum.domain.core.save;

import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Upgrades saved JSON from an old format version to the current one, one version at a time.
 *
 * <p>When a format changes, raise the version by one and register a step that converts the
 * previous format into the new one. Old save files then pass through every step in order
 * (1 to 2, 2 to 3, ...) until they reach the current version.
 *
 * <p>The same class is used in two places: by the save store for the file frame, and by each
 * feature for its own section. Example for a feature whose section is now at version 3:
 * <pre>
 * SaveMigrator migrator = new SaveMigrator(3)
 *         .step(1, json -> addDefaultColor(json))    // version 1 to 2
 *         .step(2, json -> renameField(json));       // version 2 to 3
 * JsonElement current = migrator.migrate(section.version(), section.data());
 * </pre>
 */
public final class SaveMigrator {

    private final int currentVersion;
    private final Map<Integer, UnaryOperator<JsonElement>> steps = new HashMap<>();

    /**
     * @param currentVersion the version this code writes (1 or higher)
     */
    public SaveMigrator(int currentVersion) {
        if (currentVersion < 1) {
            throw new IllegalArgumentException("currentVersion must be >= 1 but was " + currentVersion);
        }
        this.currentVersion = currentVersion;
    }

    /**
     * Registers the step that converts {@code fromVersion} into {@code fromVersion + 1}.
     *
     * @return this migrator, so calls can be chained
     */
    public SaveMigrator step(int fromVersion, UnaryOperator<JsonElement> step) {
        if (fromVersion < 1 || fromVersion >= currentVersion) {
            throw new IllegalArgumentException(
                    "fromVersion must be between 1 and " + (currentVersion - 1) + " but was " + fromVersion);
        }
        if (steps.putIfAbsent(fromVersion, Objects.requireNonNull(step, "step")) != null) {
            throw new IllegalArgumentException("a step from version " + fromVersion + " is already registered");
        }
        return this;
    }

    /** Returns the version this migrator upgrades to. */
    public int currentVersion() {
        return currentVersion;
    }

    /**
     * Upgrades {@code json} from {@code version} to the current version.
     *
     * @return the upgraded JSON (the input itself if it is already current)
     * @throws UnsupportedSaveVersionException if {@code version} is newer than the current version
     * @throws IllegalArgumentException        if {@code version} is lower than 1
     * @throws IllegalStateException           if a needed step was never registered (programming error)
     */
    public JsonElement migrate(int version, JsonElement json) {
        if (version > currentVersion) {
            throw new UnsupportedSaveVersionException(version, currentVersion);
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1 but was " + version);
        }
        JsonElement result = json;
        for (int v = version; v < currentVersion; v++) {
            UnaryOperator<JsonElement> step = steps.get(v);
            if (step == null) {
                throw new IllegalStateException("no migration step registered from version " + v);
            }
            result = step.apply(result);
        }
        return result;
    }
}
