package com.miniakvaryum.domain.core.save;

import com.google.gson.JsonElement;
import java.util.Objects;

/**
 * The part of the save file that belongs to one feature.
 *
 * <p>The save system does not know what is inside {@code data}; only the owning feature does.
 * That is what lets a feature change its saved data without touching any shared class.
 *
 * @param version format version of {@code data}, chosen and migrated by the owning feature (1 or higher)
 * @param data    the feature's saved state as JSON; treat it as read-only
 */
public record SaveSection(int version, JsonElement data) {

    public SaveSection {
        if (version < 1) {
            throw new IllegalArgumentException("section version must be >= 1 but was " + version);
        }
        // Copy, so later changes to the caller's JSON object cannot alter what will be saved.
        data = Objects.requireNonNull(data, "data").deepCopy();
    }
}
