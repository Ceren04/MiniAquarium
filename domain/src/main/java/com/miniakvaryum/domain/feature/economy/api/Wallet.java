package com.miniakvaryum.domain.feature.economy.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Holds the coins and the food of the player as an append-only transaction log (the ledger).
 *
 * <p>A balance is never stored; it is the sum of all entries of that currency. That keeps the
 * history (needed for the transaction log and later for statistics) and makes it impossible
 * for a balance and its history to disagree.
 *
 * <p><b>Paying exactly once.</b> Every transaction carries a {@code refId} that names its cause,
 * for example {@code "task:7f3c:coin"}. A second transaction with the same refId and currency is
 * ignored and reports {@link TxResult#ALREADY_APPLIED}. Callers therefore do not need their own
 * "already paid?" flag: approving a task twice simply cannot pay twice.
 */
public final class Wallet {

    private final List<LedgerEntry> entries = new ArrayList<>();
    private final List<LedgerEntry> readOnlyEntries = Collections.unmodifiableList(entries);
    private final Map<Currency, Long> balances = new EnumMap<>(Currency.class);
    private final Set<String> appliedKeys = new HashSet<>();
    private final Consumer<LedgerEntry> onAppend;

    /** Creates a wallet from saved entries (an empty list for a new game). */
    public Wallet(List<LedgerEntry> entries) {
        this(entries, entry -> { });
    }

    /**
     * @param entries  saved entries, in the order in which they were recorded
     * @param onAppend called once after every recorded transaction (not for {@code entries})
     */
    public Wallet(List<LedgerEntry> entries, Consumer<LedgerEntry> onAppend) {
        this.onAppend = Objects.requireNonNull(onAppend, "onAppend");
        for (LedgerEntry entry : Objects.requireNonNull(entries, "entries")) {
            record(entry);
        }
    }

    /** Returns the current amount of the given currency: the sum of its ledger entries. */
    public long balance(Currency currency) {
        return balances.getOrDefault(Objects.requireNonNull(currency, "currency"), 0L);
    }

    /**
     * Adds {@code amount} to a balance.
     *
     * @param amount     how much to add; must be greater than 0
     * @param reason     short label, for example {@code "task"}
     * @param refId      identifies the cause; the same refId and currency is applied only once
     * @param gameMillis trusted game time now
     * @return {@link TxResult#OK}, {@link TxResult#INVALID_AMOUNT} or {@link TxResult#ALREADY_APPLIED}
     */
    public TxResult credit(Currency currency, long amount, String reason, String refId, long gameMillis) {
        return apply(currency, amount, +1, reason, refId, gameMillis);
    }

    /**
     * Removes {@code amount} from a balance. The balance never becomes negative.
     *
     * @param amount     how much to remove; must be greater than 0
     * @param reason     short label, for example {@code "pellet"}
     * @param refId      identifies the cause; the same refId and currency is applied only once
     * @param gameMillis trusted game time now
     * @return {@link TxResult#OK}, {@link TxResult#INVALID_AMOUNT}, {@link TxResult#ALREADY_APPLIED}
     *         or {@link TxResult#INSUFFICIENT_FUNDS}
     */
    public TxResult debit(Currency currency, long amount, String reason, String refId, long gameMillis) {
        return apply(currency, amount, -1, reason, refId, gameMillis);
    }

    /** Returns all entries in recording order. The list cannot be modified. */
    public List<LedgerEntry> entries() {
        return readOnlyEntries;
    }

    private TxResult apply(Currency currency, long amount, int sign, String reason, String refId, long gameMillis) {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(reason, "reason");
        if (refId == null || refId.isBlank()) {
            throw new IllegalArgumentException("refId must not be blank");
        }
        if (amount <= 0) {
            return TxResult.INVALID_AMOUNT;
        }
        if (appliedKeys.contains(key(currency, refId))) {
            return TxResult.ALREADY_APPLIED;
        }
        if (sign < 0 && balance(currency) < amount) {
            return TxResult.INSUFFICIENT_FUNDS;
        }
        LedgerEntry entry = new LedgerEntry(refId, currency, sign * amount, reason, gameMillis);
        record(entry);
        onAppend.accept(entry);
        return TxResult.OK;
    }

    private void record(LedgerEntry entry) {
        entries.add(entry);
        appliedKeys.add(key(entry.currency(), entry.refId()));
        balances.merge(entry.currency(), entry.delta(), Long::sum);
    }

    private static String key(Currency currency, String refId) {
        return currency.name() + '|' + refId;
    }
}
