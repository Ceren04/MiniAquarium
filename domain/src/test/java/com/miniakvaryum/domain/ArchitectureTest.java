package com.miniakvaryum.domain;

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
 * Checks the architecture rules of the domain module by reading its source files.
 * If one of these tests fails, the message names the file and the rule; fix the code,
 * do not weaken the test.
 *
 * <p>Comments are ignored, so Javadoc may mention forbidden things.
 */
class ArchitectureTest {

    private static final Path SOURCES = Path.of("src/main/java");
    private static final String FEATURE_PREFIX = "com.miniakvaryum.domain.feature.";
    private static final Pattern FEATURE_IMPORT =
            Pattern.compile("import\\s+(?:static\\s+)?" + Pattern.quote(FEATURE_PREFIX) + "(\\w+)\\.(\\w+)");

    /** One source file with its comments removed. */
    private record Source(Path path, String code) {

        String normalizedPath() {
            return path.toString().replace('\\', '/');
        }

        /** Name of the feature this file belongs to, or null for core and root files. */
        String feature() {
            Matcher m = Pattern.compile("/domain/feature/(\\w+)/").matcher(normalizedPath());
            return m.find() ? m.group(1) : null;
        }

        boolean isCore() {
            return normalizedPath().contains("/domain/core/");
        }
    }

    private static List<Source> sources;

    @BeforeAll
    static void readSources() throws IOException {
        sources = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path path : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String code = Files.readString(path)
                        .replaceAll("(?s)/\\*.*?\\*/", "")   // block comments and Javadoc
                        .replaceAll("//[^\\n]*", "");        // line comments
                sources.add(new Source(path, code));
            }
        }
        assertThat(sources).as("domain source files found under " + SOURCES.toAbsolutePath()).isNotEmpty();
    }

    private static List<String> filesContaining(String forbidden, String... allowedFileNames) {
        List<String> allowed = List.of(allowedFileNames);
        return sources.stream()
                .filter(s -> s.code().contains(forbidden))
                .filter(s -> !allowed.contains(s.path().getFileName().toString()))
                .map(Source::normalizedPath)
                .toList();
    }

    @Test
    void domainDoesNotUseUiLibraries() {
        for (String forbidden : List.of("com.badlogic", "java.awt", "javax.swing")) {
            assertThat(filesContaining(forbidden))
                    .as("The domain must run in a plain JVM: '%s' is not allowed here. "
                            + "Move drawing and input code to the game module.", forbidden)
                    .isEmpty();
        }
    }

    @Test
    void timeComesOnlyFromClockAndGameTime() {
        for (String forbidden : List.of("System.currentTimeMillis", "System.nanoTime", "Instant.now",
                "LocalDateTime.now", "LocalDate.now")) {
            assertThat(filesContaining(forbidden))
                    .as("'%s' is not allowed in the domain. Use GameTime.nowMillis() "
                            + "(FeatureContext.time()), so tests can control time and the device "
                            + "date cannot be used to cheat.", forbidden)
                    .isEmpty();
        }
    }

    @Test
    void randomnessComesOnlyFromRandomSource() {
        for (String forbidden : List.of("Math.random", "new Random(", "ThreadLocalRandom")) {
            assertThat(filesContaining(forbidden, "JavaRandomSource.java"))
                    .as("'%s' is not allowed in the domain. Use the RandomSource "
                            + "(FeatureContext.random()), so tests can script the values.", forbidden)
                    .isEmpty();
        }
    }

    @Test
    void domainIsSingleThreadedAndNeverExits() {
        for (String forbidden : List.of("Thread.sleep", "new Thread(", "ExecutorService", "System.exit")) {
            assertThat(filesContaining(forbidden))
                    .as("'%s' is not allowed in the domain.", forbidden)
                    .isEmpty();
        }
    }

    @Test
    void coreKnowsNoFeature() {
        List<String> offenders = sources.stream()
                .filter(Source::isCore)
                .filter(s -> s.code().contains(FEATURE_PREFIX))
                .map(Source::normalizedPath)
                .toList();

        assertThat(offenders)
                .as("Files in domain.core must not refer to any feature. The core offers "
                        + "extension points; features plug into them, never the other way round.")
                .isEmpty();
    }

    @Test
    void featuresUseOtherFeaturesOnlyThroughTheirApiPackage() {
        List<String> offenders = new ArrayList<>();
        for (Source source : sources) {
            String own = source.feature();
            if (own == null) {
                continue;
            }
            Matcher m = FEATURE_IMPORT.matcher(source.code());
            while (m.find()) {
                String otherFeature = m.group(1);
                String nextPackage = m.group(2);
                if (!otherFeature.equals(own) && !nextPackage.equals("api")) {
                    offenders.add(source.normalizedPath() + " imports " + FEATURE_PREFIX + otherFeature + "."
                            + nextPackage);
                }
            }
        }

        assertThat(offenders)
                .as("A feature may import from another feature only what is in its 'api' package. "
                        + "If you need something that is not there, ask the owner of that feature "
                        + "to add it to the api, or listen to one of its events.")
                .isEmpty();
    }
}
