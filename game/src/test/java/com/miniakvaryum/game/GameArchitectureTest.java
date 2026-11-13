package com.miniakvaryum.game;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the architecture rules of the game module by reading its source files.
 * If one of these tests fails, the message names the file and the rule; fix the code,
 * do not weaken the test. Comments are ignored.
 */
class GameArchitectureTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final Pattern GAME_FEATURE_IMPORT =
            Pattern.compile("import\\s+(?:static\\s+)?com\\.miniakvaryum\\.game\\.feature\\.(\\w+)\\.");

    private record Source(String path, String code) {

        /** Name of the UI feature this file belongs to, or null for core and root files. */
        String feature() {
            Matcher m = Pattern.compile("/game/feature/(\\w+)/").matcher(path);
            return m.find() ? m.group(1) : null;
        }
    }

    private static List<Source> sources;

    @BeforeAll
    static void readSources() throws IOException {
        sources = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String code = Files.readString(path)
                        .replaceAll("(?s)/\\*.*?\\*/", "")
                        .replaceAll("//[^\\n]*", "");
                sources.add(new Source(path.toString().replace('\\', '/'), code));
            }
        }
        assertThat(sources).as("game source files found under " + SOURCES.toAbsolutePath()).isNotEmpty();
    }

    private static List<String> filesContaining(String forbidden) {
        return sources.stream().filter(s -> s.code().contains(forbidden)).map(Source::path).toList();
    }

    @Test
    void uiDoesNotReadClocksOrRollDice() {
        for (String forbidden : List.of("System.currentTimeMillis", "System.nanoTime", "Math.random",
                "new Random(", "TimeUtils.millis", "TimeUtils.nanoTime", "MathUtils.random")) {
            assertThat(filesContaining(forbidden))
                    .as("'%s' is not allowed in the game module. Time comes from the frame delta or "
                            + "from session.time(); randomness belongs to the game rules in the "
                            + "domain module.", forbidden)
                    .isEmpty();
        }
    }

    @Test
    void uiNeverExitsOrStartsThreads() {
        for (String forbidden : List.of("System.exit", "new Thread(", "Thread.sleep")) {
            assertThat(filesContaining(forbidden)).as("'%s' is not allowed in the game module.", forbidden).isEmpty();
        }
    }

    @Test
    void uiCoreKnowsNoFeature() {
        List<String> offenders = sources.stream()
                .filter(s -> s.path().contains("/game/core/"))
                .filter(s -> s.code().contains("com.miniakvaryum.game.feature.")
                        || s.code().contains("com.miniakvaryum.domain.feature."))
                .map(Source::path)
                .toList();

        assertThat(offenders)
                .as("Files in game.core must not refer to any feature (UI or domain). "
                        + "The main screen is filled through MainLayout, not by editing the core.")
                .isEmpty();
    }

    @Test
    void uiFeaturesDoNotImportEachOther() {
        List<String> offenders = new ArrayList<>();
        for (Source source : sources) {
            String own = source.feature();
            if (own == null) {
                continue;
            }
            Matcher m = GAME_FEATURE_IMPORT.matcher(source.code());
            while (m.find()) {
                if (!m.group(1).equals(own)) {
                    offenders.add(source.path() + " imports game.feature." + m.group(1));
                }
            }
        }

        assertThat(offenders)
                .as("A UI feature must not import classes of another UI feature. Open its screens "
                        + "by id through the Navigator, and connect features in GameFeatures.")
                .isEmpty();
    }

    @Test
    void uiUsesOnlyThePublicPartsOfDomainFeatures() {
        assertThat(sources.stream()
                .filter(s -> Pattern.compile("com\\.miniakvaryum\\.domain\\.feature\\.\\w+\\.internal\\.")
                        .matcher(s.code()).find())
                .map(Source::path).toList())
                .as("The game module must not import 'internal' packages of domain features.")
                .isEmpty();
    }
}
