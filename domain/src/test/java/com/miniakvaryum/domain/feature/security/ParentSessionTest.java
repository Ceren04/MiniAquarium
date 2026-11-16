package com.miniakvaryum.domain.feature.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.security.api.ParentAccessException;
import com.miniakvaryum.domain.feature.security.api.ParentSession;
import com.miniakvaryum.domain.feature.security.api.ParentSessions;
import com.miniakvaryum.domain.feature.security.api.PinService;
import com.miniakvaryum.domain.feature.security.api.VerifyResult;
import com.miniakvaryum.domain.feature.security.api.VerifyStatus;
import com.miniakvaryum.domain.testsupport.TestGame;
import org.junit.jupiter.api.Test;

class ParentSessionTest {

    private final GameSession session = new TestGame().start(new SecurityFeature(SecurityConfig.defaults()));
    private final ParentSession parent = session.get(ParentSession.class);

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
    void unlockWithTheCorrectPinOpensTheParentArea() {
        session.get(PinService.class).setPin("1234");

        VerifyResult result = parent.unlock("1234");

        assertThat(result.status()).isEqualTo(VerifyStatus.OK);
        assertThat(parent.isUnlocked()).isTrue();
        parent.requireUnlocked();
    }

    @Test
    void unlockWithAWrongPinStaysLockedAndReturnsTheResult() {
        session.get(PinService.class).setPin("1234");

        VerifyResult result = parent.unlock("4321");

        assertThat(result.status()).isEqualTo(VerifyStatus.WRONG);
        assertThat(result.attemptsLeft()).isEqualTo(4);
        assertThat(parent.isUnlocked()).isFalse();
        assertThatThrownBy(parent::requireUnlocked).isInstanceOf(ParentAccessException.class);
    }
}
