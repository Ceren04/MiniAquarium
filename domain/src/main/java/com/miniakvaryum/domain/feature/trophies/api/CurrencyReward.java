package com.miniakvaryum.domain.feature.trophies.api;

import com.miniakvaryum.domain.feature.economy.api.Currency;
import java.util.Objects;

/** Coins or food, paid into the wallet when the trophy is collected. */
public record CurrencyReward(Currency currency, long amount) implements Reward {

    public CurrencyReward {
        Objects.requireNonNull(currency, "currency");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
    }
}
