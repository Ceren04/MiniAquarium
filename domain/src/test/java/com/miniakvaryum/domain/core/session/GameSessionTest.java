package com.miniakvaryum.domain.core.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonPrimitive;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.save.SaveStore;
import com.miniakvaryum.domain.core.save.UnsupportedSaveVersionException;
import com.miniakvaryum.domain.core.time.CreditedGap;
import com.miniakvaryum.domain.core.time.GapSource;
import com.miniakvaryum.domain.core.time.TimeAnchor;
import com.miniakvaryum.domain.testsupport.FakeBootId;
import com.miniakvaryum.domain.testsupport.FakeClock;
import com.miniakvaryum.domain.testsupport.InMemorySaveStore;
import com.miniakvaryum.domain.testsupport.ScriptedRandom;
import com.miniakvaryum.domain.testsupport.SequentialIds;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameSessionTest {

    private static final long HOUR = 3_600_000L;

    private final FakeClock clock = new FakeClock(1_700_000_000_000L, 5_000_000_000L);
    private final FakeBootId boot = FakeBootId.of("boot-A");
    private final InMemorySaveStore store = new InMemorySaveStore();
    private final List<String> log = new ArrayList<>();

    private GameSession start(SaveStore saveStore, DomainFeature... features) {
        return GameSessionFactory.create(clock, boot, saveStore, new ScriptedRandom(), new SequentialIds(),
                CoreConfig.defaults(), List.of(features));
    }

    private GameSession start(DomainFeature... features) {
        return start(store, features);
    }

    // ---------------------------------------------------------------- test features

    /** The service that {@link CounterFeature} offers to others. */
    private static final class Counter {
        int value;
    }

    /** A minimal but complete feature: has state, a service, saves and restores itself. */
    private final class CounterFeature implements DomainFeature {
        private final String id;
        private final Counter counter = new Counter();
        FeatureContext ctx;

        CounterFeature(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public void install(FeatureContext context) {
            ctx = context;
            counter.value = context.savedSection().map(s -> s.data().getAsInt()).orElse(0);
            // Only one feature per session may provide a given service type.
            if ("counter".equals(id)) {
                context.provide(Counter.class, counter);
            }
            log.add("install " + id);
        }

        @Override
        public void tick(double dtSeconds) {
            log.add("tick " + id + " " + dtSeconds);
        }

        @Override
        public Optional<SaveSection> save() {
            return Optional.of(new SaveSection(1, new JsonPrimitive(counter.value)));
        }
    }

    /** A feature that depends on the service of another feature. */
    private final class NeedsCounterFeature implements DomainFeature {
        Counter found;

        @Override
        public String id() {
            return "needs-counter";
        }

        @Override
        public void install(FeatureContext ctx) {
            found = ctx.require(Counter.class);
            log.add("install needs-counter");
        }
    }

    // ---------------------------------------------------------------- start-up

    @Test
    void newGameInstallsFeaturesInListOrderWithoutSavedState() {
        CounterFeature counter = new CounterFeature("counter");
        NeedsCounterFeature needs = new NeedsCounterFeature();

        GameSession session = start(counter, needs);

        assertThat(log).containsExactly("install counter", "install needs-counter");
        assertThat(counter.ctx.savedSection()).isEmpty();
        assertThat(session.creditedGap()).isEqualTo(new CreditedGap(0, GapSource.FIRST_RUN, false));
        assertThat(session.time().nowMillis()).isZero();
    }

    @Test
    void servicesAreSharedBetweenFeaturesAndWithTheUi() {
        CounterFeature counter = new CounterFeature("counter");
        NeedsCounterFeature needs = new NeedsCounterFeature();

        GameSession session = start(counter, needs);

        assertThat(needs.found).isSameAs(counter.counter);
        assertThat(session.get(Counter.class)).isSameAs(counter.counter);
        assertThat(session.find(Counter.class)).isPresent();
        assertThat(session.find(String.class)).isEmpty();
    }

    @Test
    void requiringAServiceOfALaterFeatureFails() {
        assertThatThrownBy(() -> start(new NeedsCounterFeature(), new CounterFeature("counter")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Counter");
    }

    @Test
    void featureIdsMustBeValidAndUnique() {
        assertThatThrownBy(() -> start(new CounterFeature("a"), new CounterFeature("a")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("twice");
        assertThatThrownBy(() -> start(new CounterFeature("Has Space")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> start(new CounterFeature(null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void contextGivesAccessToTheSharedParts() {
        CounterFeature counter = new CounterFeature("counter");

        GameSession session = start(counter);

        assertThat(counter.ctx.time()).isSameAs(session.time());
        assertThat(counter.ctx.events()).isSameAs(session.events());
        assertThat(counter.ctx.creditedGap()).isEqualTo(session.creditedGap());
        assertThat(counter.ctx.ids().next()).isEqualTo("id-1");
        assertThat(counter.ctx.random()).isNotNull();
    }

    // ---------------------------------------------------------------- frame loop

    @Test
    void tickAdvancesEveryFeatureInOrder() {
        GameSession session = start(new CounterFeature("a"), new CounterFeature("b"));
        log.clear();

        session.tick(0.25);

        assertThat(log).containsExactly("tick a 0.25", "tick b 0.25");
    }

    @Test
    void negativeDeltaIsRejected() {
        GameSession session = start();

        assertThatThrownBy(() -> session.tick(-0.1)).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------- saving

    @Test
    void newGameIsSavedAtTheEndOfTheFirstFrame() {
        GameSession session = start(new CounterFeature("counter"));
        assertThat(store.saveCount()).isZero();

        session.tick(0.016);

        assertThat(store.saveCount()).isEqualTo(1);
    }

    @Test
    void stateSurvivesARestart() {
        CounterFeature first = new CounterFeature("counter");
        GameSession session = start(first);
        first.counter.value = 42;
        clock.advanceMillis(HOUR);
        session.saveNow();

        clock.advanceMillis(2 * HOUR);
        CounterFeature second = new CounterFeature("counter");
        GameSession restarted = start(second);

        assertThat(second.counter.value).isEqualTo(42);
        assertThat(restarted.creditedGap()).isEqualTo(new CreditedGap(2 * HOUR, GapSource.MONOTONIC, false));
        assertThat(restarted.time().nowMillis()).isEqualTo(3 * HOUR);
    }

    @Test
    void autosaveHappensWhenTheIntervalHasPassed() {
        GameSession session = start(new CounterFeature("counter"));
        session.tick(0);
        assertThat(store.saveCount()).isEqualTo(1);

        session.tick(29.0);
        assertThat(store.saveCount()).isEqualTo(1);

        session.tick(1.0);
        assertThat(store.saveCount()).isEqualTo(2);

        // The timer starts again after a save.
        session.tick(29.0);
        assertThat(store.saveCount()).isEqualTo(2);
    }

    @Test
    void requestedSaveHappensOnceAtTheEndOfTheFrame() {
        CounterFeature counter = new CounterFeature("counter");
        GameSession session = start(counter);
        session.tick(0);

        counter.ctx.requestSave();
        counter.ctx.requestSave();
        assertThat(store.saveCount()).isEqualTo(1);
        session.tick(0.016);
        assertThat(store.saveCount()).isEqualTo(2);

        session.tick(0.016);
        assertThat(store.saveCount()).isEqualTo(2);
    }

    @Test
    void failedAutosaveDoesNotStopTheGameAndIsRetried() {
        GameSession session = start(new CounterFeature("counter"));
        store.setFailing(true);

        session.tick(0.016);
        assertThat(store.saveCount()).isZero();

        store.setFailing(false);
        session.tick(30.0);
        assertThat(store.saveCount()).isEqualTo(1);
    }

    @Test
    void explicitSaveReportsFailure() {
        GameSession session = start(new CounterFeature("counter"));
        store.setFailing(true);

        assertThatThrownBy(session::saveNow).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void featureWithoutStateWritesNoSection() {
        GameSession session = start(new NeedsCounterFeatureWithOwnCounter());

        assertThat(session.toSaveData().sections()).isEmpty();
    }

    /** Stateless feature, used to check that no empty section is written. */
    private static final class NeedsCounterFeatureWithOwnCounter implements DomainFeature {
        @Override
        public String id() {
            return "stateless";
        }

        @Override
        public void install(FeatureContext ctx) {
        }
    }

    @Test
    void sectionOfAFeatureThatIsNotInstalledIsKept() {
        CounterFeature shop = new CounterFeature("shop");
        GameSession withShop = start(new CounterFeature("counter"), shop);
        shop.counter.value = 7;
        withShop.saveNow();

        // Same save, but this build of the game has no shop feature.
        GameSession withoutShop = start(new CounterFeature("counter"));
        withoutShop.saveNow();
        assertThat(store.lastSaved().sections()).containsKeys("counter", "shop");

        // The shop comes back and finds its data.
        CounterFeature shopAgain = new CounterFeature("shop");
        start(new CounterFeature("counter"), shopAgain);
        assertThat(shopAgain.counter.value).isEqualTo(7);
    }

    @Test
    void saveFromNewerGameVersionStopsStartUp() {
        SaveStore newer = new SaveStore() {
            @Override
            public Optional<SaveData> load() {
                throw new UnsupportedSaveVersionException(99, SaveData.SCHEMA_VERSION);
            }

            @Override
            public void save(SaveData data) {
                throw new AssertionError("must not be called");
            }
        };

        assertThatThrownBy(() -> start(newer, new CounterFeature("counter")))
                .isInstanceOf(UnsupportedSaveVersionException.class);
        assertThat(log).isEmpty();
    }

    @Test
    void existingSaveIsNotRewrittenJustBecauseTheGameStarted() {
        SaveData existing = SaveData.of(new TimeAnchor(HOUR, clock.monotonicNanos(), clock.wallMillis(), "boot-A"),
                Map.of("counter", new SaveSection(1, new JsonPrimitive(5))));
        InMemorySaveStore filled = InMemorySaveStore.containing(existing);

        GameSession session = start(filled, new CounterFeature("counter"));
        session.tick(0.016);

        assertThat(filled.saveCount()).isZero();
        assertThat(session.get(Counter.class).value).isEqualTo(5);
    }
}
