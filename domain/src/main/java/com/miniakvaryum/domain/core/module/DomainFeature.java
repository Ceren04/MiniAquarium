package com.miniakvaryum.domain.core.module;

import com.miniakvaryum.domain.core.save.SaveSection;
import java.util.Optional;

/**
 * One self-contained piece of the game rules (economy, tasks, aquarium, later shop, energy, ...).
 *
 * <p>A feature is the only connection between its own package and the rest of the game. To add a
 * feature, implement this interface and add one line to {@code DomainFeatures}. To remove it,
 * delete that line. No other file changes.
 *
 * <p>Life cycle, driven by the session:
 * <ol>
 *   <li>{@link #install} is called once at start-up, in the order of the list in
 *       {@code DomainFeatures}.</li>
 *   <li>{@link #tick} is called once per frame, in the same order.</li>
 *   <li>{@link #save} is called whenever the game is saved.</li>
 * </ol>
 */
public interface DomainFeature {

    /**
     * A short, unique, stable name such as {@code "tasks"}: lower-case letters, digits and dashes.
     * It is the key of this feature's section in the save file, so never change it once
     * players have save files.
     */
    String id();

    /**
     * Sets the feature up. Typical steps: read the saved state from
     * {@link FeatureContext#savedSection()} (empty means: start with defaults), get the services
     * of other features with {@link FeatureContext#require}, create the own service and publish
     * it with {@link FeatureContext#provide}, subscribe to events.
     */
    void install(FeatureContext ctx);

    /**
     * Advances the feature by one frame. Most features have nothing to do here.
     *
     * @param dtSeconds time since the previous frame in seconds
     */
    default void tick(double dtSeconds) {
    }

    /**
     * Returns the state of this feature that must survive a restart, or empty if it has none.
     * Save facts only (for example ledger entries), never values that can be recalculated
     * (a balance) and never runtime details (a fish position).
     */
    default Optional<SaveSection> save() {
        return Optional.empty();
    }
}
