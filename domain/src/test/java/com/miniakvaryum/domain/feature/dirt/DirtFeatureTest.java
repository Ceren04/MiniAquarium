package com.miniakvaryum.domain.feature.dirt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.dirt.api.DirtService;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Wiring of the dirt feature, including test case TC-4 of the MS2 test plan: the dirt level
 * rises with the offline time, limited to 24 hours, and drops to 0 after cleaning.
 */
class DirtFeatureTest {

    private static final long HOUR = 3_600_000L;

    private final TestGame game = new TestGame();

    private GameSession start(int spotsToScript) {
        for (int i = 0; i < spotsToScript; i++) {
            game.random.doubles(0.5, 0.5, 0.5).ints(0).doubles(0.0);
        }
        return game.start(new DirtFeature(DirtConfig.defaults()));
    }

    @Test
    void newGameStartsClean() {
        assertThat(start(0).get(DirtService.class).levelPercent()).isZero();
    }

    @Test
    void offlineTimeMakesTheAquariumDirty() {
        start(0).saveNow();
        game.clock.advanceMillis(6 * HOUR);

        DirtService dirt = start(3).get(DirtService.class);

        assertThat(dirt.spots()).hasSize(3);
        assertThat(dirt.levelPercent()).isEqualTo(25);
    }

    @Test
    void offlineDirtIsLimitedToTwentyFourHours() {
        start(0).saveNow();
        // A different boot and the date set three days forward: only 24 h are credited.
        game.boot.set("boot-B");
        game.clock.setMonotonicNanos(1_000L);
        game.clock.shiftWallMillis(72 * HOUR);

        DirtService dirt = start(12).get(DirtService.class);

        assertThat(dirt.spots()).hasSize(12);
        assertThat(dirt.levelPercent()).isEqualTo(100);
    }

    @Test
    void settingTheDateBackGivesNoDirt() {
        start(0).saveNow();
        game.boot.set("boot-B");
        game.clock.setMonotonicNanos(1_000L);
        game.clock.shiftWallMillis(-72 * HOUR);

        assertThat(start(0).get(DirtService.class).spots()).isEmpty();
    }

    @Test
    void cleaningBringsTheLevelToZero() {
        start(0).saveNow();
        game.clock.advanceMillis(6 * HOUR);
        DirtService dirt = start(3).get(DirtService.class);

        // All three scripted spots sit in the middle of the area; rub there until they are gone.
        while (!dirt.spots().isEmpty()) {
            dirt.wipe(640, 360, 100);
        }

        assertThat(dirt.levelPercent()).isZero();
    }

    @Test
    void dirtGrowsWhileTheGameIsRunning() {
        GameSession session = start(1);

        // Just under two hours of frames of 1/60 s: no spot yet; one second later: a spot.
        for (int i = 0; i < 2 * 60 * 60 * 60 - 60; i++) {
            session.tick(1.0 / 60);
        }
        assertThat(session.get(DirtService.class).spots()).isEmpty();
        for (int i = 0; i < 120; i++) {
            session.tick(1.0 / 60);
        }

        assertThat(session.get(DirtService.class).spots()).hasSize(1);
    }

    @Test
    void spotsAndProgressSurviveARestart() {
        start(0).saveNow();
        game.clock.advanceMillis(5 * HOUR);
        GameSession session = start(2);
        session.get(DirtService.class).wipe(640, 360, 75);
        session.saveNow();

        DirtService reloaded = start(0).get(DirtService.class);

        assertThat(reloaded.spots()).hasSize(2);
        assertThat(reloaded.spots()).allSatisfy(spot -> assertThat(spot.strength()).isEqualTo(0.75));
        assertThat(reloaded.progressMillis()).isEqualTo(HOUR);
    }

    @Test
    void saveSectionHasTheDocumentedShape() {
        start(0).saveNow();
        game.clock.advanceMillis(3 * HOUR);
        start(1).saveNow();

        JsonObject data = game.store.lastSaved().sections().get("dirt").data().getAsJsonObject();

        assertThat(data.keySet()).containsExactlyInAnyOrder("spots", "progressMillis");
        assertThat(data.get("progressMillis").getAsLong()).isEqualTo(HOUR);
        assertThat(data.getAsJsonArray("spots").get(0).getAsJsonObject().keySet())
                .containsExactlyInAnyOrder("id", "x", "y", "radius", "variant", "rotationDeg", "strength");
    }

    @Test
    void sectionWithoutSpotsIsReportedClearly() {
        start(0).saveNow();
        game.store.save(SaveData.of(game.store.lastSaved().time(),
                Map.of("dirt", new SaveSection(1, JsonParser.parseString("{}")))));

        assertThatThrownBy(() -> start(0)).isInstanceOf(IllegalStateException.class).hasMessageContaining("dirt");
    }
}
