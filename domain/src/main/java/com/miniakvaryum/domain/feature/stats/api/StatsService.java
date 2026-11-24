package com.miniakvaryum.domain.feature.stats.api;

import com.miniakvaryum.domain.feature.aquarium.api.fish.FishData;
import com.miniakvaryum.domain.feature.economy.api.Currency;
import com.miniakvaryum.domain.feature.economy.api.LedgerEntry;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.feature.tasks.api.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Statistics for the profile. Most numbers are read from the existing save (tasks, wallet,
 * fish), so a child who played before this feature existed sees them filled. Only cleanings
 * and days played are counted here.
 */
public final class StatsService {

    private final TaskService tasks;
    private final Wallet wallet;
    private final Supplier<List<FishData>> fish;
    private final Runnable onChange;
    private int cleanings;
    private int daysPlayed;
    private LocalDate lastDayPlayed;

    /**
     * @param cleanings     saved count of cleanings
     * @param daysPlayed    saved count of days
     * @param lastDayPlayed the last day counted, or null
     * @param onChange      asks for a save
     */
    public StatsService(TaskService tasks, Wallet wallet, Supplier<List<FishData>> fish, int cleanings, int daysPlayed,
            LocalDate lastDayPlayed, Runnable onChange) {
        this.tasks = Objects.requireNonNull(tasks, "tasks");
        this.wallet = Objects.requireNonNull(wallet, "wallet");
        this.fish = Objects.requireNonNull(fish, "fish");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.cleanings = Math.max(0, cleanings);
        this.daysPlayed = Math.max(0, daysPlayed);
        this.lastDayPlayed = lastDayPlayed;
    }

    /** The current numbers. */
    public Stats snapshot() {
        List<FishData> all = fish.get();
        long food = all.stream().mapToLong(FishData::feedCount).sum();
        long coins = wallet.entries().stream()
                .filter(e -> e.currency() == Currency.COIN && e.delta() > 0)
                .mapToLong(LedgerEntry::delta).sum();
        return new Stats(tasks.list(TaskStatus.APPROVED).size(), food, coins, cleanings, all.size(),
                all.isEmpty() ? 0 : 1, daysPlayed);
    }

    /** Counts one complete cleaning. */
    public void countCleaning() {
        cleanings++;
        onChange.run();
    }

    /**
     * Counts the day the game is played on; the same day counts once. The game calls this when a
     * session starts, with the local date of the device.
     */
    public void dayPlayed(LocalDate today) {
        Objects.requireNonNull(today, "today");
        if (!today.equals(lastDayPlayed)) {
            lastDayPlayed = today;
            daysPlayed++;
            onChange.run();
        }
    }

    /** For the save. */
    public int savedCleanings() {
        return cleanings;
    }

    /** For the save. */
    public int savedDaysPlayed() {
        return daysPlayed;
    }

    /** For the save; null if no day was counted yet. */
    public LocalDate savedLastDayPlayed() {
        return lastDayPlayed;
    }
}
