package com.miniakvaryum.game.feature.trophies;

import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.trophies.api.CurrencyReward;
import com.miniakvaryum.domain.feature.trophies.api.ItemKind;
import com.miniakvaryum.domain.feature.trophies.api.ItemReward;
import com.miniakvaryum.domain.feature.trophies.api.Reward;
import com.miniakvaryum.domain.feature.trophies.api.Section;
import java.util.Map;

/** All texts of the trophy road in one place. */
final class TrophyStrings {

    static final String TROPHIES = "Trophies";
    static final String TROPHIES_READY = "Trophies (%d)";
    static final String NEW_TROPHY = "New trophy!";
    static final String TITLE = "Trophy road";
    static final String PROGRESS = "%s  -  %d / %d tasks to the next trophy";
    static final String ALL_REACHED = "All trophies reached! Open your presents.";
    static final String ALL_COLLECTED = "All trophies collected!";
    static final String SECTION = "Part %d\n%s";
    static final String YOU = "You: %d";
    static final String TASKS = "%d tasks";
    static final String ONE_TASK = "1 task";
    static final String MORE_TASKS = "%d more tasks";
    static final String ONE_MORE_TASK = "1 more task";
    static final String TAP_TO_OPEN = "Tap to open!";
    static final String COLLECTED = "Collected";
    static final String BACK = "Back";
    static final String SUPER = "Super!";

    static final String PLUS_FOOD = "+%d food";
    static final String PLUS_COINS = "+%d coins";
    static final String NEW_DECO = "New decoration";
    static final String NEW_ACCESSORY = "New accessory";
    static final String NEW_FISH_PART = "New fish part";
    static final String NEW_FISH = "New fish!";
    static final String FOOD_DETAIL = "Your fish will love it.";
    static final String COINS_DETAIL = "Into your piggy bank.";
    static final String ITEM_DETAIL = "Saved for your aquarium.";
    static final String SET_PROGRESS = "%s set: %d / %d";
    static final String SET_COMPLETE = "%s set complete!";

    private static final Map<Section, String> SECTIONS = Map.of(
            Section.POND, "Pond",
            Section.RIVER, "River",
            Section.LAGOON, "Lagoon",
            Section.REEF, "Reef",
            Section.OCEAN, "Ocean");

    /** Names of the draft item ids of the plan. */
    private static final Map<String, String> ITEMS = Map.ofEntries(
            Map.entry("deco.plant", "Water plant"),
            Map.entry("deco.rock", "Rock"),
            Map.entry("deco.shell", "Shell"),
            Map.entry("deco.chest", "Treasure chest"),
            Map.entry("deco.coral", "Coral"),
            Map.entry("deco.castle", "Big castle"),
            Map.entry("acc.hat", "Hat"),
            Map.entry("acc.glasses", "Glasses"),
            Map.entry("acc.bow", "Bow"),
            Map.entry("acc.scarf", "Scarf"),
            Map.entry("acc.crown", "Crown"),
            Map.entry("fish.clown", "Clownfish"),
            Map.entry("fish.puffer", "Pufferfish"),
            Map.entry("fish.legend", "Legendary fish"));

    private static final Map<String, String> SETS = Map.of(
            "rainbow", "Rainbow",
            "gold", "Gold",
            "crystal", "Crystal");

    private static final Map<String, String> PARTS = Map.of(
            "head", "head",
            "body", "body",
            "fin", "fin",
            "tail", "tail");

    private TrophyStrings() {
    }

    static String section(Section section) {
        return SECTIONS.get(section);
    }

    /** Short text for a tile: "+5 food", "Hat", "Rainbow tail". */
    static String reward(Reward reward) {
        if (reward instanceof CurrencyReward money) {
            return String.format(money.currency() == Currency.FOOD ? PLUS_FOOD : PLUS_COINS, money.amount());
        }
        return itemName(((ItemReward) reward).itemId());
    }

    /** Title line of the reward card: "+5 food" or "New decoration". */
    static String revealTitle(Reward reward) {
        if (reward instanceof CurrencyReward) {
            return reward(reward);
        }
        ItemKind kind = ((ItemReward) reward).kind();
        return switch (kind) {
            case DECO -> NEW_DECO;
            case ACCESSORY -> NEW_ACCESSORY;
            case FISH_PART -> NEW_FISH_PART;
            case FISH -> NEW_FISH;
        };
    }

    /** Name of an item; unknown ids are shown as they are. */
    static String itemName(String itemId) {
        String name = ITEMS.get(itemId);
        if (name != null) {
            return name;
        }
        String[] part = itemId.split("\\.");
        if (part.length == 3 && part[0].equals("part") && SETS.containsKey(part[1]) && PARTS.containsKey(part[2])) {
            return SETS.get(part[1]) + " " + PARTS.get(part[2]);
        }
        return itemId;
    }

    /** "Rainbow" for "part.rainbow.tail", or the raw set id. */
    static String setName(String set) {
        return SETS.getOrDefault(set, set);
    }
}
