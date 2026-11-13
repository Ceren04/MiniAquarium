package com.miniakvaryum.domain.feature.economy.api;

/**
 * One line of the transaction log: "this currency changed by this amount, for this reason".
 * Entries are facts. They are never edited or removed, only appended.
 *
 * @param refId      identifies the cause, for example {@code "task:7f3c:coin"}; unique per currency
 * @param currency   which balance changed
 * @param delta      the change: positive for a credit, negative for a debit
 * @param reason     short English label for debugging and statistics, for example {@code "task"}
 * @param gameMillis trusted game time of the transaction
 */
public record LedgerEntry(String refId, Currency currency, long delta, String reason, long gameMillis) {
}
