package com.miniakvaryum.domain.feature.aquarium.api.food;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.core.util.IdGenerator;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import java.util.Objects;
import java.util.Optional;

/**
 * Connects the pellets in the water with the food stock in the wallet.
 *
 * <p>Key rule: <b>dropping a pellet costs nothing; food is deducted only when a fish has
 * finished eating it.</b> A pellet that dissolves uneaten is free. To stop the child from
 * dropping more pellets than there is food, pellets in the water count as reserved.
 *
 * <p><b>SKELETON</b> (specification section 7.8). Tests: {@code FoodServiceTest}.
 */
public final class FoodService {

    private final FoodField field;
    private final Wallet wallet;
    private final GameTime time;
    private final IdGenerator ids;
    private final AquariumConfig config;
    private final EventBus events;

    /**
     * @param field  pellets in the water
     * @param wallet holds the FOOD balance
     * @param time   trusted game time, used as the time of the food transactions
     * @param ids    creates pellet ids
     * @param config pellet start height and world size
     * @param events where {@link FishFed} is published
     */
    public FoodService(FoodField field, Wallet wallet, GameTime time, IdGenerator ids,
            AquariumConfig config, EventBus events) {
        this.field = Objects.requireNonNull(field, "field");
        this.wallet = Objects.requireNonNull(wallet, "wallet");
        this.time = Objects.requireNonNull(time, "time");
        this.ids = Objects.requireNonNull(ids, "ids");
        this.config = Objects.requireNonNull(config, "config");
        this.events = Objects.requireNonNull(events, "events");
    }

    /**
     * Returns how many more pellets may be dropped.
     *
     * <p>TODO: FOOD balance minus the number of pellets in the water (FREE and CLAIMED).
     */
    public long available() {
        throw new UnsupportedOperationException("TODO: FoodService.available is not implemented yet");
    }

    /**
     * Drops a pellet into the water at the given x.
     *
     * <p>TODO: if {@code available() <= 0} return empty. Otherwise clamp x into
     * {@code [minFishX, maxFishX]}, add a FREE pellet at {@code (x, pelletStartY)} with a new
     * id and return that id. Dropping does NOT change the wallet.
     *
     * @return the id of the new pellet, or empty if no food is available
     */
    public Optional<String> drop(double x) {
        throw new UnsupportedOperationException("TODO: FoodService.drop is not implemented yet");
    }

    /**
     * A fish has finished its EAT action. This is one all-or-nothing step.
     *
     * <p>TODO:
     * <ol>
     *   <li>The pellet must exist and be CLAIMED by this fish; otherwise return
     *       {@link EatResult#CLAIM_LOST} and change nothing.</li>
     *   <li>{@code wallet.debit(FOOD, 1, "pellet", "pellet:" + pelletId, time.nowMillis())}.
     *       If the result is not OK: release the pellet and return
     *       {@link EatResult#INSUFFICIENT_FOOD}.</li>
     *   <li>{@code fish.recordFeeding()}, remove the pellet, publish {@link FishFed},
     *       return {@link EatResult#OK}.</li>
     * </ol>
     * Calling it twice for the same pellet must pay once: the second call finds no pellet and
     * returns CLAIM_LOST.
     */
    public EatResult completeEating(FishEntity fish, String pelletId) {
        throw new UnsupportedOperationException("TODO: FoodService.completeEating is not implemented yet");
    }
}
