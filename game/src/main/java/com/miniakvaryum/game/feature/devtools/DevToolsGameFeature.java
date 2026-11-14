package com.miniakvaryum.game.feature.devtools;

import com.miniakvaryum.domain.feature.aquarium.api.world.AquariumWorld;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * Developer tools for trying the game by hand. NOT part of the product.
 *
 * <p>Press the toggle key ({@link DevBar#TOGGLE_KEY}) on the main screen to show or hide the
 * developer bar at the bottom of the screen. Food is added through the normal {@link Wallet}, so
 * the ledger rules still apply and the added food is saved like any other ledger entry.
 *
 * <p>Register this feature in {@code GameFeatures} <b>before</b> the aquarium feature: in feed
 * mode the aquarium takes every tap, and the first tap handler that returns true wins.
 */
public final class DevToolsGameFeature implements GameFeature {

    private static final int FOOD_AMOUNT = 5;
    private static final String REASON = "dev";
    private static final String REF_PREFIX = "dev:food:";
    private static final int BAR_LAYER_Z = 100;
    private static final float MESSAGE_SECONDS = 2f;

    @Override
    public String id() {
        return "dev-tools";
    }

    @Override
    public void install(UiContext ui) {
        Wallet wallet = ui.session().get(Wallet.class);
        float worldWidth = (float) ui.session().get(AquariumWorld.class).config().world().worldWidth();

        DevBar bar = new DevBar(ui.skin(), worldWidth, () -> addFood(ui, wallet));
        ui.main().addWorldLayer(BAR_LAYER_Z, bar);
        ui.main().addWorldTapHandler((x, y) -> bar.handleTap((float) x, (float) y));
    }

    private void addFood(UiContext ui, Wallet wallet) {
        // Every wallet transaction needs a refId that has never been used. Game time only goes
        // forward, also across restarts, so it makes each click unique.
        long now = ui.session().time().nowMillis();
        TxResult result = wallet.credit(Currency.FOOD, FOOD_AMOUNT, REASON, REF_PREFIX + now, now);
        if (result != TxResult.OK) {
            ui.main().showMessage(DevToolsStrings.NOT_ADDED, MESSAGE_SECONDS);
        }
    }
}