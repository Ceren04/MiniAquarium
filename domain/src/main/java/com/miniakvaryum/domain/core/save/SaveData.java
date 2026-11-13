package com.miniakvaryum.domain.core.save;

import com.miniakvaryum.domain.core.time.TimeAnchor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Everything that is written to the save file.
 *
 * <p>The file has a small fixed frame (schema version and time anchor) and one
 * {@link SaveSection} per feature, stored under the feature id:
 * <pre>
 * {
 *   "schemaVersion": 1,
 *   "time": { ... },
 *   "sections": {
 *     "economy": { "version": 1, "data": { ... } },
 *     "tasks":   { "version": 1, "data": { ... } }
 *   }
 * }
 * </pre>
 * Adding a feature adds a section; this class never changes. See docs/DECISIONS.md (D-004).
 *
 * @param schemaVersion version of the frame (not of the sections)
 * @param time          clocks at the moment of saving
 * @param sections      feature id to section; unmodifiable, keeps insertion order
 */
public record SaveData(int schemaVersion, TimeAnchor time, Map<String, SaveSection> sections) {

    /** The frame version this code writes and can read. */
    public static final int SCHEMA_VERSION = 1;

    public SaveData {
        Objects.requireNonNull(time, "time");
        sections = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(sections, "sections")));
    }

    /** Creates save data with the current {@link #SCHEMA_VERSION}. */
    public static SaveData of(TimeAnchor time, Map<String, SaveSection> sections) {
        return new SaveData(SCHEMA_VERSION, time, sections);
    }
}
