package com.miniakvaryum.domain.feature.aquarium.api.food;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.core.time.GameTime;
import com.miniakvaryum.domain.core.util.IdGenerator;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.WorldConfig;
import com.miniakvaryum.domain.feature.aquarium.api.fish.FishEntity;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
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
 * <p>Tests: {@code FoodServiceTest}.
 */
public final class FoodService {

    private static final String REASON = "pellet";
    private static final String REF_PREFIX = "pellet:";

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

    /** Returns how many more pellets may be dropped: FOOD balance minus all pellets in the water. */
    public long available() {
        return wallet.balance(Currency.FOOD) - field.pellets().size();
    }

    /**
     * Drops a pellet into the water at the given x. Dropping does NOT change the wallet.
     *
     * @return the id of the new pellet, or empty if no food is available
     */
    public Optional<String> drop(double x) {
        if (!canDrop()) {
            return Optional.empty();
        }
        WorldConfig world = config.world();
        double clampedX = Math.max(world.minFishX(), Math.min(world.maxFishX(), x));
        String id = ids.next();
        field.add(id, new Vec2(clampedX, config.food().pelletStartY()));
        return Optional.of(id);
    }

    /**
     * A fish has finished its EAT action. This is one all-or-nothing step: claim check, then
     * payment, then the changes. Calling it twice for the same pellet pays once, because the
     * first call removes the pellet and the second call finds no claim.
     */
    public EatResult completeEating(FishEntity fish, String pelletId) {
        if (!isClaimedBy(pelletId, fish.id())) {
            return EatResult.CLAIM_LOST;
        }

        TxResult payment = wallet.debit(Currency.FOOD, 1, REASON, REF_PREFIX + pelletId, time.nowMillis());
        if (payment != TxResult.OK) {
            field.release(pelletId, fish.id());
            return EatResult.INSUFFICIENT_FOOD;
        }

        fish.recordFeeding();
        field.remove(pelletId);
        events.publish(new FishFed(fish.id(), fish.feedCount()));
        return EatResult.OK;
    }

    private boolean isClaimedBy(String pelletId, String fishId) {
        for (Pellet pellet : field.pellets()) {
            if (pellet.id().equals(pelletId)) {
                return pellet.state() == PelletState.CLAIMED && fishId.equals(pellet.claimedBy());
            }
        }
        return false;
    }
    
    private boolean canDrop() {
    return available() > 0
            && field.pellets().size() < config.food().maxPelletsInWater();
    }
}