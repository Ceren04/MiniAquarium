package com.miniakvaryum.domain.feature.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.miniakvaryum.domain.feature.security.api.ParentAccessException;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.ParentSessions;
import com.miniakvaryum.domain.testsupport.TestGame;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ParentSessionTest {

    private final ParentSession parent = new TestGame()
            .start(new SecurityFeature(SecurityConfig.defaults()))
            .get(ParentSession.class);

    @Test
    void startsLocked() {
        assertThat(parent.isUnlocked()).isFalse();
        assertThatThrownBy(parent::requireUnlocked).isInstanceOf(ParentAccessException.class);
    }

    @Test
    void unlockedSessionPassesTheGuardUntilItIsLockedAgain() {
        ParentSessions.forceUnlock(parent);
        assertThat(parent.isUnlocked()).isTrue();
        parent.requireUnlocked();

        parent.lock();
        assertThat(parent.isUnlocked()).isFalse();
        assertThatThrownBy(parent::requireUnlocked).isInstanceOf(ParentAccessException.class);
    }

    @Test
    @Disabled("TODO: enable when ParentSession.unlock and PinService are implemented")
    void unlockWithTheCorrectPinOpensTheParentArea() {
        // Set a PIN through PinService, call unlock with it: status OK and isUnlocked() is true.
        fail("TODO: write this test");
    }

    @Test
    @Disabled("TODO: enable when ParentSession.unlock and PinService are implemented")
    void unlockWithAWrongPinStaysLockedAndReturnsTheResult() {
        // Status WRONG with attemptsLeft, isUnlocked() stays false.
        fail("TODO: write this test");
    }
}
