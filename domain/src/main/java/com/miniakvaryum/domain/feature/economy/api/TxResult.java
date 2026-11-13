package com.miniakvaryum.domain.feature.economy.api;

/** Outcome of a wallet operation. Only {@link #OK} changes the wallet. */
public enum TxResult {

    /** The transaction was recorded. */
    OK,

    /** A debit would have made the balance negative. */
    INSUFFICIENT_FUNDS,

    /** A transaction with the same refId and currency exists already; nothing was changed. */
    ALREADY_APPLIED,

    /** The amount was zero or negative. */
    INVALID_AMOUNT
}
