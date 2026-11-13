package com.miniakvaryum.domain.feature.economy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.miniakvaryum.domain.core.module.DomainFeature;
import com.miniakvaryum.domain.core.module.FeatureContext;
import com.miniakvaryum.domain.core.save.SaveMigrator;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.LedgerChanged;
import com.miniakvaryum.domain.feature.economy.api.LedgerEntry;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Economy feature: provides the {@link Wallet} (coins and food) to all other features.
 *
 * <p><b>This class is the reference example of a feature.</b> Copy its structure:
 * <ol>
 *   <li>{@link #install}: read the saved section (or start with defaults), build the service,
 *       {@code provide} it.</li>
 *   <li>{@link #save}: return the facts that must survive a restart.</li>
 *   <li>Announce changes with an event and ask for a save.</li>
 * </ol>
 *
 * <p>Provides: {@link Wallet}. Requires: nothing. Publishes: {@link LedgerChanged}.
 */
public final class EconomyFeature implements DomainFeature {

    /** Feature id and key of the save section. Never change it. */
    public static final String ID = "economy";

    /**
     * Format version of the save section. When the format changes: raise this number and
     * register a step in {@link #migrator()} that converts the previous format.
     */
    private static final int SECTION_VERSION = 1;

    private final EconomyConfig config;
    private final Gson gson = new Gson();
    private Wallet wallet;

    public EconomyFeature(EconomyConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        List<LedgerEntry> saved = ctx.savedSection().map(this::readSection).orElse(List.of());

        // Every recorded transaction is announced to other features and must not be lost.
        wallet = new Wallet(saved, entry -> {
            ctx.events().publish(new LedgerChanged(entry));
            ctx.requestSave();
        });

        if (ctx.savedSection().isEmpty()) {
            grantInitialStock(ctx.time().nowMillis());
        }
        ctx.provide(Wallet.class, wallet);
    }

    @Override
    public Optional<SaveSection> save() {
        return Optional.of(new SaveSection(SECTION_VERSION, gson.toJsonTree(new Section(wallet.entries()))));
    }

    /** A new game starts with some food and coins, recorded as ordinary ledger entries. */
    private void grantInitialStock(long nowMillis) {
        // Wallet.credit ignores an amount of 0, so "0 coins" simply creates no entry.
        wallet.credit(Currency.FOOD, config.initialFood(), "initial", "init:food", nowMillis);
        wallet.credit(Currency.COIN, config.initialCoins(), "initial", "init:coin", nowMillis);
    }

    private List<LedgerEntry> readSection(SaveSection section) {
        JsonElement current = migrator().migrate(section.version(), section.data());
        Section parsed = gson.fromJson(current, Section.class);
        if (parsed == null || parsed.ledger() == null) {
            throw new IllegalStateException("The '" + ID + "' section of the save file has no ledger");
        }
        return parsed.ledger();
    }

    private static SaveMigrator migrator() {
        // No older formats yet. A future step would look like:
        //     .step(1, json -> convertVersion1To2(json))
        return new SaveMigrator(SECTION_VERSION);
    }

    /** Shape of the save section: {@code { "ledger": [ ... ] }}. */
    private record Section(List<LedgerEntry> ledger) {
    }
}
