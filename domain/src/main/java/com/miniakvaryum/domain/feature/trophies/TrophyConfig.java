package com.miniakvaryum.domain.feature.trophies;

import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.trophies.api.CurrencyReward;
import com.miniakvaryum.domain.feature.trophies.api.ItemKind;
import com.miniakvaryum.domain.feature.trophies.api.ItemReward;
import com.miniakvaryum.domain.feature.trophies.api.Milestone;
import com.miniakvaryum.domain.feature.trophies.api.Reward;
import com.miniakvaryum.domain.feature.trophies.api.Section;
import java.util.List;
import java.util.Objects;

/**
 * The trophies of the road, sorted by the number of approved tasks.
 *
 * <p>{@link #defaults()} is the road of the plan "V12 - Pokalstrasse": 5 sections, 39 trophies
 * up to 500 tasks. Tests can use small roads. A trophy's id is its threshold, so new trophies can
 * be added later without breaking saves.
 */
public record TrophyConfig(List<Milestone> milestones) {

    public TrophyConfig {
        milestones = List.copyOf(Objects.requireNonNull(milestones, "milestones"));
        if (milestones.isEmpty()) {
            throw new IllegalArgumentException("the road needs at least one trophy");
        }
        for (int i = 1; i < milestones.size(); i++) {
            if (milestones.get(i).threshold() <= milestones.get(i - 1).threshold()) {
                throw new IllegalArgumentException("thresholds must go up: " + milestones.get(i).threshold());
            }
        }
    }

    /** The 39 trophies of the plan. Item ids are drafts, to be agreed with the inventory (V11). */
    public static TrophyConfig defaults() {
        return new TrophyConfig(List.of(
                m(1, Section.POND, food(5)),
                m(3, Section.POND, coins(10)),
                m(5, Section.POND, item("deco.plant", ItemKind.DECO)),
                m(8, Section.POND, food(8)),
                m(12, Section.POND, item("part.rainbow.tail", ItemKind.FISH_PART)),
                m(16, Section.POND, coins(15)),
                m(20, Section.POND, item("acc.hat", ItemKind.ACCESSORY)),
                m(25, Section.POND, item("part.rainbow.fin", ItemKind.FISH_PART)),
                m(30, Section.POND, food(10)),
                m(35, Section.POND, item("part.rainbow.head", ItemKind.FISH_PART)),
                m(40, Section.POND, item("deco.rock", ItemKind.DECO)),
                m(45, Section.POND, item("part.rainbow.body", ItemKind.FISH_PART)),
                m(50, Section.POND, item("fish.clown", ItemKind.FISH)),
                m(60, Section.RIVER, coins(20)),
                m(70, Section.RIVER, item("acc.glasses", ItemKind.ACCESSORY)),
                m(80, Section.RIVER, food(12)),
                m(90, Section.RIVER, item("deco.shell", ItemKind.DECO)),
                m(100, Section.RIVER, item("part.gold.tail", ItemKind.FISH_PART)),
                m(110, Section.RIVER, coins(25)),
                m(125, Section.RIVER, item("acc.bow", ItemKind.ACCESSORY)),
                m(140, Section.LAGOON, item("part.gold.fin", ItemKind.FISH_PART)),
                m(155, Section.LAGOON, food(15)),
                m(170, Section.LAGOON, item("deco.chest", ItemKind.DECO)),
                m(185, Section.LAGOON, item("part.gold.head", ItemKind.FISH_PART)),
                m(200, Section.LAGOON, coins(30)),
                m(215, Section.LAGOON, item("acc.scarf", ItemKind.ACCESSORY)),
                m(230, Section.LAGOON, item("part.gold.body", ItemKind.FISH_PART)),
                m(250, Section.LAGOON, item("fish.puffer", ItemKind.FISH)),
                m(270, Section.REEF, food(15)),
                m(290, Section.REEF, item("deco.coral", ItemKind.DECO)),
                m(310, Section.REEF, item("part.crystal.tail", ItemKind.FISH_PART)),
                m(330, Section.REEF, coins(40)),
                m(350, Section.REEF, item("acc.crown", ItemKind.ACCESSORY)),
                m(375, Section.REEF, item("part.crystal.fin", ItemKind.FISH_PART)),
                m(400, Section.OCEAN, item("deco.castle", ItemKind.DECO)),
                m(425, Section.OCEAN, item("part.crystal.head", ItemKind.FISH_PART)),
                m(450, Section.OCEAN, food(20)),
                m(475, Section.OCEAN, item("part.crystal.body", ItemKind.FISH_PART)),
                m(500, Section.OCEAN, item("fish.legend", ItemKind.FISH))));
    }

    private static Milestone m(int threshold, Section section, Reward reward) {
        return new Milestone(threshold, section, reward);
    }

    private static Reward food(long amount) {
        return new CurrencyReward(Currency.FOOD, amount);
    }

    private static Reward coins(long amount) {
        return new CurrencyReward(Currency.COIN, amount);
    }

    private static Reward item(String id, ItemKind kind) {
        return new ItemReward(id, kind);
    }
}
