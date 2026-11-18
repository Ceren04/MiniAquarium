package com.miniakvaryum.domain.feature.dirt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.feature.dirt.api.AquariumCleaned;
import com.miniakvaryum.domain.feature.dirt.api.DirtService;
import com.miniakvaryum.domain.feature.dirt.api.DirtSpot;
import com.miniakvaryum.domain.testsupport.ScriptedRandom;
import com.miniakvaryum.domain.testsupport.SequentialIds;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DirtServiceTest {

    private static final long HOUR = 3_600_000L;
    private static final DirtConfig CONFIG = DirtConfig.defaults();

    private final ScriptedRandom random = new ScriptedRandom();
    private final EventBus events = new EventBus();
    private final List<AquariumCleaned> cleaned = new ArrayList<>();
    private int changes;
    private long now = 5_000;

    DirtServiceTest() {
        events.subscribe(AquariumCleaned.class, cleaned::add);
    }

    private DirtService service(List<DirtSpot> spots) {
        return service(spots, 0);
    }

    private DirtService service(List<DirtSpot> spots, long progressMillis) {
        return new DirtService(spots, progressMillis, 0, CONFIG, random, new SequentialIds(), events, () -> changes++,
                () -> now);
    }

    /** Scripts the random values for one spot: x, y, radius, variant, rotation. */
    private void scriptSpot(double x, double y, double radius, int variant, double rotation) {
        random.doubles(x, y, radius).ints(variant).doubles(rotation);
    }

    private void scriptSpots(int count) {
        for (int i = 0; i < count; i++) {
            scriptSpot(0.5, 0.5, 0.5, 0, 0.0);
        }
    }

    /** A fresh spot at (x, y) with radius 40. */
    private static DirtSpot spot(String id, double x, double y) {
        return new DirtSpot(id, x, y, 40, 0, 0, 1.0);
    }

    @Test
    void cleanGlassHasLevelZero() {
        DirtService dirt = service(List.of());

        assertThat(dirt.spots()).isEmpty();
        assertThat(dirt.levelPercent()).isZero();
    }

    @Test
    void noSpotBeforeTheFirstInterval() {
        DirtService dirt = service(List.of());

        dirt.advance(2 * HOUR - 1);

        assertThat(dirt.spots()).isEmpty();
        assertThat(dirt.progressMillis()).isEqualTo(2 * HOUR - 1);
        assertThat(changes).isZero();
    }

    @Test
    void newSpotUsesTheRandomValuesInsideItsArea() {
        DirtService dirt = service(List.of());
        scriptSpot(0.0, 1.0, 0.5, 2, 0.25);

        dirt.advance(2 * HOUR);

        assertThat(dirt.spots()).containsExactly(new DirtSpot("id-1", 90, 580, 42, 2, 90, 1.0));
        assertThat(dirt.progressMillis()).isZero();
        assertThat(changes).isEqualTo(1);
    }

    @Test
    void oneSpotPerIntervalAndTheRestIsKept() {
        DirtService dirt = service(List.of());
        scriptSpots(3);

        dirt.advance(7 * HOUR);

        assertThat(dirt.spots()).hasSize(3);
        assertThat(dirt.progressMillis()).isEqualTo(HOUR);
        assertThat(dirt.levelPercent()).isEqualTo(25);
    }

    @Test
    void twentyFourHoursMakeTheGlassCompletelyDirty() {
        DirtService dirt = service(List.of());
        scriptSpots(12);

        dirt.advance(24 * HOUR);

        assertThat(dirt.spots()).hasSize(12);
        assertThat(dirt.levelPercent()).isEqualTo(100);
    }

    @Test
    void timeIsNotBankedWhileTheGlassIsFull() {
        DirtService dirt = service(List.of());
        scriptSpots(12);
        dirt.advance(24 * HOUR);

        dirt.advance(100 * HOUR);

        assertThat(dirt.spots()).hasSize(12);
        assertThat(dirt.progressMillis()).isEqualTo(CONFIG.spotIntervalMillis());
    }

    @Test
    void wipingFadesASpotInProportionToTheRubbing() {
        DirtService dirt = service(List.of(spot("a", 500, 300)));

        boolean touched = dirt.wipe(500, 300, 75);

        assertThat(touched).isTrue();
        assertThat(dirt.spots().get(0).strength()).isCloseTo(0.75, within(1e-9));
        assertThat(changes).isZero();
    }

    @Test
    void fiveTapsRemoveASpot() {
        DirtService dirt = service(List.of(spot("a", 500, 300)));

        for (int i = 0; i < 4; i++) {
            dirt.tap(500, 300);
        }
        assertThat(dirt.spots()).hasSize(1);
        dirt.tap(500, 300);

        assertThat(dirt.spots()).isEmpty();
    }

    @Test
    void fingerReachesTheSpotEdgePlusTheFingerRadius() {
        DirtService dirt = service(List.of(spot("a", 500, 300)));

        // Radius 40 + finger 24 = 64.
        assertThat(dirt.hasSpotAt(564, 300)).isTrue();
        assertThat(dirt.hasSpotAt(565, 300)).isFalse();
        assertThat(dirt.wipe(565, 300, 100)).isFalse();
        assertThat(dirt.spots().get(0).strength()).isEqualTo(1.0);
    }

    @Test
    void oneWipeCanCleanOverlappingSpots() {
        DirtService dirt = service(List.of(spot("a", 500, 300), spot("b", 520, 300), spot("c", 900, 300)));

        dirt.wipe(510, 300, 300);

        assertThat(dirt.spots()).extracting(DirtSpot::id).containsExactly("c");
    }

    @Test
    void removingASpotRequestsASave() {
        DirtService dirt = service(List.of(spot("a", 500, 300), spot("b", 900, 300)));

        dirt.wipe(500, 300, 300);

        assertThat(changes).isEqualTo(1);
        assertThat(cleaned).isEmpty();
    }

    @Test
    void wipingTheLastSpotAnnouncesACleanAquariumOnce() {
        DirtService dirt = service(List.of(spot("a", 500, 300)));

        dirt.wipe(500, 300, 300);
        dirt.wipe(500, 300, 300);

        assertThat(cleaned).hasSize(1);
        assertThat(dirt.levelPercent()).isZero();
    }

    @Test
    void neverCleanedReportsZero() {
        assertThat(service(List.of()).lastCleanedGameMillis()).isZero();
    }

    @Test
    void cleaningStoresTheGameTimeAndRestartsTheInterval() {
        DirtService dirt = service(List.of(spot("a", 500, 300)), 110 * 60_000L);
        now = 9_000_000;

        dirt.wipe(500, 300, 300);

        assertThat(dirt.lastCleanedGameMillis()).isEqualTo(9_000_000);
        assertThat(dirt.progressMillis()).isZero();
    }

    @Test
    void nextDirtComesAFullIntervalAfterCleaning() {
        // 1 h 50 min of the interval had passed when the child cleaned.
        DirtService dirt = service(List.of(spot("a", 500, 300)), 110 * 60_000L);
        dirt.wipe(500, 300, 300);
        scriptSpots(1);

        dirt.advance(2 * HOUR - 1);
        assertThat(dirt.spots()).isEmpty();
        dirt.advance(1);

        assertThat(dirt.spots()).hasSize(1);
    }

    @Test
    void wipingOneOfSeveralSpotsIsNotACleaning() {
        DirtService dirt = service(List.of(spot("a", 500, 300), spot("b", 900, 300)), HOUR);

        dirt.wipe(500, 300, 300);

        assertThat(dirt.lastCleanedGameMillis()).isZero();
        assertThat(dirt.progressMillis()).isEqualTo(HOUR);
    }

    @Test
    void levelCountsPartlyWipedSpots() {
        DirtService dirt = service(List.of(spot("a", 200, 300), spot("b", 600, 300), spot("c", 1000, 300)));

        dirt.wipe(200, 300, 150);

        // (0.5 + 1 + 1) / 12 = 20.8 %
        assertThat(dirt.levelPercent()).isEqualTo(21);
    }

    @Test
    void spotsCannotBeChangedFromOutside() {
        DirtService dirt = service(List.of(spot("a", 500, 300)));

        assertThatThrownBy(() -> dirt.spots().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void negativeValuesAreRejected() {
        DirtService dirt = service(List.of());

        assertThatThrownBy(() -> dirt.advance(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> dirt.wipe(0, 0, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DirtService(List.of(), -1, 0, CONFIG, random, new SequentialIds(), events,
                () -> { }, () -> now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DirtService(List.of(), 0, -1, CONFIG, random, new SequentialIds(), events,
                () -> { }, () -> now)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidConfigIsRejected() {
        assertThatThrownBy(() -> new DirtConfig(0, 1, 0, 1, 0, 1, 1, 2, 1, 0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DirtConfig(1, 1, 5, 1, 0, 1, 1, 2, 1, 0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DirtConfig(1, 1, 0, 1, 0, 1, 1, 2, 1, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
