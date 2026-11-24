package com.miniakvaryum.game.feature.energy;

import com.miniakvaryum.domain.feature.energy.api.EnergyService;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * UI of the energy feature: the energy bar in the top bar of the main screen, next to coins
 * and food. Spending energy (minigames) is done by the minigame features.
 */
public final class EnergyGameFeature implements GameFeature {

    @Override
    public String id() {
        return "energy";
    }

    @Override
    public void install(UiContext ui) {
        EnergyService energy = ui.session().get(EnergyService.class);
        ui.main().addStatusWidget(new EnergyBar(ui.skin(), ui.shapes(), energy));
        ui.main().addDebugLine(() -> String.format("Energy %.2f, %.1f per hour", energy.exactValue(),
                energy.ratePerHour()));
    }
}
