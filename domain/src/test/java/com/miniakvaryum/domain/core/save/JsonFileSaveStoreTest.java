package com.miniakvaryum.domain.core.save;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miniakvaryum.domain.core.time.TimeAnchor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonFileSaveStoreTest {

    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve("save.json");
    }

    private Path backup() {
        return dir.resolve("save.json.bak");
    }

    private Path temp() {
        return dir.resolve("save.json.tmp");
    }

    /** Save data that is easy to tell apart by its coin value. */
    private static SaveData sample(int coins) {
        JsonObject economy = new JsonObject();
        economy.addProperty("coins", coins);
        Map<String, SaveSection> sections = new LinkedHashMap<>();
        sections.put("economy", new SaveSection(1, economy));
        return SaveData.of(new TimeAnchor(7_200_000L, 123_456_789_000L, 1_763_000_000_000L, "boot-A"), sections);
    }

    private static int coins(SaveData data) {
        return data.sections().get("economy").data().getAsJsonObject().get("coins").getAsInt();
    }

    private List<String> fileNames() throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    // ------------------------------------------------------------ basics

    @Test
    void missingFileReturnsEmpty() {
        assertThat(new JsonFileSaveStore(dir).load()).isEmpty();
    }

    @Test
    void directoryIsCreatedOnFirstSave() {
        Path nested = dir.resolve("a").resolve("b");

        new JsonFileSaveStore(nested).save(sample(1));

        assertThat(nested.resolve("save.json")).exists();
    }

    @Test
    void roundTripKeepsEveryField() {
        // The title contains non-ASCII letters on purpose: parents may type in any language.
        JsonObject tasks = JsonParser.parseString("""
                {"items":[{"id":"7f3c","title":"Odayı topla äöü","coinReward":10,"status":"APPROVED"}],
                 "nested":{"flag":true,"ratio":0.5}}
                """).getAsJsonObject();
        JsonObject fish = new JsonObject();
        fish.add("name", JsonNull.INSTANCE);
        fish.addProperty("feedCount", 3);
        Map<String, SaveSection> sections = new LinkedHashMap<>();
        sections.put("tasks", new SaveSection(2, tasks));
        sections.put("fish", new SaveSection(1, fish));
        SaveData original = SaveData.of(new TimeAnchor(7_200_000L, 123_456_789_000L, 1_763_000_000_000L, ""), sections);

        new JsonFileSaveStore(dir).save(original);
        SaveData loaded = new JsonFileSaveStore(dir).load().orElseThrow();

        assertThat(loaded).isEqualTo(original);
        assertThat(loaded.sections().keySet()).containsExactly("tasks", "fish");
        // A null value must survive, not silently disappear.
        assertThat(loaded.sections().get("fish").data().getAsJsonObject().has("name")).isTrue();
    }

    @Test
    void savedFileHasTheDocumentedShape() throws IOException {
        new JsonFileSaveStore(dir).save(sample(5));

        JsonObject root = JsonParser.parseString(Files.readString(file())).getAsJsonObject();

        assertThat(root.get("schemaVersion").getAsInt()).isEqualTo(1);
        assertThat(root.getAsJsonObject("time").get("gameMillis").getAsLong()).isEqualTo(7_200_000L);
        JsonObject economy = root.getAsJsonObject("sections").getAsJsonObject("economy");
        assertThat(economy.get("version").getAsInt()).isEqualTo(1);
        assertThat(economy.getAsJsonObject("data").get("coins").getAsInt()).isEqualTo(5);
    }

    // ------------------------------------------------------------ atomic write and backup

    @Test
    void secondSaveKeepsThePreviousOneAsBackup() throws IOException {
        JsonFileSaveStore store = new JsonFileSaveStore(dir);

        store.save(sample(1));
        assertThat(backup()).doesNotExist();
        store.save(sample(2));

        assertThat(coins(store.load().orElseThrow())).isEqualTo(2);
        assertThat(Files.readString(backup())).contains("\"coins\": 1");
    }

    @Test
    void noTemporaryFileIsLeftAfterSave() throws IOException {
        JsonFileSaveStore store = new JsonFileSaveStore(dir);

        store.save(sample(1));
        store.save(sample(2));

        assertThat(fileNames()).containsExactly("save.json", "save.json.bak");
    }

    // ------------------------------------------------------------ crash recovery

    @Test
    void truncatedSaveLoadsFromBackupAndIsKept() throws IOException {
        JsonFileSaveStore store = new JsonFileSaveStore(dir);
        store.save(sample(1));
        store.save(sample(2));
        // Simulated crash: the main file is cut off in the middle and a temp file is left behind.
        String full = Files.readString(file());
        Files.writeString(file(), full.substring(0, full.length() / 2));
        Files.writeString(temp(), "{ half written");

        SaveData loaded = new JsonFileSaveStore(dir).load().orElseThrow();

        assertThat(coins(loaded)).isEqualTo(1);
        assertThat(file()).doesNotExist();
        assertThat(fileNames()).anyMatch(name -> name.startsWith("save.json.corrupt-"));
    }

    @Test
    void gameContinuesNormallyAfterRecovery() throws IOException {
        JsonFileSaveStore store = new JsonFileSaveStore(dir);
        store.save(sample(1));
        store.save(sample(2));
        Files.writeString(file(), "not json at all");

        JsonFileSaveStore next = new JsonFileSaveStore(dir);
        next.load();
        next.save(sample(3));

        assertThat(coins(new JsonFileSaveStore(dir).load().orElseThrow())).isEqualTo(3);
    }

    @Test
    void wrongShapeCountsAsCorrupt() throws IOException {
        JsonFileSaveStore store = new JsonFileSaveStore(dir);
        store.save(sample(1));
        store.save(sample(2));
        // Valid JSON, but not a save: the time anchor and the sections are missing.
        Files.writeString(file(), "{\"schemaVersion\": 1}");

        assertThat(coins(new JsonFileSaveStore(dir).load().orElseThrow())).isEqualTo(1);
    }

    @Test
    void corruptSaveWithoutBackupStartsNewGame() throws IOException {
        Files.writeString(file(), "[1, 2, 3]");

        assertThat(new JsonFileSaveStore(dir).load()).isEmpty();
        assertThat(fileNames()).hasSize(1).allMatch(name -> name.startsWith("save.json.corrupt-"));
    }

    @Test
    void corruptSaveAndCorruptBackupStartNewGameWithoutDeletingAnything() throws IOException {
        Files.writeString(file(), "garbage");
        Files.writeString(backup(), "more garbage");

        assertThat(new JsonFileSaveStore(dir).load()).isEmpty();
        assertThat(fileNames()).hasSize(2);
        assertThat(Files.readString(backup())).isEqualTo("more garbage");
    }

    @Test
    void twoCorruptFilesWithTheSameTimestampAreBothKept() throws IOException {
        Files.writeString(file(), "first");
        Files.setLastModifiedTime(file(), java.nio.file.attribute.FileTime.fromMillis(5_000));
        new JsonFileSaveStore(dir).load();
        Files.writeString(file(), "second");
        Files.setLastModifiedTime(file(), java.nio.file.attribute.FileTime.fromMillis(5_000));
        new JsonFileSaveStore(dir).load();

        assertThat(fileNames()).containsExactly("save.json.corrupt-5000", "save.json.corrupt-5000-1");
    }

    // ------------------------------------------------------------ versions

    @Test
    void newerVersionIsRefusedAndNeverOverwritten() throws IOException {
        String newer = "{\"schemaVersion\": 99, \"future\": true}";
        Files.writeString(file(), newer);
        JsonFileSaveStore store = new JsonFileSaveStore(dir);

        assertThatThrownBy(store::load).isInstanceOf(UnsupportedSaveVersionException.class);
        assertThatThrownBy(() -> store.save(sample(1))).isInstanceOf(IllegalStateException.class);

        assertThat(Files.readString(file(), StandardCharsets.UTF_8)).isEqualTo(newer);
        assertThat(fileNames()).containsExactly("save.json");
    }

    @Test
    void olderFrameIsMigratedOnLoad() throws IOException {
        // Pretend version 1 called the time anchor "clock"; version 2 renamed it to "time".
        Files.writeString(file(), """
                {"schemaVersion": 1,
                 "clock": {"gameMillis": 10, "monotonicNanos": 20, "wallMillis": 30, "bootId": "b"},
                 "sections": {}}
                """);
        SaveMigrator migrator = new SaveMigrator(2).step(1, json -> {
            JsonObject root = json.getAsJsonObject();
            root.add("time", root.remove("clock"));
            return root;
        });

        SaveData loaded = new JsonFileSaveStore(dir, migrator).load().orElseThrow();

        assertThat(loaded.schemaVersion()).isEqualTo(2);
        assertThat(loaded.time()).isEqualTo(new TimeAnchor(10, 20, 30, "b"));
    }

    @Test
    void wrongSchemaVersionCannotBeWritten() {
        SaveData wrong = new SaveData(SaveData.SCHEMA_VERSION + 1, sample(1).time(), Map.of());

        assertThatThrownBy(() -> new JsonFileSaveStore(dir).save(wrong))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(file()).doesNotExist();
    }

    // ------------------------------------------------------------ location

    @Test
    void defaultDirectoryHonoursTheSystemProperty() {
        String before = System.getProperty(JsonFileSaveStore.HOME_PROPERTY);
        try {
            System.setProperty(JsonFileSaveStore.HOME_PROPERTY, dir.toString());
            assertThat(JsonFileSaveStore.defaultDirectory()).isEqualTo(dir);

            System.clearProperty(JsonFileSaveStore.HOME_PROPERTY);
            assertThat(JsonFileSaveStore.defaultDirectory())
                    .isEqualTo(Path.of(System.getProperty("user.home"), ".mini-akvaryum"));
        } finally {
            if (before == null) {
                System.clearProperty(JsonFileSaveStore.HOME_PROPERTY);
            } else {
                System.setProperty(JsonFileSaveStore.HOME_PROPERTY, before);
            }
        }
    }
}
