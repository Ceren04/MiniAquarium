package com.miniakvaryum.domain.testsupport;

import com.miniakvaryum.domain.DomainFeatures;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.session.CoreConfig;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.session.GameSessionFactory;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A complete fake device for feature tests: fake clock, fake boot id, in-memory save,
 * predictable ids. Start a session with exactly the features the test needs.
 *
 * <pre>
 * TestGame game = new TestGame();
 * GameSession session = game.start(new EconomyFeature(EconomyConfig.defaults()));
 * Wallet wallet = session.get(Wallet.class);
 * ...
 * session.saveNow();
 * GameSession afterRestart = game.start(new EconomyFeature(EconomyConfig.defaults()));
 * </pre>
 * Calling {@link #start} again on the same TestGame simulates closing and reopening the app:
 * the save and the clock are kept, the features are created anew.
 */
public final class TestGame {

    public final FakeClock clock = new FakeClock(1_700_000_000_000L, 5_000_000_000L);
    public final FakeBootId boot = FakeBootId.of("boot-A");
    public final InMemorySaveStore store = new InMemorySaveStore();
    public final ScriptedRandom random = new ScriptedRandom();
    public final SequentialIds ids = new SequentialIds();
    /** Local time for bedtime; 15:00 by default, so bedtime does not interfere with other tests. */
    public final FixedLocalClock localClock = new FixedLocalClock(LocalDateTime.of(2026, 11, 24, 15, 0));

    /**
     * Starts a session with every feature of the real game ({@link DomainFeatures#all}).
     * Fish start in the middle of the swim area.
     */
    public GameSession startFullGame() {
        // The aquarium draws two random numbers (x, y) per fish for its start position.
        int fishCount = AquariumConfig.defaults().fish().initialFishCount();
        for (int i = 0; i < fishCount * 2; i++) {
            random.doubles(0.5);
        }
        return GameSessionFactory.create(clock, boot, store, random, ids, CoreConfig.defaults(), DomainFeatures.all(localClock));
    }

    /** Starts a session with the given features, in the given order. */
    public GameSession start(DomainFeature... features) {
        return GameSessionFactory.create(clock, boot, store, random, ids, CoreConfig.defaults(), List.of(features));
    }
}
