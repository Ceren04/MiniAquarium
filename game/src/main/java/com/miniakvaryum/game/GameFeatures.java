package com.miniakvaryum.game;

import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.feature.aquarium.AquariumGameFeature;
import com.miniakvaryum.game.feature.devtools.DevToolsGameFeature;
import com.miniakvaryum.game.feature.dirt.DirtGameFeature;
import com.miniakvaryum.game.feature.economy.EconomyGameFeature;
import com.miniakvaryum.game.feature.profile.ProfileGameFeature;
import com.miniakvaryum.game.feature.security.SecurityGameFeature;
import com.miniakvaryum.game.feature.tasks.TasksGameFeature;
import java.util.List;

/**
 * THE list of UI features. Adding UI for a feature means adding one line here; removing it
 * means deleting that line. The matching list for the game rules is {@code DomainFeatures}.
 *
 * <p>Order decides the order of status texts and buttons in the top bar of the main screen.
 * It is also the place where UI features are connected to each other by screen ids, so that
 * they never import each other's classes.
 */
public final class GameFeatures {

    private GameFeatures() {
    }

    /** Creates a fresh instance of every UI feature. */
    public static List<GameFeature> all() {
        return List.of(
                new ProfileGameFeature(),       // first: the picture is the leftmost item of the top bar
                new EconomyGameFeature(),
                new DevToolsGameFeature(),
                new AquariumGameFeature(),
                new DirtGameFeature(),          // after the aquarium: feed mode wins taps
                new TasksGameFeature(),
                // After a correct PIN the gate opens the parent area of the tasks feature.
                new SecurityGameFeature(TasksGameFeature.SCREEN_PARENT_AREA)
                
        );
    }
}
