package com.miniakvaryum.domain.feature.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.time.TimeAnchor;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.security.api.PinState;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests the wiring of the feature (install, save, load). The PIN rules are tested in PinServiceTest. */
class SecurityFeatureTest {

    private final TestGame game = new TestGame();

    private GameSession start() {
        return game.start(new SecurityFeature(SecurityConfig.defaults()));
    }

    @Test
    void newGameHasNoPinAndStartsLocked() {
        GameSession session = start();

        assertThat(session.get(PinService.class).isPinSet()).isFalse();
        assertThat(session.get(PinService.class).state()).isEmpty();
        assertThat(session.get(ParentSession.class).isUnlocked()).isFalse();
    }

    @Test
    void sectionWithoutPinIsWrittenAndReadBack() {
        start().saveNow();

        assertThat(game.store.lastSaved().sections()).containsKey("security");
        assertThat(start().get(PinService.class).isPinSet()).isFalse();
    }

    @Test
    void savedPinStateIsLoadedAndSavedBackUnchanged() {
        PinState saved = new PinState("c2FsdA==", "aGFzaA==", 120_000, 2, 1, 5_000);
        JsonObject data = new JsonObject();
        data.add("pin", new Gson().toJsonTree(saved));
        game.store.save(SaveData.of(new TimeAnchor(0, game.clock.monotonicNanos(), game.clock.wallMillis(), "boot-A"),
                Map.of("security", new SaveSection(1, data))));

        GameSession session = start();

        assertThat(session.get(PinService.class).isPinSet()).isTrue();
        assertThat(session.get(PinService.class).state()).contains(saved);
        // The "unlocked" flag is runtime state: a restart always begins locked.
        assertThat(session.get(ParentSession.class).isUnlocked()).isFalse();

        session.saveNow();
        assertThat(game.store.lastSaved().sections().get("security").data()).isEqualTo(data);
    }
}
