package com.miniakvaryum.domain.feature.aquarium;

import com.miniakvaryum.domain.feature.aquarium.api.fish.ActionType;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishContext;
import java.util.EnumMap;
import java.util.Map;

/**
 * How likely each action is in each situation. A fish picks its next action by a weighted
 * random draw over one column of this table:
 * <pre>
 *              NORMAL   FOOD_DETECTED   FOOD_IN_EATING_RANGE
 *  SLOW_MOVE     2           3                  0
 *  DASH          1           2                  0
 *  IDLE          2           1                  1
 *  EAT           0           0                  4
 * </pre>
 * In NORMAL the total is 5, so SLOW_MOVE is chosen in 2 of 5 draws. The draw walks the actions
 * in the order of {@link ActionType#values()}; that order is part of the rules because tests
 * script the random numbers.
 */
public final class ActionWeights {

    private final Map<FishContext, Map<ActionType, Integer>> table = new EnumMap<>(FishContext.class);

    private ActionWeights() {
    }

    /** The table from the specification (shown in the class comment). */
    public static ActionWeights defaults() {
        ActionWeights weights = new ActionWeights();
        weights.column(FishContext.NORMAL, 2, 1, 2, 0);
        weights.column(FishContext.FOOD_DETECTED, 3, 2, 1, 0);
        weights.column(FishContext.FOOD_IN_EATING_RANGE, 0, 0, 1, 4);
        return weights;
    }

    private void column(FishContext context, int slowMove, int dash, int idle, int eat) {
        Map<ActionType, Integer> column = new EnumMap<>(ActionType.class);
        column.put(ActionType.SLOW_MOVE, slowMove);
        column.put(ActionType.DASH, dash);
        column.put(ActionType.IDLE, idle);
        column.put(ActionType.EAT, eat);
        table.put(context, column);
    }

    /** Returns the weight of one action in one context (0 means: never chosen). */
    public int weight(FishContext context, ActionType action) {
        return table.get(context).get(action);
    }

    /** Returns the sum of all weights of a context; the bound for the random draw. */
    public int total(FishContext context) {
        return table.get(context).values().stream().mapToInt(Integer::intValue).sum();
    }
}
