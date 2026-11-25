package com.miniakvaryum.domain.feature.trophies.api;

/** What a trophy gives: coins or food ({@link CurrencyReward}) or a thing ({@link ItemReward}). */
public sealed interface Reward permits CurrencyReward, ItemReward {
}
