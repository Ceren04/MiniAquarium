package com.miniakvaryum.domain.feature.security;

import static org.assertj.core.api.Assertions.fail;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.testsupport.TestGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Required tests for the PIN rules (specification section 7.5).
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test. Use {@code game.clock.advanceMillis(...)} to let lock
 * times pass; never sleep.
 *
 * <p>Tip: hashing with 120 000 iterations takes a noticeable moment per call. For tests that
 * verify many times, start the feature with a cheaper config, for example
 * {@code new SecurityConfig(4, 5, 1_000, List.of(30, 60, 300, 900))}.
 */
@Disabled("TODO: enable when PinService is implemented")
class PinServiceTest {

    private final TestGame game = new TestGame();
    private PinService pins;

    @BeforeEach
    void setUp() {
        GameSession session = game.start(new SecurityFeature(SecurityConfig.defaults()));
        pins = session.get(PinService.class);
    }

    @Test
    void setThenVerifyAcceptsThePin() {
        fail("TODO: write this test");
    }

    @Test
    void verifyWithoutPinReturnsNotSet() {
        fail("TODO: write this test");
    }

    @Test
    void wrongPinDecrementsAttemptsLeft() {
        // 5 attempts allowed: first wrong PIN leaves 4, second leaves 3, ...
        fail("TODO: write this test");
    }

    @Test
    void lockoutAfterFiveFailuresWithGrowingDurations() {
        // Lock durations for consecutive lockouts: 30, 60, 300, 900, 900 seconds.
        fail("TODO: write this test");
    }

    @Test
    void verifyDuringLockoutDoesNotAcceptTheRightPin() {
        fail("TODO: write this test");
    }

    @Test
    void lockExpiresWithGameTime() {
        fail("TODO: write this test");
    }

    @Test
    void correctPinResetsFailedAttemptsAndLockoutStage() {
        fail("TODO: write this test");
    }

    @Test
    void invalidPinFormatIsRejected() {
        // "123", "12345", "12a4", "", null: IllegalArgumentException for setPin and verify.
        fail("TODO: write this test");
    }

    @Test
    void samePinGetsADifferentHashWithADifferentSalt() {
        // Two fresh services, same PIN: saltB64 and hashB64 differ.
        fail("TODO: write this test");
    }

    @Test
    void settingAPinTwiceIsRefused() {
        fail("TODO: write this test");
    }

    @Test
    void thePinItselfIsNeverPartOfTheSavedState() {
        // Neither saltB64 nor hashB64 nor the saved JSON contains "1234".
        fail("TODO: write this test");
    }

    @Test
    void everyStateChangeRequestsASave() {
        // After setPin and after a wrong verify, session.tick(...) increases game.store.saveCount().
        fail("TODO: write this test");
    }

    @Test
    void pinStateSurvivesARestart() {
        // Set a PIN, saveNow, game.start(...) again: verify still accepts the PIN.
        fail("TODO: write this test");
    }
}
