package com.miniakvaryum.domain.core.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.miniakvaryum.domain.core.time.TimeAnchor;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SaveDataTest {

    private static final TimeAnchor TIME = new TimeAnchor(1, 2, 3, "boot");

    @Test
    void sectionsCannotBeChangedFromOutside() {
        Map<String, SaveSection> source = new HashMap<>();
        source.put("a", new SaveSection(1, new JsonPrimitive(1)));
        SaveData data = SaveData.of(TIME, source);

        source.put("b", new SaveSection(1, new JsonPrimitive(2)));

        assertThat(data.sections()).containsOnlyKeys("a");
        assertThatThrownBy(() -> data.sections().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void sectionKeepsItsOwnCopyOfTheJson() {
        JsonObject json = new JsonObject();
        json.addProperty("coins", 5);
        SaveSection section = new SaveSection(1, json);

        json.addProperty("coins", 999);

        assertThat(section.data().getAsJsonObject().get("coins").getAsInt()).isEqualTo(5);
    }

    @Test
    void sectionVersionMustBePositive() {
        assertThatThrownBy(() -> new SaveSection(0, new JsonObject())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void factoryUsesCurrentSchemaVersion() {
        assertThat(SaveData.of(TIME, Map.of()).schemaVersion()).isEqualTo(SaveData.SCHEMA_VERSION);
    }
}
