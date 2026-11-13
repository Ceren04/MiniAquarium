package com.miniakvaryum.domain.feature.economy.api;

/**
 * Event: a transaction was recorded in the wallet.
 * Subscribe to it to react to earning or spending (statistics, trophy road, sound effects).
 *
 * @param entry the new ledger entry
 */
public record LedgerChanged(LedgerEntry entry) {
}
