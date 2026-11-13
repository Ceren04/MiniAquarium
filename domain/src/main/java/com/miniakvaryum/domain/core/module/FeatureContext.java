package com.miniakvaryum.domain.core.module;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.time.CreditedGap;
import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.core.util.IdGenerator;
import com.miniakvaryum.domain.core.util.RandomSource;
import java.util.Optional;

/**
 * Everything the core offers to one feature while it installs itself.
 * Each feature receives its own context; a feature may keep it for later use.
 */
public interface FeatureContext {

    /** Trusted game time. The session has already been started. */
    GameTime time();

    /** The random source of this session. */
    RandomSource random();

    /** The id generator of this session. */
    IdGenerator ids();

    /** The event bus shared by all features. */
    EventBus events();

    /**
     * The offline time credited at this start-up. Use it for everything that should happen
     * while the app was closed (ageing, and later dirt or energy).
     */
    CreditedGap creditedGap();

    /**
     * This feature's section from the save file. Empty in a new game, and also when the feature
     * was added to the game after the save file was written; start with defaults in both cases.
     */
    Optional<SaveSection> savedSection();

    /**
     * Makes a service available to features installed later and to the UI.
     *
     * @throws IllegalStateException if a service of that type is already provided
     */
    <T> void provide(Class<T> type, T service);

    /**
     * Returns a service provided by a feature that was installed earlier.
     *
     * @throws IllegalStateException if no earlier feature provides it
     */
    <T> T require(Class<T> type);

    /**
     * Asks the session to save at the end of the current frame. Call it after a change that
     * must not be lost (a reward was paid, a task was created). Cheap to call many times:
     * several requests in one frame cause a single save.
     */
    void requestSave();
}
