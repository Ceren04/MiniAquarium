package com.miniakvaryum.domain.feature.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.security.api.PinState;
import com.miniakvaryum.domain.feature.security.api.VerifyResult;
import com.miniakvaryum.domain.feature.security.api.VerifyStatus;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Required tests for the PIN rules (specification section 9.2).
 *
 * <p>The tests use a cheap hash (1 000 iterations instead of 120 000) because some of them
 * check many PINs; the rules do not depend on the iteration count.
 */
class PinServiceTest {

    private static final SecurityConfig CONFIG = new SecurityConfig(4, 5, 1_000, List.of(30, 60, 300, 900));
    private static final String PIN = "1234";
    private static final String WRONG = "0000";

    private final TestGame game = new TestGame();
    private GameSession session;
    private PinService pins;

    @BeforeEach
    void setUp() {
        session = game.start(new SecurityFeature(CONFIG));
        pins = session.get(PinService.class);
    }

    @Test
    void setThenVerifyAcceptsThePin() {
        pins.setPin(PIN);

        assertThat(pins.isPinSet()).isTrue();
        assertThat(pins.state().orElseThrow().iterations()).isEqualTo(1_000);
        VerifyResult result = pins.verify(PIN);
        assertThat(result.status()).isEqualTo(VerifyStatus.OK);
        assertThat(result.attemptsLeft()).isEqualTo(5);
        assertThat(result.lockedRemainingMillis()).isZero();
    }

    @Test
    void verifyWithoutPinReturnsNotSet() {
        assertThat(pins.verify(PIN).status()).isEqualTo(VerifyStatus.NOT_SET);
        assertThat(pins.state()).isEmpty();
    }

    @Test
    void wrongPinDecrementsAttemptsLeft() {
        pins.setPin(PIN);

        for (int expectedLeft = 4; expectedLeft >= 1; expectedLeft--) {
            VerifyResult result = pins.verify(WRONG);
            assertThat(result.status()).isEqualTo(VerifyStatus.WRONG);
            assertThat(result.attemptsLeft()).isEqualTo(expectedLeft);
        }
        assertThat(pins.verify(WRONG).status()).isEqualTo(VerifyStatus.LOCKED);
    }

    @Test
    void lockoutAfterFiveFailuresWithGrowingDurations() {
        pins.setPin(PIN);

        for (int expectedSeconds : List.of(30, 60, 300, 900, 900)) {
            VerifyResult result = failFiveTimes();
            assertThat(result.status()).isEqualTo(VerifyStatus.LOCKED);
            assertThat(result.lockedRemainingMillis()).isEqualTo(expectedSeconds * 1_000L);
            game.clock.advanceMillis(expectedSeconds * 1_000L);
        }
        assertThat(pins.state().orElseThrow().lockoutStage()).isEqualTo(5);
    }

    @Test
    void verifyDuringLockoutDoesNotAcceptTheRightPin() {
        pins.setPin(PIN);
        failFiveTimes();
        PinState locked = pins.state().orElseThrow();

        VerifyResult result = pins.verify(PIN);

        assertThat(result.status()).isEqualTo(VerifyStatus.LOCKED);
        assertThat(result.lockedRemainingMillis()).isEqualTo(30_000L);
        assertThat(pins.state().orElseThrow()).isEqualTo(locked);
    }

    @Test
    void lockExpiresWithGameTime() {
        pins.setPin(PIN);
        failFiveTimes();

        game.clock.advanceMillis(29_999);
        VerifyResult stillLocked = pins.verify(PIN);
        assertThat(stillLocked.status()).isEqualTo(VerifyStatus.LOCKED);
        assertThat(stillLocked.lockedRemainingMillis()).isEqualTo(1L);

        game.clock.advanceMillis(1);
        assertThat(pins.verify(PIN).status()).isEqualTo(VerifyStatus.OK);
    }

    @Test
    void correctPinResetsFailedAttemptsAndLockoutStage() {
        pins.setPin(PIN);
        failFiveTimes();
        game.clock.advanceMillis(30_000);
        pins.verify(WRONG);
        pins.verify(WRONG);
        assertThat(pins.state().orElseThrow().failedAttempts()).isEqualTo(2);
        assertThat(pins.state().orElseThrow().lockoutStage()).isEqualTo(1);

        assertThat(pins.verify(PIN).status()).isEqualTo(VerifyStatus.OK);

        assertThat(pins.state().orElseThrow().failedAttempts()).isZero();
        assertThat(pins.state().orElseThrow().lockoutStage()).isZero();
        // Stage 0 again: the next lockout is the short one.
        assertThat(failFiveTimes().lockedRemainingMillis()).isEqualTo(30_000L);
    }

    @Test
    void invalidPinFormatIsRejected() {
        List<String> invalid = Arrays.asList("123", "12345", "12a4", "", null, " 123", "١٢٣٤");
        for (String pin : invalid) {
            assertThatThrownBy(() -> pins.setPin(pin)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(pins.isPinSet()).isFalse();

        pins.setPin(PIN);
        PinState before = pins.state().orElseThrow();
        for (String pin : invalid) {
            assertThatThrownBy(() -> pins.verify(pin)).isInstanceOf(IllegalArgumentException.class);
        }
        // A badly formatted PIN is not an attempt.
        assertThat(pins.state().orElseThrow()).isEqualTo(before);
    }

    @Test
    void samePinGetsADifferentHashWithADifferentSalt() {
        PinService first = new PinService(null, session.time(), CONFIG);
        PinService second = new PinService(null, session.time(), CONFIG);

        first.setPin(PIN);
        second.setPin(PIN);

        PinState a = first.state().orElseThrow();
        PinState b = second.state().orElseThrow();
        assertThat(a.saltB64()).isNotEqualTo(b.saltB64());
        assertThat(a.hashB64()).isNotEqualTo(b.hashB64());
        assertThat(first.verify(PIN).status()).isEqualTo(VerifyStatus.OK);
        assertThat(second.verify(PIN).status()).isEqualTo(VerifyStatus.OK);
    }

    @Test
    void settingAPinTwiceIsRefused() {
        pins.setPin(PIN);
        PinState first = pins.state().orElseThrow();

        assertThatThrownBy(() -> pins.setPin("9876")).isInstanceOf(IllegalStateException.class);

        assertThat(pins.state().orElseThrow()).isEqualTo(first);
        assertThat(pins.verify(PIN).status()).isEqualTo(VerifyStatus.OK);
    }

    @Test
    void thePinItselfIsNeverPartOfTheSavedState() {
        pins.setPin(PIN);
        session.saveNow();

        PinState state = pins.state().orElseThrow();
        assertThat(state.saltB64()).doesNotContain(PIN);
        assertThat(state.hashB64()).doesNotContain(PIN);
        String savedJson = game.store.lastSaved().sections().get(SecurityFeature.ID).data().toString();
        assertThat(savedJson).doesNotContain(PIN);
    }

    @Test
    void everyStateChangeRequestsASave() {
        session.tick(0.016);    // writes the first save of a new game
        int afterStart = game.store.saveCount();

        pins.setPin(PIN);
        session.tick(0.016);
        int afterSet = game.store.saveCount();
        assertThat(afterSet).isGreaterThan(afterStart);

        pins.verify(WRONG);
        session.tick(0.016);
        assertThat(game.store.saveCount()).isGreaterThan(afterSet);
    }

    @Test
    void pinStateSurvivesARestart() {
        pins.setPin(PIN);
        pins.verify(WRONG);
        PinState before = pins.state().orElseThrow();
        session.saveNow();

        PinService afterRestart = game.start(new SecurityFeature(CONFIG)).get(PinService.class);

        assertThat(afterRestart.state()).contains(before);
        assertThat(afterRestart.verify(PIN).status()).isEqualTo(VerifyStatus.OK);
    }

    /** Enters a wrong PIN five times and returns the last result (the lockout). */
    private VerifyResult failFiveTimes() {
        VerifyResult result = null;
        for (int i = 0; i < 5; i++) {
            result = pins.verify(WRONG);
        }
        return result;
    }
}
