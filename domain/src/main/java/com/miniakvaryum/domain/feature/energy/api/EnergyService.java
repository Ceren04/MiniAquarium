package com.miniakvaryum.domain.feature.energy.api;

import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.energy.EnergyConfig;
import com.miniakvaryum.domain.feature.energy.FishLevels;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Energy from 0 to 100: it fills up with time (also while the app is closed, at most 24 hours)
 * and is spent on minigames. It is never earned in any other way.
 *
 * <p>Per hour: {@code base} + for every active fish ({@code perFish} + {@code perLevel} x level).
 * With today's start (3 fish, level 1) that is 10 per hour, so one game (20) every 2 hours.
 *
 * <p>The value is kept with decimals, so the small gains of single frames are not lost; the
 * screen shows the whole number ({@link #energy()}).
 */
public final class EnergyService {

    /** How many spent refIds are remembered; older ones are forgotten so the save stays small. */
    public static final int REMEMBERED_REF_IDS = 50;

    private static final double MILLIS_PER_HOUR = 3_600_000.0;
    private static final double MINUTES_PER_HOUR = 60.0;

    private final EnergyConfig config;
    private final Supplier<List<FishData>> activeFish;
    private final FishLevels levels;
    private final Runnable onChange;
    private final Deque<String> spentRefIds;
    private double value;

    /**
     * @param value       saved energy (or the start value of a new game)
     * @param spentRefIds refIds already spent, oldest first
     * @param config      rules
     * @param activeFish  the fish swimming in the aquarium now
     * @param levels      level of a fish
     * @param onChange    asks for a save
     */
    public EnergyService(double value, Collection<String> spentRefIds, EnergyConfig config,
            Supplier<List<FishData>> activeFish, FishLevels levels, Runnable onChange) {
        this.config = Objects.requireNonNull(config, "config");
        this.activeFish = Objects.requireNonNull(activeFish, "activeFish");
        this.levels = Objects.requireNonNull(levels, "levels");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.spentRefIds = new ArrayDeque<>(spentRefIds);
        this.value = clamp(value);
    }

    /** Energy as shown on screen: the whole number, 0 to {@link #max()}. */
    public int energy() {
        return (int) Math.floor(value);
    }

    /** Energy with decimals, for the save. */
    public double exactValue() {
        return value;
    }

    /** Highest energy. */
    public int max() {
        return (int) config.max();
    }

    /** Energy one minigame costs. */
    public int gameCost() {
        return config.gameCost();
    }

    /** Energy gained per hour with the fish in the aquarium now. */
    public double ratePerHour() {
        double rate = config.base();
        for (FishData fish : activeFish.get()) {
            rate += config.perFish() + config.perLevel() * levels.levelOf(fish);
        }
        return rate;
    }

    /** Lets time pass: adds the energy of {@code millis} milliseconds at the current rate. */
    public void advance(long millis) {
        if (millis > 0) {
            add(ratePerHour() * millis / MILLIS_PER_HOUR);
        }
    }

    /** Like {@link #advance(long)} for a frame time in seconds. */
    public void advanceSeconds(double seconds) {
        if (seconds > 0) {
            add(ratePerHour() * seconds * 1_000.0 / MILLIS_PER_HOUR);
        }
    }

    private void add(double amount) {
        value = clamp(value + amount);
    }

    /**
     * Spends energy, for example 20 when a minigame starts.
     *
     * @param amount energy to take, at least 1
     * @param refId  id of the thing paid for (for example {@code "memory:<game id>"}); the same
     *               refId is never paid twice
     */
    public SpendResult spend(int amount, String refId) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be >= 1 but was " + amount);
        }
        Objects.requireNonNull(refId, "refId");
        if (spentRefIds.contains(refId)) {
            return SpendResult.ALREADY_APPLIED;
        }
        if (energy() < amount) {
            return SpendResult.NOT_ENOUGH;
        }
        value -= amount;
        spentRefIds.addLast(refId);
        while (spentRefIds.size() > REMEMBERED_REF_IDS) {
            spentRefIds.removeFirst();
        }
        onChange.run();
        return SpendResult.OK;
    }

    /** Returns true if there is enough energy for {@code amount}. */
    public boolean canSpend(int amount) {
        return energy() >= amount;
    }

    /**
     * Minutes until the energy reaches {@code amount} at the current rate, rounded up; 0 if it
     * is already there. Used for texts like "More energy in 25 min".
     */
    public int minutesUntil(int amount) {
        double missing = Math.min(amount, config.max()) - value;
        if (missing <= 0) {
            return 0;
        }
        double rate = ratePerHour();
        if (rate <= 0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.ceil(missing / rate * MINUTES_PER_HOUR);
    }

    /** The remembered spent refIds, oldest first, for the save. */
    public List<String> spentRefIds() {
        return List.copyOf(spentRefIds);
    }

    /**
     * Sets the energy directly. <b>Developer tools only</b> ("Energy 0" / "Energy 100" in the
     * developer bar); the game itself never calls this.
     */
    public void setForDevelopment(double newValue) {
        value = clamp(newValue);
        onChange.run();
    }

    private double clamp(double v) {
        if (Double.isNaN(v)) {
            return 0;
        }
        return Math.max(0, Math.min(config.max(), v));
    }
}
