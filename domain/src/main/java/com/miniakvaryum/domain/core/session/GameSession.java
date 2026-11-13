package com.miniakvaryum.domain.core.session;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.ServiceRegistry;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.save.SaveStore;
import com.miniakvaryum.domain.core.time.CreditedGap;
import com.miniakvaryum.domain.core.time.GameTime;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One running game: the installed features, their services, the game time and the save logic.
 * Created by {@link GameSessionFactory}; the UI holds exactly one instance.
 *
 * <p>The UI uses it in three ways: {@link #tick} once per frame, {@link #get} to reach a
 * feature's service, and {@link #saveNow} when the game closes.
 */
public final class GameSession {

    private static final Logger LOG = Logger.getLogger(GameSession.class.getName());

    private final GameTime time;
    private final CreditedGap creditedGap;
    private final SaveStore store;
    private final CoreConfig config;
    private final List<DomainFeature> features;
    private final EventBus events = new EventBus();
    private final ServiceRegistry services = new ServiceRegistry();

    /** Sections of features that are not installed right now. Written back unchanged. */
    private final Map<String, SaveSection> foreignSections;

    private boolean saveRequested;
    private double secondsSinceSave;

    GameSession(GameTime time, CreditedGap creditedGap, SaveStore store, CoreConfig config,
            List<DomainFeature> features, Map<String, SaveSection> foreignSections) {
        this.time = time;
        this.creditedGap = creditedGap;
        this.store = store;
        this.config = config;
        this.features = List.copyOf(features);
        this.foreignSections = new LinkedHashMap<>(foreignSections);
    }

    /** Trusted game time of this session. */
    public GameTime time() {
        return time;
    }

    /** The offline time that was credited when this session started. */
    public CreditedGap creditedGap() {
        return creditedGap;
    }

    /** The event bus shared by all features. */
    public EventBus events() {
        return events;
    }

    /**
     * Returns the service of the given type, for example {@code session.get(TaskService.class)}.
     *
     * @throws IllegalStateException if no installed feature provides it
     */
    public <T> T get(Class<T> type) {
        return services.require(type);
    }

    /** Like {@link #get}, but returns empty if the feature that provides the service is not installed. */
    public <T> Optional<T> find(Class<T> type) {
        return services.find(type);
    }

    ServiceRegistry services() {
        return services;
    }

    /**
     * Advances the game by one frame: ticks every feature in installation order, then saves if
     * a save was requested or the autosave interval has passed.
     *
     * <p>A failed automatic save is logged and tried again after the next interval; it does not
     * stop the game.
     *
     * @param dtSeconds time since the previous frame in seconds, not negative
     */
    public void tick(double dtSeconds) {
        if (dtSeconds < 0) {
            throw new IllegalArgumentException("dtSeconds must be >= 0 but was " + dtSeconds);
        }
        for (DomainFeature feature : features) {
            feature.tick(dtSeconds);
        }
        secondsSinceSave += dtSeconds;
        if (saveRequested || secondsSinceSave >= config.autosaveIntervalS()) {
            try {
                saveNow();
            } catch (UncheckedIOException e) {
                LOG.log(Level.WARNING, "Automatic save failed, will try again later", e);
                saveRequested = false;
                secondsSinceSave = 0;
            }
        }
    }

    /** Asks for a save at the end of the current (or next) {@link #tick}. */
    public void requestSave() {
        saveRequested = true;
    }

    /** Collects the time anchor and the section of every feature. */
    public SaveData toSaveData() {
        Map<String, SaveSection> sections = new LinkedHashMap<>(foreignSections);
        for (DomainFeature feature : features) {
            feature.save().ifPresent(section -> sections.put(feature.id(), section));
        }
        return SaveData.of(time.anchor(), sections);
    }

    /**
     * Saves immediately. Call it when the game closes.
     *
     * @throws UncheckedIOException if the data could not be written
     */
    public void saveNow() {
        store.save(toSaveData());
        saveRequested = false;
        secondsSinceSave = 0;
    }
}
