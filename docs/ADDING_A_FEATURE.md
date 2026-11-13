# Working on features

Two situations are covered here: **finishing a skeleton** that exists already, and **adding a
new feature** from nothing. Read the reference example first: `feature/economy` in the domain
(`EconomyFeature`, `Wallet`, `WalletTest`, `EconomyFeatureTest`) and
`feature/economy/EconomyGameFeature` in the game module.

## A. Finishing a skeleton

1. Create a branch: `git checkout -b feature/<name>`.
2. Open the test class of your part (for example `TaskServiceTest`) and delete the line
   `@Disabled("TODO: ...")` above the class. Run `gradlew test`: every test fails with
   "TODO: write this test". That list is your to-do list.
3. For one test at a time: write the test, run it (red), implement the method until it is
   green. The Javadoc above each `TODO` method says step by step what it must do.
4. When all tests of the class are green, add your package to `coverageGatedPackages` in
   `domain/build.gradle`, for example `'com/miniakvaryum/domain/feature/tasks/**'`.
5. `gradlew build` must be green. Start the game and try your part by hand.
6. Open a pull request. Keep it small: one class with its tests is a good size.

You should not need to change any file outside your feature's packages, apart from that one
line in `domain/build.gradle`. If you think you must, ask in the team first.

## B. Adding a new feature

Example: an **energy** feature (energy from 0 to 100 that refills over time).

### 1. Rules (domain module)

Create the package `domain/feature/energy` with this content:

```
feature/energy/
  api/EnergyService.java     the service other code may use
  EnergyConfig.java          the tunable numbers
  EnergyFeature.java         the connection to the game
```

`EnergyConfig.java`: every number that someone might want to tune, with the defaults.

```java
public record EnergyConfig(int max, double refillPerHour) {
    public static EnergyConfig defaults() {
        return new EnergyConfig(100, 10);
    }
}
```

`api/EnergyService.java`: the rules. Plain Java, no core classes needed.

```java
public final class EnergyService {
    private final EnergyConfig config;
    private double energy;

    public EnergyService(double savedEnergy, EnergyConfig config) {
        this.config = config;
        this.energy = savedEnergy;
    }

    public int energy() {
        return (int) energy;
    }

    /** Refills for the given time, never above the maximum. */
    public void refill(double hours) {
        energy = Math.min(config.max(), energy + hours * config.refillPerHour());
    }
}
```

`EnergyFeature.java`: copy this shape.

```java
public final class EnergyFeature implements DomainFeature {

    public static final String ID = "energy";      // key in the save file; never change it
    private static final int SECTION_VERSION = 1;

    private final EnergyConfig config;
    private EnergyService service;

    public EnergyFeature(EnergyConfig config) {
        this.config = config;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void install(FeatureContext ctx) {
        // 1. Saved state, or the default for a new game.
        double saved = ctx.savedSection().map(s -> s.data().getAsDouble()).orElse(0.0);
        service = new EnergyService(saved, config);
        // 2. Things that happened while the app was closed.
        service.refill(ctx.creditedGap().millis() / 3_600_000.0);
        // 3. Let others use it.
        ctx.provide(EnergyService.class, service);
    }

    @Override
    public void tick(double dtSeconds) {
        service.refill(dtSeconds / 3600.0);
    }

    @Override
    public Optional<SaveSection> save() {
        return Optional.of(new SaveSection(SECTION_VERSION, new JsonPrimitive((double) service.energy())));
    }
}
```

Switch it on with one line in `DomainFeatures.java`:

```java
new EnergyFeature(EnergyConfig.defaults())
```

Order matters there: a feature can only `require` services of features above it.

### 2. Tests

Put them under `domain/src/test/java/...` in the same package. `TestGame` gives you a complete
fake device:

```java
class EnergyFeatureTest {
    private final TestGame game = new TestGame();

    @Test
    void energyRefillsWhileTheAppIsClosed() {
        GameSession session = game.start(new EnergyFeature(EnergyConfig.defaults()));
        session.saveNow();

        game.clock.advanceMillis(2 * 3_600_000L);                 // two hours pass
        GameSession later = game.start(new EnergyFeature(EnergyConfig.defaults()));   // "reopen the app"

        assertThat(later.get(EnergyService.class).energy()).isEqualTo(20);
    }
}
```

Useful helpers in `testsupport` and next to the features:

| Helper | Use |
|---|---|
| `TestGame` | start a session with chosen features; call `start` again to simulate a restart |
| `game.clock.advanceMillis(..)` / `shiftWallMillis(..)` | let time pass / change the device date |
| `game.random.ints(..).doubles(..)` | script the "random" numbers |
| `game.store.saveCount()` / `lastSaved()` | check that and what was saved |
| `ParentSessions.forceUnlock(parent)` | open the parent area without a PIN |
| `Fishes.at(..)`, `Fishes.eating(..)` | build a fish in an exact state |

Never use `Thread.sleep`, the real clock or real randomness in a test.

### 3. UI (game module)

Create the package `game/feature/energy`:

```java
public final class EnergyGameFeature implements GameFeature {

    @Override
    public String id() {
        return "energy";
    }

    @Override
    public void install(UiContext ui) {
        EnergyService energy = ui.session().get(EnergyService.class);
        ui.main().addStatus(() -> EnergyStrings.ENERGY + energy.energy());
    }
}
```

User-visible texts go into `EnergyStrings` in the same package. Switch the UI on with one line in
`GameFeatures.java`.

What `ui` offers:

| Call | Effect |
|---|---|
| `ui.session().get(X.class)` | a domain service |
| `ui.main().addStatus(supplier)` | text in the top bar, refreshed every frame |
| `ui.main().addButton(label, action)` / `addToggle(label, onToggle)` | button in the top bar |
| `ui.main().addWorldLayer(z, actor)` | something drawn in the aquarium; lower z is further back (water -100, sand -90, pellets 0, fish 10) |
| `ui.main().addWorldTapHandler(handler)` | react to taps into the water |
| `ui.main().addDebugLine(supplier)` | a line in the F3 overlay |
| `ui.main().showMessage(text, seconds)` | short message on the main screen |
| `ui.nav().register(id, () -> new MyScreen(ui))` / `ui.nav().show(id)` | own screens |
| `ui.shapes()` | placeholder images, or a file from `assets` if it exists |

For a screen of your own, extend `BaseScreen` and build it with Scene2D tables; use the
inherited `button(text, action)` so buttons are big enough and skeleton code cannot crash the
game.

### 4. Finish

Add your packages to `coverageGatedPackages`, run `gradlew build`, try it in the game, open a
pull request.

## Recipes

**React to something another feature does.** Subscribe to its event in `install`:

```java
ctx.events().subscribe(TaskApproved.class, event -> counter.increment());
```

If the event you need does not exist, ask the owner of that feature to add a record to its
`api` package and publish it. Do not edit their service yourself.

**Store extra data about fish (mood, name, accessory).** Do not add fields to `FishData`.
Keep a map from fish id to your data in your own feature and save it in your own section.

**Make sure a change is saved.** Call `ctx.requestSave()` after it (pass `ctx::requestSave`
into your service as a `Runnable`). Several requests in one frame cause one save.

**Change the format of your save section.** Raise `SECTION_VERSION` by one and register a step
that converts the old JSON into the new one:

```java
new SaveMigrator(2).step(1, json -> convertVersion1To2(json)).migrate(section.version(), section.data());
```

Add a test that loads a version 1 section. Never change the meaning of an existing version.

**Pay or charge something exactly once.** Give the wallet transaction a `refId` that names its
cause (`"task:<id>:coin"`, `"buy:<purchaseId>"`). The wallet ignores a second transaction with
the same refId and currency.

## Do and do not

| Do | Do not |
|---|---|
| Read time from `ctx.time().nowMillis()` | `System.currentTimeMillis()`, `LocalDateTime.now()` |
| Draw random numbers from `ctx.random()` | `Math.random()`, `new Random()` |
| Put numbers into your `...Config` record | magic numbers in the code |
| Put user-visible text into your `...Strings` class (game module) | user-visible text in the domain or inline in screens |
| Change domain state through services | set fields of domain objects from the UI |
| Import another feature's `api` package | import its other packages, or another UI feature |
| Save facts (ledger entries, counters) | save values that can be recalculated, or positions |
| Throw `IllegalArgumentException` / `IllegalStateException` for wrong calls, return an enum for expected outcomes | swallow exceptions or return null |

`ArchitectureTest` and `GameArchitectureTest` check several of these and explain what to do
when they fail.

## Git workflow

- `main` is protected: changes arrive only through pull requests with one approval and a green build.
- One branch per piece of work: `feature/<name>`. Pull `main` into it often.
- The only shared files are the two feature lists and `coverageGatedPackages`; everybody adds
  single lines there, so conflicts are trivial to resolve (keep both lines).
- Record every deviation from the specification as one line in `docs/DECISIONS.md`.
