package com.miniakvaryum.domain.feature.stats.api;

/**
 * The numbers on the profile ("My stats"). All of them only ever grow.
 *
 * @param tasksDone    tasks the parents approved
 * @param foodGiven    pellets eaten by the fish
 * @param coinsEarned  coins received (spending does not lower it)
 * @param cleanings    times the aquarium was cleaned completely, counted since this feature exists
 * @param fishCount    fish in the aquarium
 * @param bestLevel    highest fish level (1 until fish levels exist, V10)
 * @param daysPlayed   different days on which the game was opened
 */
public record Stats(int tasksDone, long foodGiven, long coinsEarned, int cleanings, int fishCount, int bestLevel,
        int daysPlayed) {
}
