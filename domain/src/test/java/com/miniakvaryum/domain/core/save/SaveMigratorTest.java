package com.miniakvaryum.domain.core.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class SaveMigratorTest {

    /** A step that records its name, so the test can see which steps ran and in which order. */
    private static UnaryOperator<JsonElement> mark(String name) {
        return json -> {
            JsonArray copy = json.getAsJsonArray().deepCopy();
            copy.add(name);
            return copy;
        };
    }

    private static SaveMigrator threeVersions() {
        // Registered out of order on purpose: order of registration must not matter.
        return new SaveMigrator(3).step(2, mark("2to3")).step(1, mark("1to2"));
    }

    @Test
    void stepsRunInVersionOrder() {
        JsonElement result = threeVersions().migrate(1, new JsonArray());

        assertThat(result.toString()).isEqualTo("[\"1to2\",\"2to3\"]");
    }

    @Test
    void onlyTheMissingStepsRun() {
        JsonElement result = threeVersions().migrate(2, new JsonArray());

        assertThat(result.toString()).isEqualTo("[\"2to3\"]");
    }

    @Test
    void currentVersionIsReturnedUnchanged() {
        JsonArray input = new JsonArray();

        assertThat(threeVersions().migrate(3, input)).isSameAs(input);
    }

    @Test
    void newerVersionIsRefused() {
        assertThatThrownBy(() -> threeVersions().migrate(4, new JsonArray()))
                .isInstanceOf(UnsupportedSaveVersionException.class);
    }

    @Test
    void versionBelowOneIsRejected() {
        assertThatThrownBy(() -> threeVersions().migrate(0, new JsonArray()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingStepIsAProgrammingError() {
        SaveMigrator incomplete = new SaveMigrator(3).step(1, mark("1to2"));

        assertThatThrownBy(() -> incomplete.migrate(1, new JsonArray()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidRegistrationsAreRejected() {
        SaveMigrator migrator = new SaveMigrator(2).step(1, mark("1to2"));

        assertThatThrownBy(() -> migrator.step(1, mark("again"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> migrator.step(2, mark("2to3"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SaveMigrator(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
