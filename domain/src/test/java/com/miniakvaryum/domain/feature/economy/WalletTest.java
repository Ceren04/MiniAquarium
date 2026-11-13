package com.miniakvaryum.domain.feature.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.LedgerEntry;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WalletTest {

    private final Wallet wallet = new Wallet(List.of());

    @Test
    void creditAndDebitChangeTheBalance() {
        assertThat(wallet.credit(Currency.COIN, 10, "task", "task:1:coin", 100)).isEqualTo(TxResult.OK);
        assertThat(wallet.debit(Currency.COIN, 4, "shop", "buy:1", 200)).isEqualTo(TxResult.OK);

        assertThat(wallet.balance(Currency.COIN)).isEqualTo(6);
        assertThat(wallet.entries()).containsExactly(
                new LedgerEntry("task:1:coin", Currency.COIN, 10, "task", 100),
                new LedgerEntry("buy:1", Currency.COIN, -4, "shop", 200));
    }

    @Test
    void currenciesAreIndependent() {
        wallet.credit(Currency.COIN, 10, "task", "task:1:coin", 0);
        wallet.credit(Currency.FOOD, 2, "task", "task:1:food", 0);

        assertThat(wallet.balance(Currency.COIN)).isEqualTo(10);
        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(2);
    }

    @Test
    void emptyWalletHasZeroBalance() {
        assertThat(wallet.balance(Currency.COIN)).isZero();
        assertThat(wallet.balance(Currency.FOOD)).isZero();
    }

    @Test
    void overdraftIsRefusedAndChangesNothing() {
        wallet.credit(Currency.FOOD, 3, "initial", "init:food", 0);

        assertThat(wallet.debit(Currency.FOOD, 4, "pellet", "pellet:1", 0)).isEqualTo(TxResult.INSUFFICIENT_FUNDS);

        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(3);
        assertThat(wallet.entries()).hasSize(1);
    }

    @Test
    void balanceCanGoDownToExactlyZero() {
        wallet.credit(Currency.FOOD, 3, "initial", "init:food", 0);

        assertThat(wallet.debit(Currency.FOOD, 3, "pellet", "pellet:1", 0)).isEqualTo(TxResult.OK);
        assertThat(wallet.balance(Currency.FOOD)).isZero();
    }

    @Test
    void sameRefIdAndCurrencyIsAppliedOnlyOnce() {
        assertThat(wallet.credit(Currency.COIN, 10, "task", "task:1:coin", 0)).isEqualTo(TxResult.OK);
        assertThat(wallet.credit(Currency.COIN, 10, "task", "task:1:coin", 5)).isEqualTo(TxResult.ALREADY_APPLIED);
        assertThat(wallet.debit(Currency.COIN, 1, "other", "task:1:coin", 5)).isEqualTo(TxResult.ALREADY_APPLIED);

        assertThat(wallet.balance(Currency.COIN)).isEqualTo(10);
        assertThat(wallet.entries()).hasSize(1);
    }

    @Test
    void sameRefIdMayBeUsedForDifferentCurrencies() {
        assertThat(wallet.credit(Currency.COIN, 10, "task", "task:1", 0)).isEqualTo(TxResult.OK);
        assertThat(wallet.credit(Currency.FOOD, 2, "task", "task:1", 0)).isEqualTo(TxResult.OK);
    }

    @Test
    void duplicateIsReportedEvenWhenFundsWouldBeMissing() {
        wallet.credit(Currency.FOOD, 1, "initial", "init:food", 0);
        wallet.debit(Currency.FOOD, 1, "pellet", "pellet:1", 0);

        // Same pellet again with an empty stock: "already done" is the helpful answer.
        assertThat(wallet.debit(Currency.FOOD, 1, "pellet", "pellet:1", 0)).isEqualTo(TxResult.ALREADY_APPLIED);
    }

    @Test
    void zeroAndNegativeAmountsAreInvalid() {
        assertThat(wallet.credit(Currency.COIN, 0, "x", "a", 0)).isEqualTo(TxResult.INVALID_AMOUNT);
        assertThat(wallet.credit(Currency.COIN, -5, "x", "b", 0)).isEqualTo(TxResult.INVALID_AMOUNT);
        assertThat(wallet.debit(Currency.COIN, 0, "x", "c", 0)).isEqualTo(TxResult.INVALID_AMOUNT);
        assertThat(wallet.debit(Currency.COIN, -5, "x", "d", 0)).isEqualTo(TxResult.INVALID_AMOUNT);

        assertThat(wallet.entries()).isEmpty();
        // An invalid attempt does not use up the refId.
        assertThat(wallet.credit(Currency.COIN, 1, "x", "a", 0)).isEqualTo(TxResult.OK);
    }

    @Test
    void blankRefIdIsAProgrammingError() {
        assertThatThrownBy(() -> wallet.credit(Currency.COIN, 1, "x", " ", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.credit(Currency.COIN, 1, "x", null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void balanceIsDerivedFromSavedEntries() {
        Wallet loaded = new Wallet(List.of(
                new LedgerEntry("init:food", Currency.FOOD, 5, "initial", 0),
                new LedgerEntry("task:1:coin", Currency.COIN, 10, "task", 10),
                new LedgerEntry("pellet:1", Currency.FOOD, -1, "pellet", 20)));

        assertThat(loaded.balance(Currency.FOOD)).isEqualTo(4);
        assertThat(loaded.balance(Currency.COIN)).isEqualTo(10);
        // Saved refIds still protect against paying twice after a restart.
        assertThat(loaded.credit(Currency.COIN, 10, "task", "task:1:coin", 30)).isEqualTo(TxResult.ALREADY_APPLIED);
    }

    @Test
    void entriesCannotBeChangedFromOutside() {
        List<LedgerEntry> source = new ArrayList<>();
        source.add(new LedgerEntry("init:food", Currency.FOOD, 5, "initial", 0));
        Wallet loaded = new Wallet(source);

        source.clear();
        assertThat(loaded.balance(Currency.FOOD)).isEqualTo(5);

        assertThatThrownBy(() -> loaded.entries().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> loaded.entries().add(new LedgerEntry("hack", Currency.COIN, 999, "x", 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void listenerHearsOnlyRecordedTransactions() {
        List<LedgerEntry> heard = new ArrayList<>();
        Wallet observed = new Wallet(List.of(new LedgerEntry("init:food", Currency.FOOD, 5, "initial", 0)), heard::add);

        observed.credit(Currency.COIN, 10, "task", "task:1:coin", 7);
        observed.credit(Currency.COIN, 10, "task", "task:1:coin", 8);   // duplicate
        observed.debit(Currency.COIN, 50, "shop", "buy:1", 9);          // too expensive
        observed.credit(Currency.COIN, 0, "x", "zero", 9);              // invalid

        assertThat(heard).containsExactly(new LedgerEntry("task:1:coin", Currency.COIN, 10, "task", 7));
    }
}
