package com.miniakvaryum.domain.feature.trophies.api;

import com.miniakvaryum.domain.core.events.EventBus;
import com.miniakvaryum.domain.feature.economy.api.TxResult;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongSupplier;

/**
 * The trophy road ("Pokalstrasse"): progress, trophy states and collecting.
 *
 * <p>Progress is the number of different tasks a parent approved; it never goes down and does not
 * depend on time, so moving the clock changes nothing. Reaching a trophy does not pay it: the
 * trophy becomes {@link TrophyState#READY} and waits until the child taps it
 * ({@link #claim(int)}). Every reward is given exactly once.
 */
public final class TrophyService {

    private final List<Milestone> milestones;
    private final Wallet wallet;
    private final LongSupplier nowMillis;
    private final EventBus events;
    private final Runnable onChange;
    private final Set<String> countedTaskIds;
    private final Set<Integer> claimed;

    /**
     * @param milestones     the road, sorted by threshold
     * @param countedTaskIds saved ids of the tasks already counted
     * @param claimed        saved thresholds of the collected trophies
     * @param onChange       called after every change (save request)
     */
    public TrophyService(List<Milestone> milestones, Wallet wallet, LongSupplier nowMillis, EventBus events,
            Collection<String> countedTaskIds, Collection<Integer> claimed, Runnable onChange) {
        this.milestones = List.copyOf(Objects.requireNonNull(milestones, "milestones"));
        this.wallet = Objects.requireNonNull(wallet, "wallet");
        this.nowMillis = Objects.requireNonNull(nowMillis, "nowMillis");
        this.events = Objects.requireNonNull(events, "events");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.countedTaskIds = new LinkedHashSet<>(countedTaskIds);
        this.claimed = new TreeSet<>(claimed);
    }

    /** Number of approved tasks counted so far. */
    public int approvedCount() {
        return countedTaskIds.size();
    }

    /** All trophies of the road, sorted by threshold. */
    public List<Milestone> milestones() {
        return milestones;
    }

    /**
     * State of the trophy with this threshold.
     *
     * @throws IllegalArgumentException if no trophy has this threshold
     */
    public TrophyState stateOf(int threshold) {
        at(threshold);
        if (claimed.contains(threshold)) {
            return TrophyState.CLAIMED;
        }
        return approvedCount() >= threshold ? TrophyState.READY : TrophyState.LOCKED;
    }

    /** Number of trophies waiting to be collected; shown on the "Trophies (n)" button. */
    public int readyCount() {
        return (int) milestones.stream().filter(m -> stateOf(m.threshold()) == TrophyState.READY).count();
    }

    /** Number of collected trophies of the road, for "Trophies 4 / 39". */
    public int claimedCount() {
        return (int) milestones.stream().filter(m -> claimed.contains(m.threshold())).count();
    }

    /** The next trophy that still needs tasks, or empty when the whole road is reached. */
    public Optional<Milestone> nextMilestone() {
        int count = approvedCount();
        return milestones.stream().filter(m -> m.threshold() > count).findFirst();
    }

    /**
     * Counts an approved task. The same task counts once. Publishes {@link TrophyUnlocked} for
     * every trophy this task reaches.
     *
     * @return true if the task was new
     */
    public boolean countApproved(String taskId) {
        Objects.requireNonNull(taskId, "taskId");
        int before = approvedCount();
        if (!countedTaskIds.add(taskId)) {
            return false;
        }
        int after = approvedCount();
        for (Milestone m : milestones) {
            if (m.threshold() > before && m.threshold() <= after && !claimed.contains(m.threshold())) {
                events.publish(new TrophyUnlocked(m));
            }
        }
        onChange.run();
        return true;
    }

    /**
     * Collects a trophy and gives its reward.
     *
     * <ol>
     *   <li>Not {@link TrophyState#READY}: returns {@link ClaimResult#NOT_READY} or
     *       {@link ClaimResult#ALREADY_CLAIMED}; nothing changes.</li>
     *   <li>Coins or food are credited with refId {@code "trophy:<threshold>"}, reason
     *       {@code "trophy"}. {@code ALREADY_APPLIED} also counts as success: the payment was made
     *       before a crash, so it is not made twice.</li>
     *   <li>Items are only remembered ({@link #earnedItems()}); the wallet does not change.</li>
     *   <li>Marks the trophy as collected, publishes {@link TrophyClaimed}, requests a save.</li>
     * </ol>
     *
     * @throws IllegalArgumentException if no trophy has this threshold
     */
    public ClaimResult claim(int threshold) {
        TrophyState state = stateOf(threshold);
        if (state == TrophyState.LOCKED) {
            return ClaimResult.NOT_READY;
        }
        if (state == TrophyState.CLAIMED) {
            return ClaimResult.ALREADY_CLAIMED;
        }
        Milestone milestone = at(threshold);
        if (milestone.reward() instanceof CurrencyReward money) {
            TxResult tx = wallet.credit(money.currency(), money.amount(), "trophy", "trophy:" + threshold,
                    nowMillis.getAsLong());
            if (tx != TxResult.OK && tx != TxResult.ALREADY_APPLIED) {
                throw new IllegalStateException("trophy " + threshold + ": " + tx);
            }
        }
        claimed.add(threshold);
        events.publish(new TrophyClaimed(milestone));
        onChange.run();
        return ClaimResult.OK;
    }

    /** The things from collected trophies, sorted by threshold. The inventory (V11) takes them from here. */
    public List<ItemReward> earnedItems() {
        List<ItemReward> items = new ArrayList<>();
        for (Milestone m : milestones) {
            if (claimed.contains(m.threshold()) && m.reward() instanceof ItemReward item) {
                items.add(item);
            }
        }
        return items;
    }

    /** For the save: ids of the counted tasks, in counting order. */
    public List<String> savedCountedTaskIds() {
        return List.copyOf(countedTaskIds);
    }

    /** For the save: thresholds of the collected trophies, sorted. */
    public List<Integer> savedClaimed() {
        return List.copyOf(claimed);
    }

    private Milestone at(int threshold) {
        for (Milestone m : milestones) {
            if (m.threshold() == threshold) {
                return m;
            }
        }
        throw new IllegalArgumentException("no trophy at " + threshold + " tasks");
    }
}
