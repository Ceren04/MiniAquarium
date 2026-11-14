package com.miniakvaryum.domain.feature.aquarium.api.fish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.util.JavaRandomSource;
import com.miniakvaryum.domain.core.util.Vec2;
import com.miniakvaryum.domain.feature.aquarium.AquariumConfig;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodField;
import com.miniakvaryum.domain.feature.aquarium.api.food.FoodService;
import com.miniakvaryum.domain.feature.aquarium.api.food.PelletState;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.economy.api.Wallet;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for fish behavior (specification section 9.4).
 *
 * <p>How the random numbers work here: {@code game.random} is a scripted source that fails when
 * it runs out of values. A test that scripts nothing therefore also proves that the code under
 * test draws no random numbers. Each test calls {@code ints(...)} and {@code doubles(...)} at
 * most once, listing the values in the order in which the brain draws them.
 *
 * <p>Handy values for the first draw ({@code nextInt}), from the weight table:
 * <pre>
 *   NORMAL (2/1/2/0):          0-1 SLOW_MOVE, 2 DASH, 3-4 IDLE
 *   FOOD_DETECTED (3/2/1/0):   0-2 SLOW_MOVE, 3-4 DASH, 5 IDLE
 *   FOOD_IN_EATING_RANGE (0/0/1/4): 0 IDLE, 1-4 EAT
 * </pre>
 * The pellet always lies at (640, pelletStartY), because that is where {@code drop} puts it; the
 * tests place the fish relative to it.
 */
class FishBrainTest {

    private static final double DT = 1.0 / 60;
    private static final double PELLET_X = 640;
    private static final double TOLERANCE = 1e-9;
    private static final long SEED = 12_345L;
    private static final int DRAWS = 10_000;
    private static final int WEIGHT_TOLERANCE = 300; // 3 % of 10 000 draws

    private final AquariumConfig config = AquariumConfig.defaults();
    private final TestGame game = new TestGame();
    private final FishBrain brain = new FishBrain(config, game.random);
    private final double pelletY = config.food().pelletStartY();

    private FoodField field;
    private FoodService foodService;

    @BeforeEach
    void setUp() {
        GameSession session = game.start(new EconomyFeature(EconomyConfig.defaults()));
        field = new FoodField(config);
        foodService = new FoodService(field, session.get(Wallet.class), session.time(), game.ids, config,
                session.events());
    }

    private static FishEntity fishAt(double x, double y) {
        return new FishEntity(new FishData("fish-1", null, 0, 0), new Vec2(x, y));
    }

    /** A fish exactly {@code distance} below the pellet position. */
    private FishEntity fishBelowPellet(double distance) {
        return fishAt(PELLET_X, pelletY - distance);
    }

    private FishEntity runningFish(ActionType action, double x, double y, Vec2 velocity, double remainingS) {
        FishEntity fish = fishAt(x, y);
        fish.state().setAction(action);
        fish.state().setVelocity(velocity);
        fish.state().setRemainingS(remainingS);
        return fish;
    }

    private String dropPellet() {
        return foodService.drop(PELLET_X).orElseThrow();
    }

    /** Lets {@code draws} fresh fish choose an action and counts what they chose. */
    private Map<ActionType, Integer> countActions(Vec2 fishPosition) {
        FishBrain seededBrain = new FishBrain(config, new JavaRandomSource(SEED));
        Map<ActionType, Integer> counts = new EnumMap<>(ActionType.class);
        for (int i = 0; i < DRAWS; i++) {
            FishEntity fish = fishAt(fishPosition.x(), fishPosition.y());
            seededBrain.update(fish, field, DT);
            counts.merge(fish.state().action(), 1, Integer::sum);
            if (fish.state().claimedPelletId() != null) {
                field.release(fish.state().claimedPelletId(), fish.id());
            }
        }
        return counts;
    }

    // ---------------------------------------------------------------- running an action

    @Test
    void runningActionMovesTheFishAndCountsDown() {
        FishEntity fish = runningFish(ActionType.SLOW_MOVE, 500, 300, new Vec2(40, 0), 3.0);

        FishEvent event = brain.update(fish, field, 0.5);

        assertThat(fish.state().position()).isEqualTo(new Vec2(520, 300));
        assertThat(fish.state().remainingS()).isEqualTo(2.5);
        assertThat(event).isEqualTo(FishEvent.NONE);
    }

    @Test
    void positionIsClampedAtTheWorldEdges() {
        FishEntity right = runningFish(ActionType.DASH, config.world().maxFishX() - 5, 300, new Vec2(160, 0), 1.0);
        FishEntity bottom = runningFish(ActionType.DASH, 500, config.world().minFishY() + 5, new Vec2(0, -160), 1.0);

        brain.update(right, field, 1.0);
        brain.update(bottom, field, 1.0);

        assertThat(right.state().position().x()).isEqualTo(config.world().maxFishX());
        assertThat(bottom.state().position().y()).isEqualTo(config.world().minFishY());
    }

    // ---------------------------------------------------------------- the weighted draw

    @Test
    void weightsAreRespectedInNormalContext() {
        Map<ActionType, Integer> counts = countActions(new Vec2(640, 300)); // no pellet in the water

        assertThat(counts.getOrDefault(ActionType.SLOW_MOVE, 0)).isCloseTo(DRAWS * 2 / 5, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.DASH, 0)).isCloseTo(DRAWS / 5, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.IDLE, 0)).isCloseTo(DRAWS * 2 / 5, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.EAT, 0)).isZero();
    }

    @Test
    void weightsAreRespectedWhenFoodIsDetected() {
        dropPellet();
        double detected = (config.fish().eatRange() + config.fish().detectRange()) / 2;

        Map<ActionType, Integer> counts = countActions(new Vec2(PELLET_X, pelletY - detected));

        assertThat(counts.getOrDefault(ActionType.SLOW_MOVE, 0)).isCloseTo(DRAWS * 3 / 6, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.DASH, 0)).isCloseTo(DRAWS * 2 / 6, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.IDLE, 0)).isCloseTo(DRAWS / 6, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.EAT, 0)).isZero();
    }

    @Test
    void weightsAreRespectedInEatingRange() {
        dropPellet();

        Map<ActionType, Integer> counts = countActions(new Vec2(PELLET_X, pelletY - 30));

        assertThat(counts.getOrDefault(ActionType.IDLE, 0)).isCloseTo(DRAWS / 5, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.EAT, 0)).isCloseTo(DRAWS * 4 / 5, within(WEIGHT_TOLERANCE));
        assertThat(counts.getOrDefault(ActionType.SLOW_MOVE, 0)).isZero();
        assertThat(counts.getOrDefault(ActionType.DASH, 0)).isZero();
    }

    @Test
    void contextSwitchesExactlyAtTheRangeBorders() {
        // Draw 4 means a different action in each context: EAT (eating range), DASH (detected), IDLE (normal).
        game.random.ints(4, 4, 4, 4);
        game.random.doubles(0.5, 0.5); // only the last fish idles: duration and look-around
        String pelletId = dropPellet();
        double eatRange = config.fish().eatRange();
        double detectRange = config.fish().detectRange();

        FishEntity atEatBorder = fishBelowPellet(eatRange);
        brain.update(atEatBorder, field, DT);
        field.release(pelletId, atEatBorder.id());

        FishEntity justOutsideEat = fishBelowPellet(eatRange + 0.5);
        brain.update(justOutsideEat, field, DT);

        FishEntity atDetectBorder = fishBelowPellet(detectRange);
        brain.update(atDetectBorder, field, DT);

        FishEntity justOutsideDetect = fishBelowPellet(detectRange + 0.5);
        brain.update(justOutsideDetect, field, DT);

        assertThat(atEatBorder.state().action()).isEqualTo(ActionType.EAT);
        assertThat(justOutsideEat.state().action()).isEqualTo(ActionType.DASH);
        assertThat(atDetectBorder.state().action()).isEqualTo(ActionType.DASH);
        assertThat(justOutsideDetect.state().action()).isEqualTo(ActionType.IDLE);
    }

    @Test
    void claimedPelletsAreIgnoredWhenChoosing() {
        String pelletId = dropPellet();
        field.tryClaim(pelletId, "someone-else");
        game.random.ints(4); // would be EAT if the claimed pellet counted
        game.random.doubles(0.5, 0.5);
        FishEntity fish = fishBelowPellet(30);

        brain.update(fish, field, DT);

        assertThat(fish.state().action()).isEqualTo(ActionType.IDLE);
        assertThat(fish.state().claimedPelletId()).isNull();
        assertThat(field.pellets().get(0).claimedBy()).isEqualTo("someone-else");
    }

    // ---------------------------------------------------------------- moving

    @Test
    void moveGoesTowardThePelletWhenFoodIsDetected() {
        dropPellet();
        game.random.ints(0); // SLOW_MOVE in the FOOD_DETECTED column
        FishEntity fish = fishAt(440, 520); // 200 left of and 100 below the pellet
        Vec2 toPellet = new Vec2(PELLET_X, pelletY).minus(fish.state().position()).normalized();

        brain.update(fish, field, DT);

        double speed = config.fish().slowSpeed();
        assertThat(fish.state().action()).isEqualTo(ActionType.SLOW_MOVE);
        assertThat(fish.state().velocity().x()).isCloseTo(toPellet.x() * speed, within(TOLERANCE));
        assertThat(fish.state().velocity().y()).isCloseTo(toPellet.y() * speed, within(TOLERANCE));
        assertThat(fish.state().remainingS()).isEqualTo(config.fish().slowDurationS());
    }

    @Test
    void moveInNormalContextUsesTheRandomAngle() {
        game.random.ints(0); // SLOW_MOVE
        game.random.doubles(0.125); // angle = 0.125 * 2 * PI = 45 degrees
        FishEntity fish = fishAt(640, 300);

        brain.update(fish, field, DT);

        double angle = 0.125 * 2 * Math.PI;
        double speed = config.fish().slowSpeed();
        assertThat(fish.state().velocity().x()).isCloseTo(Math.cos(angle) * speed, within(TOLERANCE));
        assertThat(fish.state().velocity().y()).isCloseTo(Math.sin(angle) * speed, within(TOLERANCE));
    }

    @Test
    void directionIsMirroredWhenTheEndPointWouldLeaveTheArea() {
        game.random.ints(2); // DASH
        game.random.doubles(0.0); // angle 0: straight to the right
        FishEntity fish = fishAt(1200, 300); // a dash of 160 units would end far outside

        brain.update(fish, field, DT);

        assertThat(fish.state().velocity().x()).isCloseTo(-config.fish().dashSpeed(), within(TOLERANCE));
        assertThat(fish.state().velocity().y()).isCloseTo(0, within(TOLERANCE));
        assertThat(fish.state().facing()).isEqualTo(-1);
    }

    // ---------------------------------------------------------------- facing

    @Test
    void facingFollowsTheHorizontalDirectionAtActionStart() {
        dropPellet();
        game.random.ints(0, 0); // SLOW_MOVE twice
        FishEntity pelletOnTheLeft = fishAt(840, 520);
        FishEntity pelletOnTheRight = fishAt(440, 520);
        pelletOnTheRight.state().setFacing(-1);

        brain.update(pelletOnTheLeft, field, DT);
        brain.update(pelletOnTheRight, field, DT);

        assertThat(pelletOnTheLeft.state().facing()).isEqualTo(-1);
        assertThat(pelletOnTheRight.state().facing()).isEqualTo(1);
    }

    @Test
    void facingStaysWhenMovingAlmostVertically() {
        game.random.ints(0); // SLOW_MOVE
        game.random.doubles(0.25); // angle 90 degrees: straight up, dir.x is about 0
        FishEntity fish = fishAt(640, 300);
        fish.state().setFacing(-1);

        brain.update(fish, field, DT);

        assertThat(fish.state().velocity().x()).isCloseTo(0, within(TOLERANCE));
        assertThat(fish.state().facing()).isEqualTo(-1);
    }

    @Test
    void facingNeverChangesWhileAnActionRuns() {
        // Moving left while facing right: only the start of an action may change the facing.
        FishEntity fish = runningFish(ActionType.SLOW_MOVE, 640, 300, new Vec2(-40, 0), 3.0);

        for (int i = 0; i < 5; i++) {
            brain.update(fish, field, 0.2);
            assertThat(fish.state().facing()).isEqualTo(1);
        }
    }

    // ---------------------------------------------------------------- idling

    @Test
    void idleDurationIsBetweenMinAndMax() {
        game.random.ints(3, 3, 3); // IDLE in the NORMAL column
        game.random.doubles(0.0, 0.5, 0.5, 0.5, 0.999, 0.5); // duration and look-around, per fish
        double min = config.fish().idleMinS();
        double max = config.fish().idleMaxS();
        FishEntity shortest = fishAt(640, 300);
        FishEntity middle = fishAt(640, 300);
        FishEntity longest = fishAt(640, 300);

        brain.update(shortest, field, DT);
        brain.update(middle, field, DT);
        brain.update(longest, field, DT);

        assertThat(shortest.state().action()).isEqualTo(ActionType.IDLE);
        assertThat(shortest.state().velocity()).isEqualTo(Vec2.ZERO);
        assertThat(shortest.state().remainingS()).isEqualTo(min);
        assertThat(middle.state().remainingS()).isEqualTo(min + 0.5 * (max - min));
        assertThat(longest.state().remainingS()).isBetween(min, max);
    }

    @Test
    void idleFlipsFacingWithThirtyPercentChance() {
        game.random.ints(3, 3);
        game.random.doubles(0.5, 0.29, 0.5, 0.30); // 0.29 flips, 0.30 does not
        FishEntity flips = fishAt(640, 300);
        FishEntity stays = fishAt(640, 300);

        brain.update(flips, field, DT);
        brain.update(stays, field, DT);

        assertThat(flips.state().facing()).isEqualTo(-1);
        assertThat(stays.state().facing()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- eating

    @Test
    void eatClaimsThePelletAndFacesIt() {
        String pelletId = dropPellet();
        game.random.ints(4); // EAT in the FOOD_IN_EATING_RANGE column
        FishEntity fish = fishAt(PELLET_X + 60, pelletY - 20); // about 63 units away, pellet on its left

        brain.update(fish, field, DT);

        assertThat(fish.state().action()).isEqualTo(ActionType.EAT);
        assertThat(fish.state().claimedPelletId()).isEqualTo(pelletId);
        assertThat(fish.state().velocity()).isEqualTo(Vec2.ZERO);
        assertThat(fish.state().remainingS()).isEqualTo(config.fish().eatDurationS());
        assertThat(fish.state().facing()).isEqualTo(-1);
        assertThat(field.pellets().get(0).state()).isEqualTo(PelletState.CLAIMED);
        assertThat(field.pellets().get(0).claimedBy()).isEqualTo(fish.id());
    }

    @Disabled("Cannot be reached with the real classes: see the note in the test")
    @Test
    void failedClaimFallsBackToIdle() {
        // The brain asks nearestFree for a FREE pellet and claims it in the very next call.
        // In single-threaded code nothing can happen in between, so tryClaim cannot fail there.
        // Testing this branch needs a seam (for example FoodField behind an interface) or a
        // deliberately inconsistent field with two pellets of the same id. Decide as a team.
    }

    @Test
    void finishedEatingIsReturnedExactlyOnce() {
        game.random.ints(3); // after eating the fish idles (NORMAL: no pellet left)
        game.random.doubles(0.5, 0.5);
        FishEntity fish = runningFish(ActionType.EAT, 640, 300, Vec2.ZERO, 0.1);

        FishEvent first = brain.update(fish, field, 0.2);
        FishEvent second = brain.update(fish, field, DT);

        assertThat(first).isEqualTo(FishEvent.FINISHED_EATING);
        assertThat(second).isEqualTo(FishEvent.NONE);
        assertThat(fish.state().action()).isEqualTo(ActionType.IDLE);
    }
}