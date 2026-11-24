package com.miniakvaryum.domain;

import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.time.LocalClock;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.AquariumFeature;
import com.miniakvaryum.domain.feature.bedtime.BedtimeConfig;
import com.miniakvaryum.domain.feature.bedtime.BedtimeFeature;
import com.miniakvaryum.domain.feature.dirt.DirtConfig;
import com.miniakvaryum.domain.feature.dirt.DirtFeature;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.energy.EnergyConfig;
import com.miniakvaryum.domain.feature.energy.EnergyFeature;
import com.miniakvaryum.domain.feature.energy.FishLevels;
import com.miniakvaryum.domain.feature.profile.ProfileFeature;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import com.miniakvaryum.domain.feature.security.SecurityFeature;
import com.miniakvaryum.domain.feature.stats.StatsFeature;
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

    /**
     * Creates a fresh instance of every feature, in installation order.
     *
     * @param localClock local time of the device, for rules that depend on the hour (bedtime)
     */
    public static List<DomainFeature> all(LocalClock localClock) {
        return List.of(
                new EconomyFeature(EconomyConfig.defaults()),
                new SecurityFeature(SecurityConfig.defaults()),
                new TasksFeature(TasksConfig.defaults()),       // needs economy, security
                new BedtimeFeature(BedtimeConfig.defaults(), localClock),
                new AquariumFeature(AquariumConfig.defaults()), // needs economy; later bedtime (V10)
                // Needs the aquarium. Fish levels do not exist yet (D-030): every fish counts as level 1.
                new EnergyFeature(EnergyConfig.defaults(), FishLevels.ALL_LEVEL_ONE),
                new DirtFeature(DirtConfig.defaults()),
                new ProfileFeature(),                            // needs aquarium
                new StatsFeature()                               // needs economy, tasks, aquarium
        );
    }
}
