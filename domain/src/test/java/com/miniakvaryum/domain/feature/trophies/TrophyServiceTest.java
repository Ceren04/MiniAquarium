package com.miniakvaryum.domain.feature.trophies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.trophies.api.ClaimResult;
import com.miniakvaryum.domain.feature.trophies.api.CurrencyReward;
import com.miniakvaryum.domain.feature.trophies.api.ItemKind;
import com.miniakvaryum.domain.feature.trophies.api.ItemReward;
import com.miniakvaryum.domain.feature.trophies.api.Milestone;
import com.miniakvaryum.domain.feature.trophies.api.Section;
import com.miniakvaryum.domain.feature.trophies.api.TrophyClaimed;
import com.miniakvaryum.domain.feature.trophies.api.TrophyService;
import com.miniakvaryum.domain.feature.trophies.api.TrophyState;
import com.miniakvaryum.domain.feature.trophies.api.TrophyUnlocked;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The trophy road on a small road of four trophies (plan tests 2-6, 9, 10). */
class TrophyServiceTest {

    private static final List<Milestone> ROAD = List.of(
            new Milestone(1, Section.POND, new CurrencyReward(Currency.FOOD, 5)),
            new Milestone(3, Section.POND, new CurrencyReward(Currency.COIN, 10)),
            new Milestone(5, Section.POND, new ItemReward("deco.plant", ItemKind.DECO)),
            new Milestone(8, Section.RIVER, new CurrencyReward(Currency.FOOD, 8)));

    private final Wallet wallet = new Wallet(List.of());
    private final EventBus events = new EventBus();
    private final List<Object> published = new ArrayList<>();
    private int saves;

    private TrophyService road(List<String> counted, List<Integer> claimed) {
        events.subscribe(TrophyUnlocked.class, published::add);
        events.subscribe(TrophyClaimed.class, published::add);
        return new TrophyService(ROAD, wallet, () -> 42L, events, counted, claimed, () -> saves++);
    }

    private static void approve(TrophyService trophies, int count) {
        for (int i = 0; i < count; i++) {
            trophies.countApproved("t-" + trophies.approvedCount());
        }
    }

    @Test
    void theSameTaskCountsOnce() {
        TrophyService trophies = road(List.of(), List.of());
        assertThat(trophies.countApproved("t-1")).isTrue();
        assertThat(trophies.countApproved("t-1")).isFalse();
        assertThat(trophies.approvedCount()).isEqualTo(1);
        assertThat(saves).isEqualTo(1);
    }

    @Test
    void aTrophyIsReadyAtItsThresholdAndLockedBelow() {
        TrophyService trophies = road(List.of(), List.of());
        approve(trophies, 2);
        assertThat(trophies.stateOf(1)).isEqualTo(TrophyState.READY);
        assertThat(trophies.stateOf(3)).isEqualTo(TrophyState.LOCKED);
        assertThat(trophies.readyCount()).isEqualTo(1);
        assertThat(trophies.nextMilestone()).map(Milestone::threshold).contains(3);
        assertThat(published).containsExactly(new TrophyUnlocked(ROAD.get(0)));
    }

    @Test
    void claimPaysOnceAndASecondClaimChangesNothing() {
        TrophyService trophies = road(List.of(), List.of());
        approve(trophies, 3);
        assertThat(trophies.claim(3)).isEqualTo(ClaimResult.OK);
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(10);
        assertThat(trophies.claim(3)).isEqualTo(ClaimResult.ALREADY_CLAIMED);
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(10);
        assertThat(trophies.stateOf(3)).isEqualTo(TrophyState.CLAIMED);
        assertThat(trophies.claimedCount()).isEqualTo(1);
        assertThat(published).contains(new TrophyClaimed(ROAD.get(1)));
    }

    @Test
    void aLockedTrophyCannotBeClaimed() {
        TrophyService trophies = road(List.of(), List.of());
        assertThat(trophies.claim(1)).isEqualTo(ClaimResult.NOT_READY);
        assertThat(wallet.balance(Currency.FOOD)).isZero();
        assertThatThrownBy(() -> trophies.claim(2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anItemIsRememberedAndTheWalletDoesNotChange() {
        TrophyService trophies = road(List.of(), List.of());
        approve(trophies, 5);
        trophies.claim(5);
        assertThat(trophies.earnedItems()).containsExactly(new ItemReward("deco.plant", ItemKind.DECO));
        assertThat(wallet.entries()).isEmpty();
    }

    @Test
    void aPaymentMadeBeforeACrashIsNotMadeAgain() {
        // The coins were credited, then the game stopped before the trophy was saved as collected.
        wallet.credit(Currency.COIN, 10, "trophy", "trophy:3", 1L);
        TrophyService trophies = road(List.of("a", "b", "c"), List.of());
        assertThat(trophies.claim(3)).isEqualTo(ClaimResult.OK);
        assertThat(wallet.balance(Currency.COIN)).isEqualTo(10);
        assertThat(trophies.stateOf(3)).isEqualTo(TrophyState.CLAIMED);
    }

    @Test
    void passingSeveralThresholdsAtOnceMakesEachReady() {
        TrophyService trophies = road(List.of("a", "b", "c", "d", "e", "f", "g"), List.of(1));
        trophies.countApproved("h");
        assertThat(trophies.stateOf(1)).isEqualTo(TrophyState.CLAIMED);
        assertThat(trophies.stateOf(3)).isEqualTo(TrophyState.READY);
        assertThat(trophies.stateOf(5)).isEqualTo(TrophyState.READY);
        assertThat(trophies.stateOf(8)).isEqualTo(TrophyState.READY);
        assertThat(trophies.readyCount()).isEqualTo(3);
        assertThat(trophies.nextMilestone()).isEmpty();
    }

    @Test
    void theSaveKeepsCountingOrderAndSortedClaims() {
        TrophyService trophies = road(List.of("b", "a"), List.of(3, 1));
        assertThat(trophies.savedCountedTaskIds()).containsExactly("b", "a");
        assertThat(trophies.savedClaimed()).containsExactly(1, 3);
        assertThat(trophies.milestones()).isEqualTo(ROAD);
    }
}
