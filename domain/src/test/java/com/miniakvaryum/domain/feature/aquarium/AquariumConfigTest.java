package com.miniakvaryum.domain.feature.aquarium;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniakvaryum.domain.feature.aquarium.api.fish.ActionType;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishContext;
import org.junit.jupiter.api.Test;

class AquariumConfigTest {

    private static int[] column(ActionWeights weights, FishContext context) {
        return new int[] {
                weights.weight(context, ActionType.SLOW_MOVE),
                weights.weight(context, ActionType.DASH),
                weights.weight(context, ActionType.IDLE),
                weights.weight(context, ActionType.EAT)};
    }

    @Test
    void defaultWeightsMatchTheSpecificationTable() {
        ActionWeights weights = ActionWeights.defaults();

        assertThat(column(weights, FishContext.NORMAL)).containsExactly(2, 1, 2, 0);
        assertThat(column(weights, FishContext.FOOD_DETECTED)).containsExactly(3, 2, 1, 0);
        assertThat(column(weights, FishContext.FOOD_IN_EATING_RANGE)).containsExactly(0, 0, 1, 4);
    }

    @Test
    void totalsAreTheColumnSums() {
        ActionWeights weights = ActionWeights.defaults();

        assertThat(weights.total(FishContext.NORMAL)).isEqualTo(5);
        assertThat(weights.total(FishContext.FOOD_DETECTED)).isEqualTo(6);
        assertThat(weights.total(FishContext.FOOD_IN_EATING_RANGE)).isEqualTo(5);
    }

    @Test
    void actionOrderIsTheDrawOrderOfTheSpecification() {
        assertThat(ActionType.values()).containsExactly(
                ActionType.SLOW_MOVE, ActionType.DASH, ActionType.IDLE, ActionType.EAT);
    }

    @Test
    void swimAreaFollowsFromWorldSizeAndMargin() {
        WorldConfig world = WorldConfig.defaults();

        assertThat(world.minFishX()).isEqualTo(70);
        assertThat(world.maxFishX()).isEqualTo(1210);
        assertThat(world.minFishY()).isEqualTo(115);
        assertThat(world.maxFishY()).isEqualTo(605);
    }

    @Test
    void defaultsHoldTheSpecificationValues() {
        AquariumConfig config = AquariumConfig.defaults();

        assertThat(config.fish().initialFishCount()).isEqualTo(3);
        assertThat(config.fish().detectRange()).isEqualTo(300);
        assertThat(config.fish().eatRange()).isEqualTo(70);
        assertThat(config.fish().level2Feeds()).isEqualTo(20);
        assertThat(config.fish().level3Hours()).isEqualTo(300);
        assertThat(config.food().pelletStartY()).isEqualTo(620);
        assertThat(config.food().pelletLifetimeS()).isEqualTo(60);
        assertThat(config.food().claimTimeoutS()).isEqualTo(3.5);
    }
}
