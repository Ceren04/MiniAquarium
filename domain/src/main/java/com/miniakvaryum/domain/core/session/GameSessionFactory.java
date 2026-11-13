package com.miniakvaryum.domain.core.session;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.save.SaveStore;
import com.miniakvaryum.domain.core.time.BootIdProvider;
import com.miniakvaryum.domain.core.time.Clock;
import com.miniakvaryum.domain.core.time.CreditedGap;
import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.core.util.IdGenerator;
import com.miniakvaryum.domain.core.util.RandomSource;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds a {@link GameSession}. This is the composition root: the one place where the parts of
 * the game are connected to each other. The UI calls it once at start-up.
 */
public final class GameSessionFactory {

    private static final Pattern FEATURE_ID = Pattern.compile("[a-z][a-z0-9-]*");

    private GameSessionFactory() {
    }

    /**
     * Loads the save (if any), starts the game time and installs the features in list order.
     *
     * @param clock    device clocks (platform implementation, or a fake in tests)
     * @param boot     boot id of the device
     * @param store    where the save is read from and written to
     * @param random   random source for all features
     * @param ids      id generator for all features
     * @param config   core settings
     * @param features the features of this game, in installation order; a feature may only
     *                 require services of features that come before it
     * @throws com.miniakvaryum.domain.core.save.UnsupportedSaveVersionException
     *                                  if the save was written by a newer version of the game
     * @throws IllegalArgumentException if a feature id is invalid or used twice
     */
    public static GameSession create(Clock clock, BootIdProvider boot, SaveStore store,
            RandomSource random, IdGenerator ids, CoreConfig config, List<DomainFeature> features) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(ids, "ids");
        Set<String> featureIds = validateIds(features);

        Optional<SaveData> saved = store.load();
        GameTime time = new GameTime(clock, boot, config.maxGapMillis());
        CreditedGap gap = time.startSession(saved.map(SaveData::time));

        Map<String, SaveSection> savedSections = saved.map(SaveData::sections).orElse(Map.of());
        // Sections whose feature is not installed (removed, or not written yet on this branch)
        // are carried along untouched, so that data is not lost.
        Map<String, SaveSection> foreign = new LinkedHashMap<>(savedSections);
        foreign.keySet().removeAll(featureIds);

        GameSession session = new GameSession(time, gap, store, config, features, foreign);
        for (DomainFeature feature : features) {
            Optional<SaveSection> section = Optional.ofNullable(savedSections.get(feature.id()));
            feature.install(new Context(session, random, ids, section));
        }
        if (saved.isEmpty()) {
            // New game: write the freshly created state at the end of the first frame.
            session.requestSave();
        }
        return session;
    }

    private static Set<String> validateIds(List<DomainFeature> features) {
        Set<String> seen = new HashSet<>();
        for (DomainFeature feature : features) {
            String id = feature.id();
            if (id == null || !FEATURE_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid feature id '" + id
                        + "': use lower-case letters, digits and dashes, starting with a letter");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Feature id '" + id + "' is used twice");
            }
        }
        return seen;
    }

    /** The view of the session that one feature gets. */
    private record Context(GameSession session, RandomSource random, IdGenerator ids,
            Optional<SaveSection> savedSection) implements FeatureContext {

        @Override
        public GameTime time() {
            return session.time();
        }

        @Override
        public EventBus events() {
            return session.events();
        }

        @Override
        public CreditedGap creditedGap() {
            return session.creditedGap();
        }

        @Override
        public <T> void provide(Class<T> type, T service) {
            session.services().provide(type, service);
        }

        @Override
        public <T> T require(Class<T> type) {
            return session.services().require(type);
        }

        @Override
        public void requestSave() {
            session.requestSave();
        }
    }
}
