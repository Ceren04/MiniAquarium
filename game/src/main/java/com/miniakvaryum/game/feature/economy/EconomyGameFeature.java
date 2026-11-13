package com.miniakvaryum.game.feature.economy;

import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.game.core.GameFeature;
import com.miniakvaryum.game.core.UiContext;

/**
 * UI of the economy feature: shows the coin and food balance in the top bar of the main screen.
 *
 * <p>This class is the reference example of a small game feature: get a domain service, add
 * something to the main screen.
 *
 * <p>TODO (spec section 8.5): show the food as "stock (free)" once {@code FoodService.available()}
 * is implemented, for example "Food: 5 (3 free)".
 */
public final class EconomyGameFeature implements GameFeature {

    @Override
    public String id() {
        return "economy";
    }

    @Override
    public void install(UiContext ui) {
        Wallet wallet = ui.session().get(Wallet.class);
        // The suppliers are called every frame, so the texts always show the current balance.
        ui.main().addStatus(() -> EconomyStrings.COINS + wallet.balance(Currency.COIN));
        ui.main().addStatus(() -> EconomyStrings.FOOD + wallet.balance(Currency.FOOD));
    }
}
