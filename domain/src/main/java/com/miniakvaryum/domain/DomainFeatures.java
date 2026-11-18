package com.miniakvaryum.domain;

import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.AquariumFeature;
import com.miniakvaryum.domain.feature.dirt.DirtConfig;
import com.miniakvaryum.domain.feature.dirt.DirtFeature;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import com.miniakvaryum.domain.feature.security.SecurityFeature;
import com.miniakvaryum.domain.feature.tasks.TasksConfig;
import com.miniakvaryum.domain.feature.tasks.TasksFeature;
import java.util.List;

/**
 * THE list of game-rule features. Adding a feature to the game means adding one line here;
 * removing it means deleting that line.
 *
 * <p>Order matters: features are installed from top to bottom, and a feature can only use
 * services of features above it. Keep foundations (economy, security) at the top.
 */
public final class DomainFeatures {

    private DomainFeatures() {
    }

    /** Creates a fresh instance of every feature, in installation order. */
    public static List<DomainFeature> all() {
        return List.of(
                new EconomyFeature(EconomyConfig.defaults()),
                new SecurityFeature(SecurityConfig.defaults()),
                new TasksFeature(TasksConfig.defaults()),       // needs economy, security
                new AquariumFeature(AquariumConfig.defaults()), // needs economy
                new DirtFeature(DirtConfig.defaults())
        );
    }
}
