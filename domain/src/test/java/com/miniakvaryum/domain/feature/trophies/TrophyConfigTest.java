package com.miniakvaryum.domain.feature.trophies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.trophies.api.CurrencyReward;
import com.miniakvaryum.domain.feature.trophies.api.ItemKind;
import com.miniakvaryum.domain.feature.trophies.api.ItemReward;
import com.miniakvaryum.domain.feature.trophies.api.Milestone;
import com.miniakvaryum.domain.feature.trophies.api.Reward;
import com.miniakvaryum.domain.feature.trophies.api.Section;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The road of the plan (plan test 11) and the checks of the config. */
class TrophyConfigTest {

    private final List<Milestone> road = TrophyConfig.defaults().milestones();

    /** Food, coins and the four item kinds are six different kinds of reward. */
    private static String kindOf(Reward reward) {
        if (reward instanceof CurrencyReward money) {
            return money.currency().name();
        }
        return ((ItemReward) reward).kind().name();
    }

    @Test
    void theRoadHas39TrophiesUpTo500Tasks() {
        assertThat(road).hasSize(39);
        assertThat(road.get(0).threshold()).isEqualTo(1);
        assertThat(road.get(38).threshold()).isEqualTo(500);
        assertThat(road).extracting(Milestone::section).containsSequence(Section.POND, Section.POND);
        assertThat(road.stream().map(Milestone::section).distinct()).containsExactly(Section.values());
    }

    @Test
    void neverTheSameKindOfRewardTwiceInARow() {
        for (int i = 1; i < road.size(); i++) {
            assertThat(kindOf(road.get(i).reward()))
                    .as("trophy at %d tasks", road.get(i).threshold())
                    .isNotEqualTo(kindOf(road.get(i - 1).reward()));
        }
    }

    @Test
    void everySectionHasADecorationAndAFishPart() {
        for (Section section : Section.values()) {
            Set<ItemKind> kinds = EnumSet.noneOf(ItemKind.class);
            road.stream().filter(m -> m.section() == section).map(Milestone::reward)
                    .filter(ItemReward.class::isInstance).map(r -> ((ItemReward) r).kind()).forEach(kinds::add);
            assertThat(kinds).as(section.name()).contains(ItemKind.DECO, ItemKind.FISH_PART);
        }
    }

    @Test
    void coinsAreFewAndWholeFishComeAt50And250And500() {
        long coins = road.stream().map(Milestone::reward).filter(CurrencyReward.class::isInstance)
                .map(CurrencyReward.class::cast).filter(r -> r.currency() == Currency.COIN)
                .mapToLong(CurrencyReward::amount).sum();
        assertThat(coins).isEqualTo(140);
        assertThat(road.stream().filter(m -> m.reward() instanceof ItemReward item && item.kind() == ItemKind.FISH)
                .map(Milestone::threshold)).containsExactly(50, 250, 500);
    }

    @Test
    void thresholdsMustGoUpAndTheRoadCannotBeEmpty() {
        Milestone five = new Milestone(5, Section.POND, new CurrencyReward(Currency.FOOD, 1));
        Milestone three = new Milestone(3, Section.POND, new CurrencyReward(Currency.FOOD, 1));
        assertThatThrownBy(() -> new TrophyConfig(List.of(five, three))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrophyConfig(List.of(five, five))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TrophyConfig(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CurrencyReward(Currency.COIN, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ItemReward(" ", ItemKind.DECO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Milestone(0, Section.POND, five.reward()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
