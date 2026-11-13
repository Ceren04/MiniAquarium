package com.miniakvaryum.domain.feature.aquarium.api.fish;

import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Required tests for fish levels (specification section 7.7).
 *
 * <p>How to work here: remove the {@code @Disabled} line of the class, then replace each
 * {@code fail(...)} with a real test. Use {@code FishConfig.defaults()}.
 */
@Disabled("TODO: enable when LevelRules is implemented")
class LevelRulesTest {

    @Test
    void newFishIsLevelOne() {
        fail("TODO: write this test");
    }

    @Test
    void levelTwoNeedsTwentyFeeds() {
        // 19 feeds with 48 h is level 1; 20 feeds with 48 h is level 2.
        fail("TODO: write this test");
    }

    @Test
    void levelTwoNeedsFortyEightHours() {
        // 20 feeds with 47 h is level 1; 20 feeds with 48 h is level 2.
        fail("TODO: write this test");
    }

    @Test
    void levelThreeNeedsHundredFeeds() {
        // 99 feeds with 300 h is level 2; 100 feeds with 300 h is level 3.
        fail("TODO: write this test");
    }

    @Test
    void levelThreeNeedsThreeHundredHours() {
        // 100 feeds with 299 h is level 2; 100 feeds with 300 h is level 3.
        fail("TODO: write this test");
    }

    @Test
    void partOfAnHourDoesNotCount() {
        // 47 h 59 min 59 s is still 47 hours.
        fail("TODO: write this test");
    }
}
