package com.miniakvaryum.domain.feature.economy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.save.UnsupportedSaveVersionException;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.LedgerChanged;
import com.miniakvaryum.domain.feature.economy.api.LedgerEntry;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EconomyFeatureTest {

    private final TestGame game = new TestGame();

    private GameSession start() {
        return game.start(new EconomyFeature(EconomyConfig.defaults()));
    }

    @Test
    void newGameGrantsInitialStockAsLedgerEntries() {
        Wallet wallet = start().get(Wallet.class);

        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(5);
        assertThat(wallet.balance(Currency.COIN)).isZero();
        // 0 coins creates no entry.
        assertThat(wallet.entries()).containsExactly(new LedgerEntry("init:food", Currency.FOOD, 5, "initial", 0));
    }

    @Test
    void initialStockComesFromTheConfig() {
        Wallet wallet = game.start(new EconomyFeature(new EconomyConfig(2, 30))).get(Wallet.class);

        assertThat(wallet.balance(Currency.FOOD)).isEqualTo(2);
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(30);
    }

    @Test
    void ledgerSurvivesARestartAndInitialStockIsNotGrantedAgain() {
        GameSession session = start();
        session.get(Wallet.class).credit(Currency.COIN, 10, "task", "task:1:coin", 50);
        session.get(Wallet.class).debit(Currency.FOOD, 1, "pellet", "pellet:1", 60);
        session.saveNow();

        Wallet reloaded = start().get(Wallet.class);

        assertThat(reloaded.balance(Currency.COIN)).isEqualTo(10);
        assertThat(reloaded.balance(Currency.FOOD)).isEqualTo(4);
        assertThat(reloaded.entries()).hasSize(3);
        assertThat(reloaded.credit(Currency.COIN, 10, "task", "task:1:coin", 70)).isEqualTo(TxResult.ALREADY_APPLIED);
    }

    @Test
    void everyTransactionIsAnnouncedAsAnEvent() {
        GameSession session = start();
        List<LedgerEntry> heard = new ArrayList<>();
        session.events().subscribe(LedgerChanged.class, event -> heard.add(event.entry()));

        session.get(Wallet.class).credit(Currency.COIN, 10, "task", "task:1:coin", 50);

        assertThat(heard).containsExactly(new LedgerEntry("task:1:coin", Currency.COIN, 10, "task", 50));
    }

    @Test
    void transactionIsSavedAtTheEndOfTheFrame() {
        GameSession session = start();
        session.tick(0.016);
        int savesBefore = game.store.saveCount();

        session.get(Wallet.class).credit(Currency.COIN, 10, "task", "task:1:coin", 50);
        session.tick(0.016);

        assertThat(game.store.saveCount()).isEqualTo(savesBefore + 1);
    }

    @Test
    void saveSectionHasTheDocumentedShape() {
        GameSession session = start();
        session.saveNow();

        SaveSection section = game.store.lastSaved().sections().get("economy");
        JsonObject first = section.data().getAsJsonObject().getAsJsonArray("ledger").get(0).getAsJsonObject();

        assertThat(section.version()).isEqualTo(1);
        assertThat(first.get("refId").getAsString()).isEqualTo("init:food");
        assertThat(first.get("currency").getAsString()).isEqualTo("FOOD");
        assertThat(first.get("delta").getAsLong()).isEqualTo(5);
        assertThat(first.get("reason").getAsString()).isEqualTo("initial");
        assertThat(first.get("gameMillis").getAsLong()).isZero();
    }

    @Test
    void sectionFromNewerVersionStopsStartUp() {
        GameSession session = start();
        session.saveNow();
        SaveData saved = game.store.lastSaved();
        game.store.save(SaveData.of(saved.time(),
                Map.of("economy", new SaveSection(99, saved.sections().get("economy").data()))));

        assertThatThrownBy(this::start).isInstanceOf(UnsupportedSaveVersionException.class);
    }

    @Test
    void sectionWithoutLedgerIsReportedClearly() {
        GameSession session = start();
        session.saveNow();
        game.store.save(SaveData.of(game.store.lastSaved().time(),
                Map.of("economy", new SaveSection(1, JsonParser.parseString("{}")))));

        assertThatThrownBy(this::start).isInstanceOf(IllegalStateException.class).hasMessageContaining("economy");
    }

    @Test
    void negativeInitialAmountsAreRejected() {
        assertThatThrownBy(() -> new EconomyConfig(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
