# Mini-Akvaryum: Technical Specification for the Coding Agent

*Java, desktop (laptop) first · Version 2.0 · Supersedes version 1.0 · Audience: an AI coding agent, the six team members, and the human reviewers of their output*

**What this version is.** Version 1.0 described a project that one agent would build from nothing, step by step. Since then the base of the project has been built, and its structure was changed so that six people can work on it at the same time. Version 2.0 describes the project **as it exists now**: what is finished, what is left to write, how to add to it, and which decisions were taken and why. Where this document and version 1.0 disagree, this document wins; every difference is listed with its reason in section 18.

The rules of the game itself (numbers, algorithms, state machines) are unchanged from version 1.0.

## 0. Rules for the agent

1. **Stay inside your feature.** You write and change files only in the packages of the feature you were given (`domain/.../feature/<name>`, `game/.../feature/<name>`) and in their tests. The only shared files you may touch are listed in section 14.5, and you add single lines there. If a task seems to need a change in `core` or in another feature, stop and report it instead of making the change.
2. **Implement exactly what is specified.** Do not add features, libraries or modules that are not listed. If a detail is missing, use the stated default and record the decision in `docs/DECISIONS.md` (one table row per decision).
3. **One work package is one branch and one or more small pull requests** (section 15). After every pull request `./gradlew build` MUST pass, including all tests, the architecture tests and the coverage gate.
4. **Tests first or together with the code.** A method is not finished while its listed tests are disabled or missing.
5. The `domain` module MUST NOT import `com.badlogic.gdx`, `java.awt` or `javax.swing`. It must run in a plain JVM.
6. **All time comes from `GameTime`. All randomness comes from `RandomSource`.** Never call `System.currentTimeMillis()`, `System.nanoTime()`, `Math.random()` or `new Random()` outside the classes that implement those interfaces (`SystemClock`, `JavaRandomSource`).
7. **Everything is in English**: identifiers, comments, commit messages, and the text the player sees. User-visible text lives only in the `...Strings` classes of the `game` module, one class per feature.
8. Every public class and interface in `domain` has Javadoc. No mutable static state. No singletons.
9. Everything is single-threaded (the libGDX render thread). Do not create threads.
10. Domain failures use enums or result records, or an unchecked exception for programming errors (violated preconditions). Never swallow exceptions silently. Use `java.util.logging` for logs.
11. **Tunable numbers live in the config record of the feature.** No magic numbers elsewhere.
12. **Use another feature only through its `api` package or its events.** Never import anything else from it.

## 1. The product in one paragraph

Mini-Akvaryum is a game for children aged 7 to 11. Parents create real-life tasks in a PIN-protected parent area. The child reports a task as done, and the parent approves it after checking in real life. An approval pays coins and fish food, once. The child feeds fish in a virtual aquarium. Fish swim autonomously by weighted random actions and grow through feeding. The game keeps its own trusted clock so that changing the device date gives no advantage. Everything works offline.

## 2. Scope and current state

### 2.1 What the base contains

| Part | State |
|---|---|
| Gradle project with modules `domain`, `game`, `desktop`; version catalog; CI workflow; VS Code files | Complete |
| Domain core: `util`, `time`, `save`, `events`, `module`, `session` | Complete, tested |
| Feature `economy` (wallet with ledger) | Complete, tested. **The reference example of a feature.** |
| Features `security`, `tasks`, `aquarium` | **Skeleton**: wiring, data types, configs and save format complete; rule methods are `TODO` |
| UI core: main screen frame, navigation, skin, placeholder shapes | Complete |
| UI of `economy` (balances) and feed mode, pellet drawing | Complete |
| UI of `security`, `tasks`; four-part fish drawing | **Skeleton** |
| Architecture tests for `domain` and `game`; coverage gate | Complete |

Numbers at the time of writing: 137 tests run and pass; 88 further tests exist by name and are disabled until their feature is written; 20 domain methods are `TODO`.

The game starts, shows the aquarium with three fish that do not move yet, saves and loads, and all four features are installed. Calling unfinished code from the UI shows "Not implemented yet" instead of crashing.

### 2.2 What "skeleton" means

- The `...Feature` class is complete: it reads its save section, builds its services, provides them and saves.
- Data types, enums, config records and simple getters are complete.
- Every rule method has its final signature and a Javadoc that says step by step what it must do, and its body is `throw new UnsupportedOperationException("TODO: <Class>.<method> is not implemented yet")`.
- One exception: `AquariumWorld.update` does nothing instead of throwing, because it runs every frame.
- Every required test exists by name in a test class annotated `@Disabled`, with a body `fail("TODO: write this test")`.

### 2.3 Out of scope (do not build)

Android build, shop, rollback snapshots, energy, mood, sleep at 21:00, minigame, trophy road, profile, music or sound, bubbles on tap, fish names, recurring tasks, backend or any network code, real artwork, changing or recovering the PIN.

These are later features. The structure is prepared for them (section 14); do not add placeholders for them.

## 3. Technology

| Item | Decision |
|---|---|
| Language | Java 17, enforced with `options.release = 17` in the root `build.gradle`. Any JDK 17 or newer works. |
| Build | Gradle wrapper 8.14.3, Groovy build files |
| Game framework | libGDX 1.14.2, LWJGL3 desktop backend |
| Versions | All pinned in `gradle/libs.versions.toml`. No snapshot versions. |
| Tests | JUnit Jupiter 5.11.4, AssertJ 3.27.3 (tests only) |
| JSON | Gson 2.11.0 (allowed in `domain`) |
| Coverage | JaCoCo; build fails below 70 % line coverage of the packages listed in `coverageGatedPackages` (`domain/build.gradle`) |
| Other libraries | None. Hashing and crypto use the JDK (`javax.crypto`, `java.security`). |
| IDE | VS Code; `.vscode/launch.json` starts `com.miniakvaryum.desktop.DesktopLauncher` with working directory `${workspaceFolder}/assets` |

The desktop `run` task sets the working directory to the root `assets` folder.

**Environment notes.** The project path must not contain non-ASCII letters and should not lie inside a synchronized folder such as OneDrive; otherwise the Gradle wrapper cannot open its jar on Windows. A JDK is required, a JRE is not enough.

## 4. Repository layout

```
mini-akvaryum/
  README.md                       setup, commands, troubleshooting, architecture overview
  settings.gradle                 includes domain, game, desktop
  build.gradle                    shared config (Java 17, JUnit)
  gradle/libs.versions.toml       the only place for versions
  assets/                         optional real images; placeholders are generated in code
  docs/SPECIFICATION.md           this document
  docs/DECISIONS.md               every deviation from version 1.0, one row each
  docs/ADDING_A_FEATURE.md        step-by-step guide with a full example
  docs/WORK_PACKAGES.md           the open work, split for six people
  .github/workflows/build.yml     CI: ./gradlew build on push and pull request
  domain/   src/main/java/com/miniakvaryum/domain/...     src/test/java/...
  game/     src/main/java/com/miniakvaryum/game/...       src/test/java/...
  desktop/  src/main/java/com/miniakvaryum/desktop/...
```

| Module | Contains | May depend on |
|---|---|---|
| `domain` | All rules and state | JDK, Gson |
| `game` | Rendering, screens, UI, input, asset loading | `domain`, libGDX core |
| `desktop` | Window start-up, `SystemClock`, `DesktopBootIdProvider` | `game`, libGDX LWJGL3 |

Packages inside `domain` (root `com.miniakvaryum.domain`):

```
core/util      Vec2, RandomSource, JavaRandomSource, IdGenerator, UuidIdGenerator
core/time      Clock, BootIdProvider, TimeAnchor, GapSource, CreditedGap, GameTime
core/save      SaveStore, SaveData, SaveSection, JsonFileSaveStore, SaveMigrator,
               UnsupportedSaveVersionException
core/events    EventBus
core/module    DomainFeature, FeatureContext, ServiceRegistry
core/session   CoreConfig, GameSession, GameSessionFactory
feature/economy     EconomyFeature, EconomyConfig, api/{Wallet, LedgerEntry, Currency,
                    TxResult, LedgerChanged}
feature/security    SecurityFeature, SecurityConfig, api/{PinService, ParentSession,
                    PinState, VerifyStatus, VerifyResult, ParentAccessException}
feature/tasks       TasksFeature, TasksConfig, api/{TaskService, Task, TaskStatus,
                    ApprovalResult, TaskApproved}
feature/aquarium    AquariumFeature, AquariumConfig, WorldConfig, FishConfig, FoodConfig,
                    ActionWeights, api/fish/..., api/food/..., api/world/...
DomainFeatures      the list of domain features
```

Packages inside `game` (root `com.miniakvaryum.game`):

```
core           GameFeature, UiContext, CoreStrings
core/ui        MainScreen, MainLayout, WorldTapHandler, Navigator, BaseScreen,
               PlaceholderScreen, UiSkin, UiConfig, ShapeTextures
feature/economy     EconomyGameFeature, EconomyStrings
feature/aquarium    AquariumGameFeature, AquariumTextures, FishActor, FishAnimationMath,
                    PelletLayer, AquariumStrings
feature/security    SecurityGameFeature, ParentGateScreen, SecurityStrings
feature/tasks       TasksGameFeature, ChildTasksScreen, ParentScreen, TasksStrings
GameFeatures        the list of UI features
MiniAkvaryumGame    libGDX entry point and composition root of the UI
```

## 5. Architecture rules

- **Dependency direction:** `desktop -> game -> domain`. Never the reverse.
- **Core and features.** `core` knows no game concept and no feature. A feature is one self-contained piece of the game that plugs into the core. A feature is switched on by one line in `DomainFeatures` (rules) and one line in `GameFeatures` (UI); deleting the two lines removes it.
- **`api` packages.** Inside a domain feature, the `api` package holds what others may use: services, records, enums, events. Other features and the UI import only from there. Helpers nobody else may use go into an `internal` package.
- **Two ways to talk to another feature.** If you *need* something from it, call its service (`ctx.require(Wallet.class)`). If you only *announce* that something happened, publish an event (`events.publish(new TaskApproved(task))`); the publisher does not know or care who listens.
- **UI features never import each other.** They meet only through `UiContext`: the main screen's extension points and screen ids. Connections between them are made in `GameFeatures`.
- **Composition roots.** `GameSessionFactory` connects the domain; `MiniAkvaryumGame.create()` connects the UI. Nothing else creates services.
- **State changes only through services.** The UI reads domain state and never edits it. For fish and pellets this is enforced by the compiler: their setters are package-private.
- **Persisted, derived, runtime.** Persisted are facts only: ledger entries, tasks, PIN state, fish counters, the time anchor. Balances and fish level are derived and never saved. Fish positions, pellets, claims and the "parent area is unlocked" flag are runtime-only and never saved.
- **Actions end at boundaries.** A fish finishes its current action before it reacts to anything new. Every action has a bounded duration of at most 3 seconds.
- **Determinism.** With a scripted `RandomSource` and a fake `Clock`, domain behavior MUST be reproducible in tests.

What enforces the rules:

| Rule | Enforced by |
|---|---|
| `domain` has no UI code | Gradle: `domain` gets no libGDX dependency |
| Time only from `GameTime`, randomness only from `RandomSource`, no threads, no `System.exit` | `ArchitectureTest` (domain), `GameArchitectureTest` (game) |
| `core` refers to no feature | the same two tests |
| Features use each other only through `api`; UI features do not import each other | the same two tests |
| The UI cannot change a fish or a pellet | package-private setters |
| Finished packages are at least 70 % covered | JaCoCo gate |
| All of the above on every pull request | `.github/workflows/build.yml` |

If an architecture test fails, the message names the file and says what to do instead. Fix the code; do not weaken the test.

## 6. The feature contract

### 6.1 Domain side

```java
public interface DomainFeature {
    String id();                              // "tasks": also the key of its save section
    void install(FeatureContext ctx);         // once at start-up, in list order
    default void tick(double dtSeconds) {}    // once per frame, in list order
    default Optional<SaveSection> save() { return Optional.empty(); }   // on every save
}

public interface FeatureContext {
    GameTime time();
    RandomSource random();
    IdGenerator ids();
    EventBus events();
    CreditedGap creditedGap();                // offline time accepted at this start-up
    Optional<SaveSection> savedSection();     // empty: start with defaults
    <T> void provide(Class<T> type, T service);
    <T> T require(Class<T> type);             // only services of features installed earlier
    void requestSave();                       // save at the end of the current frame
}
```

Rules:

- A feature id matches `[a-z][a-z0-9-]*`, is unique, and never changes once save files exist.
- `install` typically: read the saved section or use defaults; `require` other services; build the own service; `provide` it; subscribe to events.
- A feature may `require` only what a feature **above it** in `DomainFeatures` provides. A wrong order stops the game at start-up with a message that names the missing service.
- Only one feature may provide a given service type.
- Services stay free of core classes where possible: they receive what they need as constructor arguments, for example an `EventBus` and a `Runnable onChange` that the feature binds to `ctx::requestSave`.

`EventBus`: synchronous and single-threaded. `subscribe(Class<E>, Consumer<? super E>)` and `publish(Object)`. A listener receives exactly the class it subscribed to. Listeners run in subscription order. An event published from inside a listener is delivered after the current event has reached all its listeners, so events are always handled in publish order. Events are immutable records in the `api` package of the publishing feature.

### 6.2 Session

```java
public final class GameSessionFactory {
    public static GameSession create(Clock clock, BootIdProvider boot, SaveStore store,
            RandomSource random, IdGenerator ids, CoreConfig config,
            List<DomainFeature> features);
}

public final class GameSession {
    public GameTime time();
    public CreditedGap creditedGap();
    public EventBus events();
    public <T> T get(Class<T> type);              // service lookup, throws if missing
    public <T> Optional<T> find(Class<T> type);
    public void tick(double dtSeconds);           // features, then save if due
    public void requestSave();
    public SaveData toSaveData();
    public void saveNow();
}
```

`create`: validate the feature ids; load the save (empty means new game); `time.startSession(saved anchor)`; install every feature in list order, giving each its own section; if there was no save, request one.

`tick`: tick every feature in list order; then save if a save was requested or `autosaveIntervalS` has passed since the last save. A failed automatic save is logged and retried after the next interval; it does not stop the game. `saveNow()` propagates the failure.

Sections whose feature is not installed are written back unchanged, so removing a feature (or working on a branch without it) never destroys its data.

### 6.3 UI side

```java
public interface GameFeature {
    String id();
    void install(UiContext ui);       // once at start-up, after the session exists
}

public final class UiContext {
    public GameSession session();     // ui.session().get(TaskService.class)
    public Skin skin();
    public Navigator nav();           // register(id, factory), show(id), showMain()
    public MainLayout main();         // extension points of the main screen
    public ShapeTextures shapes();    // placeholder images, or a file from assets
    public void restartFromSave();    // developer tools only (D-037)
}

public interface MainLayout {
    void addWorldLayer(int z, Actor actor);                   // lower z is further back
    void addStatus(Supplier<String> text);                    // top bar, refreshed per frame
    TextButton addButton(String label, Runnable onClick);
    TextButton addToggle(String label, Consumer<Boolean> onToggle);
    void addWorldTapHandler(WorldTapHandler handler);         // first handler returning true wins
    void addWorldDragHandler(WorldDragHandler handler);       // finger movements, same rule
    void addDebugLine(Supplier<String> line);                 // F3 overlay
    void showMessage(String text, float seconds);
}
```

The main screen contains no feature code. World layers in use: water -100, sand -90, pellets 0, fish 10, dirt 20.

Screens extend `BaseScreen(Skin)`, which provides the stage with a `FitViewport(1280, 720)`, input routing, `newRootTable()`, `button(text, action)`, `showMessage(text, seconds)` and `guarded(action)`. `guarded` turns the `UnsupportedOperationException` of skeleton code into an on-screen message; every button created with `button(...)` and every world tap is guarded.

## 7. Configuration

There is no single `GameConfig`. The core has `CoreConfig`; each feature has its own immutable config record with a static `defaults()` factory.

| Record | Constant | Default | Meaning |
|---|---|---|---|
| `CoreConfig` | `maxGapMillis` | 86 400 000 | Credited offline time cap (24 h) |
| | `autosaveIntervalS` | 30 | Autosave period |
| `EconomyConfig` | `initialFood`, `initialCoins` | 5, 0 | Starting stock, granted once as ledger entries |
| `SecurityConfig` | `pinLength`, `pinMaxAttempts` | 4, 5 | PIN rules |
| | `pbkdf2Iterations` | 120 000 | PIN hashing cost |
| | `lockoutSeconds` | 30, 60, 300, 900 | Lockout stages (last value repeats) |
| `TasksConfig` | `titleMaxLength`, `rewardMax` | 60, 999 | Task validation |
| `WorldConfig` | `worldWidth`, `worldHeight` | 1280, 720 | World units, origin bottom-left, y up |
| | `waterTop`, `waterFloor` | 640, 80 | Y range fish may use |
| | `fishMargin` | 70 | Fish centers keep this distance from the left and right edge, half of it from top and floor |
| `FishConfig` | `initialFishCount` | 3 | Fish in a new game |
| | `detectRange`, `eatRange` | 300, 70 | Pellet detection and eating distance |
| | `slowSpeed`, `dashSpeed` | 40, 160 | Units per second |
| | `slowDurationS`, `dashDurationS` | 3.0, 1.0 | Seconds |
| | `idleMinS`, `idleMaxS` | 1.0, 2.5 | Idle duration range |
| | `eatDurationS` | 1.5 | Eating time |
| | `level2Feeds`, `level2Hours` | 20, 48 | Level 2 needs both |
| | `level3Feeds`, `level3Hours` | 100, 300 | Level 3 needs both |
| `FoodConfig` | `pelletStartY`, `pelletSinkSpeed`, `pelletLifetimeS` | 620, 30, 60 | Pellet physics; unclaimed pellets dissolve |
| | `claimTimeoutS` | 3.5 | Safety timeout for a claim |

`AquariumConfig` groups `WorldConfig`, `FishConfig` and `FoodConfig`. `WorldConfig` offers `minFishX()` 70, `maxFishX()` 1210, `minFishY()` 115, `maxFishY()` 605.

Action weights (`ActionWeights`, part of `FishConfig`). The draw walks the actions in the order of `ActionType.values()`, which is the row order below; that order is part of the rules.

| Action | NORMAL | FOOD_DETECTED | FOOD_IN_EATING_RANGE |
|---|---|---|---|
| SLOW_MOVE | 2 | 3 | 0 |
| DASH | 1 | 2 | 0 |
| IDLE | 2 | 1 | 1 |
| EAT | 0 | 0 | 4 |

## 8. Core specification (complete; do not change)

### 8.1 util

```java
public record Vec2(double x, double y) {       // immutable; Vec2.ZERO
    Vec2 plus(Vec2 o); Vec2 minus(Vec2 o); Vec2 scale(double k);
    double length(); Vec2 normalized(); double distanceTo(Vec2 o);
}
public interface RandomSource { int nextInt(int bound); double nextDouble(); }
public interface IdGenerator { String next(); }
```

`normalized()` of the zero vector is the zero vector. Production: `JavaRandomSource(long seed)`, `UuidIdGenerator`.

### 8.2 time: trusted game time

```java
public interface Clock { long wallMillis(); long monotonicNanos(); }
public interface BootIdProvider { Optional<String> currentBootId(); }
public record TimeAnchor(long gameMillis, long monotonicNanos, long wallMillis, String bootId) {}
public enum GapSource { FIRST_RUN, MONOTONIC, WALL, WALL_NEGATIVE_CLAMPED }
public record CreditedGap(long millis, GapSource source, boolean capped) {}
public final class GameTime {
    public GameTime(Clock clock, BootIdProvider boot, long maxGapMillis);
    public CreditedGap startSession(Optional<TimeAnchor> saved);   // exactly once
    public long nowMillis();
    public TimeAnchor anchor();
}
```

Algorithm of `startSession`:

1. No saved anchor: gap 0, source `FIRST_RUN`, game time starts at 0.
2. Saved anchor exists, its `bootId` is non-empty and equals the current boot id, and `monotonicNow >= saved.monotonicNanos`: `raw = (monotonicNow - saved.monotonicNanos) / 1_000_000`, source `MONOTONIC`. The wall clock is ignored.
3. Otherwise: `raw = wallNow - saved.wallMillis`. If `raw < 0` then `raw = 0` and source `WALL_NEGATIVE_CLAMPED`, else source `WALL`.
4. `gap = min(raw, maxGapMillis)`; `capped = raw > maxGapMillis`.
5. `gameMillisAtStart = saved.gameMillis + gap`.

`nowMillis() = gameMillisAtStart + (clock.monotonicNanos() - monotonicAtStart) / 1_000_000`. Calling `startSession` twice, or `nowMillis`/`anchor` before it, throws `IllegalStateException`.

`DesktopBootIdProvider`: on Linux reads `/proc/sys/kernel/random/boot_id`; on every other system returns empty, which forces the wall-clock path with the 24 h cap.

### 8.3 save

```java
public interface SaveStore { Optional<SaveData> load(); void save(SaveData data); }
public record SaveSection(int version, JsonElement data) {}
public record SaveData(int schemaVersion, TimeAnchor time, Map<String, SaveSection> sections) {}
public final class SaveMigrator {
    public SaveMigrator(int currentVersion);
    public SaveMigrator step(int fromVersion, UnaryOperator<JsonElement> step);
    public JsonElement migrate(int version, JsonElement json);
}
```

The file has a fixed frame (`schemaVersion`, `time`) and one section per feature, keyed by feature id (example in section 11). `SaveData.SCHEMA_VERSION = 1` is the version of the frame. Each section carries its own version, owned and migrated by its feature.

`JsonFileSaveStore(Path directory)`:

- Location: `${user.home}/.mini-akvaryum/`, overridable with the system property `akvaryum.home`. Files: `save.json`, `save.json.bak`, `save.json.tmp`.
- Atomic write: write to `save.json.tmp`, flush and fsync, copy the current `save.json` to `save.json.bak`, then `Files.move(tmp, save.json, ATOMIC_MOVE, REPLACE_EXISTING)`.
- Load: if `save.json` is missing, return empty. If it is unreadable or invalid, rename it to `save.json.corrupt-<millis>` (its last-modified time) and try `save.json.bak`; if that also fails, return empty and log a warning. A corrupt file is never deleted.
- A file with a newer `schemaVersion` than the code supports throws `UnsupportedSaveVersionException`, is not renamed, and the store refuses every later `save`.

`SaveMigrator` upgrades JSON one version at a time. It is used for the frame and by every feature for its own section. A version newer than the current one throws `UnsupportedSaveVersionException`; a missing step is an `IllegalStateException`.

## 9. Feature specifications

Each subsection gives the public API as built, the rules, and the required tests. "Complete" means implemented and tested; everything else is a skeleton to implement.

### 9.1 economy (complete)

Provides `Wallet`. Requires nothing. Publishes `LedgerChanged(LedgerEntry entry)` after every recorded transaction. Save section: `{ "ledger": [ LedgerEntry... ] }`, version 1.

```java
public enum Currency { COIN, FOOD }
public record LedgerEntry(String refId, Currency currency, long delta, String reason,
        long gameMillis) {}
public enum TxResult { OK, INSUFFICIENT_FUNDS, ALREADY_APPLIED, INVALID_AMOUNT }
public final class Wallet {
    public Wallet(List<LedgerEntry> entries);
    public Wallet(List<LedgerEntry> entries, Consumer<LedgerEntry> onAppend);
    public long balance(Currency c);
    public TxResult credit(Currency c, long amount, String reason, String refId, long gameMillis);
    public TxResult debit(Currency c, long amount, String reason, String refId, long gameMillis);
    public List<LedgerEntry> entries();        // unmodifiable
}
```

Rules: the ledger is append-only and a balance is the sum of its entries. Checks in this order: `amount <= 0` gives `INVALID_AMOUNT`; a `refId` already used for that currency gives `ALREADY_APPLIED` and changes nothing; a debit above the balance gives `INSUFFICIENT_FUNDS`. Balances can never be negative. A blank `refId` throws `IllegalArgumentException`.

**The `refId` is how anything is paid or charged exactly once.** Name it after its cause: `"init:food"`, `"task:<id>:coin"`, `"pellet:<pelletId>"`.

A new game (no economy section) credits `initialFood` with refId `"init:food"` and `initialCoins` with refId `"init:coin"`; an amount of 0 creates no entry.

### 9.2 security (skeleton)

Provides `PinService`, `ParentSession`. Requires nothing. Save section: `{ "pin": PinState }`, or `{}` while no PIN is set; version 1.

```java
public record PinState(String saltB64, String hashB64, int iterations,
        int failedAttempts, int lockoutStage, long lockedUntilGameMillis) {}
public enum VerifyStatus { OK, WRONG, LOCKED, NOT_SET }
public record VerifyResult(VerifyStatus status, int attemptsLeft, long lockedRemainingMillis) {}
public final class PinService {
    public PinService(PinState stateOrNull, GameTime time, SecurityConfig config);
    public PinService(PinState stateOrNull, GameTime time, SecurityConfig config,
            Runnable onChange);
    public boolean isPinSet();                    // complete
    public Optional<PinState> state();            // complete
    public void setPin(String pin);               // TODO
    public VerifyResult verify(String pin);       // TODO
}
public final class ParentSession {
    public ParentSession(PinService pins);
    public VerifyResult unlock(String pin);       // TODO
    public boolean isUnlocked();                  // complete
    public void lock();                           // complete
    public void requireUnlocked();                // complete; throws ParentAccessException
}
```

Rules:

- A PIN is exactly `pinLength` digits (`[0-9]`); anything else throws `IllegalArgumentException` and is not counted as an attempt.
- Hash: `PBKDF2WithHmacSHA256`, 16 random salt bytes from `SecureRandom`, 256-bit key, iterations from the config. Compare with `MessageDigest.isEqual`. Never store, log or print the PIN.
- `setPin` works only while no PIN is set; otherwise `IllegalStateException`.
- `verify`, in this order: no PIN set gives `NOT_SET`. Locked (`now < lockedUntilGameMillis`) gives `LOCKED` with the remaining time and does **not** check the PIN. A correct PIN resets `failedAttempts` and `lockoutStage` to 0 and gives `OK`. A wrong PIN increments `failedAttempts`; when it reaches `pinMaxAttempts`, set `lockedUntilGameMillis = now + lockoutSeconds[min(stage, last index)] * 1000`, increment `lockoutStage`, reset `failedAttempts` to 0 and give `LOCKED`; otherwise give `WRONG` with the attempts left.
- Lock expiry uses `GameTime`, not the wall clock.
- Call `onChange` after every change of the state.
- `ParentSession.unlock` calls `verify`; the session is unlocked exactly when the status is `OK`. The unlocked flag is never saved.

Required tests (`PinServiceTest`, 13; `ParentSessionTest`, 2 waiting): set then verify; verify without a PIN; wrong PIN decrements `attemptsLeft`; lockout after 5 failures with durations 30, 60, 300, 900, 900 s; verify during lockout does not accept the right PIN; lock expires with fake time; reset on success; invalid PIN format; same PIN gives a different hash with a different salt; setting a PIN twice is refused; the PIN is never part of the saved state; every change requests a save; state survives a restart; unlock with the correct PIN; unlock with a wrong PIN.

### 9.3 tasks (skeleton)

Provides `TaskService`. Requires `Wallet`, `ParentSession`. Publishes `TaskApproved(Task task)` once per task. Save section: `{ "tasks": [ Task... ] }`, version 1.

```java
public enum TaskStatus { ACTIVE, PENDING, APPROVED }
public record Task(String id, String title, int coinReward, int foodReward, TaskStatus status) {
    public Task withStatus(TaskStatus newStatus);
}
public enum ApprovalResult { APPROVED, ALREADY_APPROVED, NOT_PENDING }
public final class TaskService {
    public TaskService(List<Task> initial, Wallet wallet, ParentSession parent, GameTime time,
            IdGenerator ids, TasksConfig config, EventBus events, Runnable onChange);
    public Task create(String title, int coins, int food);              // parent only, TODO
    public Task update(String id, String title, int coins, int food);   // parent only, TODO
    public void delete(String id);                                      // parent only, TODO
    public void reportDone(String id);                                  // child, TODO
    public ApprovalResult approve(String id);                           // parent only, TODO
    public void reject(String id);                                      // parent only, TODO
    public List<Task> list();                                           // complete
    public List<Task> list(TaskStatus status);                          // complete
}
```

Rules:

- Parent-only methods call `parent.requireUnlocked()` as their first line.
- Validation: the title is trimmed and must then have 1 to `titleMaxLength` characters; each reward is 0 to `rewardMax`. Violations throw `IllegalArgumentException`. An unknown id throws `IllegalArgumentException`.
- State machine: `ACTIVE -> PENDING` (`reportDone`), `PENDING -> APPROVED` (`approve`), `PENDING -> ACTIVE` (`reject`). `update` and `delete` only while `ACTIVE`. Any other transition throws `IllegalStateException`, except `approve` on an `APPROVED` task, which returns `ALREADY_APPROVED`.
- `approve` credits coins with refId `"task:<id>:coin"` and food with refId `"task:<id>:food"`, reason `"task"`; a reward of 0 is skipped. Both `OK` and `ALREADY_APPLIED` count as success. Then it stores the `APPROVED` task, publishes `TaskApproved` and calls `onChange`.
- Tasks are immutable; replace the list element.

Required tests (`TaskServiceTest`, 14): full happy path; report does not pay; approve twice pays once; reject returns to ACTIVE and a later approve pays once; parent-only methods throw when locked; `reportDone` works while locked; title trimmed and validated; rewards validated; update and delete only while ACTIVE; unknown id; approving an ACTIVE task is illegal; a reward of 0 creates no ledger entry; `TaskApproved` is published exactly once; tasks and rewards survive a restart.

### 9.4 aquarium (skeleton)

Provides `AquariumWorld`, `FoodService`. Requires `Wallet`. Publishes `FishFed(String fishId, int feedCount)`. Save section: `{ "fish": [ FishData... ] }`, version 1. Sub-packages `api.fish`, `api.food`, `api.world`.

The feature class is complete: a new game creates `initialFishCount` unnamed fish; every start gives each fish a random position inside the swim area (x drawn first, then y) and adds the credited gap to its `aquariumMillis`.

**fish**

```java
public enum ActionType { SLOW_MOVE, DASH, IDLE, EAT }
public enum FishContext { NORMAL, FOOD_DETECTED, FOOD_IN_EATING_RANGE }
public enum FishEvent { NONE, FINISHED_EATING }
public record FishData(String id, String name, int feedCount, long aquariumMillis) {}
public final class FishState {        // runtime only; getters public, setters package-private
    Vec2 position(); int facing(); ActionType action(); double remainingS();
    Vec2 velocity(); String claimedPelletId();
}
public final class FishEntity {       // complete
    public FishEntity(FishData data, Vec2 startPosition);
    String id(); String name(); int feedCount(); long aquariumMillis(); FishState state();
    public FishData toData();
    public void recordFeeding();                 // feedCount++, clears the claim
    public void addAquariumMillis(long millis);
}
public final class LevelRules {
    public static int levelOf(int feedCount, long aquariumMillis, FishConfig config);   // TODO
}
public final class FishBrain {
    public FishBrain(AquariumConfig config, RandomSource random);
    public FishEvent update(FishEntity fish, FoodField food, double dtSeconds);         // TODO
}
```

`LevelRules`: hours are `aquariumMillis / 3 600 000`. Level 3 if `feedCount >= level3Feeds` and `hours >= level3Hours`; else level 2 if `feedCount >= level2Feeds` and `hours >= level2Hours`; else 1.

`FishBrain.update`, one call per fish per frame:

1. If the fish has a running action (`remainingS > 0`), advance it: `position += velocity * dt`, `remainingS -= dt`, clamp the position into `[minFishX, maxFishX] x [minFishY, maxFishY]`. If the action just ended and it was `EAT`, return `FINISHED_EATING`; otherwise `NONE`.
2. Otherwise choose a new action. Find the nearest FREE pellet (`food.nearestFree`); claimed pellets are never considered. The context is `FOOD_IN_EATING_RANGE` if its distance is `<= eatRange`, else `FOOD_DETECTED` if `<= detectRange`, else `NORMAL`. Draw `random.nextInt(weights.total(context))` and walk the actions in order.
3. Starting `SLOW_MOVE` or `DASH`: duration and speed from the config. Direction: in `NORMAL` a random angle `random.nextDouble() * 2 * PI`; in `FOOD_DETECTED` toward the pellet. If the end point `position + dir * speed * duration` would leave the allowed area, mirror the offending component of the direction. Set `facing = sign(dir.x)` if `abs(dir.x) > 0.001`, otherwise keep it.
4. Starting `IDLE`: velocity 0, duration `idleMinS + random.nextDouble() * (idleMaxS - idleMinS)`. Then, if `random.nextDouble() < 0.3`, flip facing.
5. Starting `EAT`: `food.tryClaim(pelletId, fish.id())`. If it fails, start `IDLE` instead. On success store the claimed pellet id, velocity 0, duration `eatDurationS`, face the pellet.
6. Facing changes only when an action starts. Random numbers are drawn exactly in the order written here.

**food**

```java
public enum PelletState { FREE, CLAIMED }
public final class Pellet {           // runtime only; getters public, setters package-private
    String id(); Vec2 position(); PelletState state(); String claimedBy();
    double ageS(); double claimAgeS();
}
public final class FoodField {
    public FoodField(AquariumConfig config);
    public List<Pellet> pellets();                              // complete, unmodifiable
    public Optional<Pellet> nearestFree(Vec2 from);             // TODO
    public boolean tryClaim(String pelletId, String fishId);    // TODO
    public void release(String pelletId, String fishId);        // TODO
    public void remove(String pelletId);                        // TODO
    public void update(double dtSeconds);                       // TODO
}
public enum EatResult { OK, CLAIM_LOST, INSUFFICIENT_FOOD }
public final class FoodService {
    public FoodService(FoodField field, Wallet wallet, GameTime time, IdGenerator ids,
            AquariumConfig config, EventBus events);
    public long available();                                           // TODO
    public Optional<String> drop(double x);                            // TODO
    public EatResult completeEating(FishEntity fish, String pelletId); // TODO
}
```

- `tryClaim` succeeds only if the pellet exists and is FREE. `release` frees a pellet only if it is CLAIMED by exactly that fish.
- `FoodField.update`: each pellet sinks at `pelletSinkSpeed` until `waterFloor + 10`. A FREE pellet older than `pelletLifetimeS` is removed and costs nothing. A CLAIMED pellet whose `claimAgeS` exceeds `claimTimeoutS` becomes FREE again.
- `available()` is the FOOD balance minus the pellets in the water (FREE and CLAIMED).
- `drop(x)` creates a FREE pellet at `(clamp(x), pelletStartY)` only if `available() > 0`. Dropping does not change the wallet.
- `completeEating` is one all-or-nothing step: the pellet must be CLAIMED by this fish (else `CLAIM_LOST`, no change); `wallet.debit(FOOD, 1, "pellet", "pellet:" + pelletId, now)` (if not OK, release the pellet and return `INSUFFICIENT_FOOD`); `fish.recordFeeding()`; remove the pellet; publish `FishFed`; return `OK`.

**world**

```java
public final class AquariumWorld {
    public AquariumWorld(AquariumConfig config, List<FishEntity> fish, FoodField food,
            FoodService foodService, FishBrain brain);
    public AquariumConfig config();              // complete
    public List<FishEntity> fish();              // complete
    public FoodField food();                     // complete
    public void update(double dtSeconds);        // TODO (does nothing until implemented)
    public Optional<String> dropPellet(double x);   // TODO
}
```

`update`: `dt = min(dtSeconds, 1/15)`; increment a tick counter; update the food field; visit every fish once, starting at index `tickCounter % n` and wrapping around (fair order); for each fish remember its claimed pellet id, call `brain.update`, on `FINISHED_EATING` call `foodService.completeEating`, then add `dt * 1000` to its `aquariumMillis`.

Required tests: `LevelRulesTest` (6): all boundaries 19/20 feeds, 47/48 h, 99/100 feeds, 299/300 h. `FishBrainTest` (18): weights respected (seeded random, 10 000 draws, tolerance 3 %) in all three contexts; context switching at the exact range borders; claimed pellets ignored; direction toward the pellet; clamping; mirroring; facing only changes at action start; idle duration and the 30 % flip; failed claim falls back to IDLE; `FINISHED_EATING` returned exactly once. `FoodFieldTest` (12) and `FoodServiceTest` (13): three fish and one pellet give exactly one eater; the second claim fails; stock decreases only on `completeEating`; an uneaten pellet dissolves without cost; drop refused when `available() == 0`, including pellets already in the water; claim timeout releases; `completeEating` twice pays once; `available()` arithmetic. `AquariumWorldTest` (7): fair update order; long frames cut to 1/15 s; a world update with 3 fish and 1 pellet ends with exactly one feed count increment.

Order inside this feature: implement `FoodField` first; the `FishBrain` tests need it.

## 10. Game module specification

### 10.1 Application structure

`MiniAkvaryumGame extends com.badlogic.gdx.Game` takes a `Clock` and a `BootIdProvider`. `create()` builds the session with `GameSessionFactory`, `JsonFileSaveStore`, `JavaRandomSource(clock.monotonicNanos())` (the clock reading is only the random seed), `UuidIdGenerator`, `CoreConfig.defaults()` and `DomainFeatures.all()`; then the skin, `ShapeTextures`, `Navigator` and the empty `MainScreen`; then installs every `GameFeature` from `GameFeatures.all()`; then shows the main screen. `dispose()` disposes the UI and calls `session.saveNow()`.

`desktop` contains `DesktopLauncher` (title from `CoreStrings.APP_TITLE`, 1280 x 720, resizable, vsync on, foreground FPS 60), `SystemClock` (the only place that may call `System.currentTimeMillis()` and `System.nanoTime()`) and `DesktopBootIdProvider`.

One `FitViewport(1280, 720)` is used for the world and for every Scene2D stage. All positions are in world units, never pixels. Touchable controls are at least 64 x 64 units. Mouse clicks behave as touches; use stage input, no mouse-only APIs.

### 10.2 Frame loop

`MiniAkvaryumGame.render()`: `dt = min(delta, 1/15)`; `session.tick(dt)`; then the active screen draws. The rules advance on every screen, not only on the main screen. Actors copy domain state in `act(delta)`.

### 10.3 Placeholder graphics

`ShapeTextures` creates rectangles, circles, ellipses and triangles with `Pixmap`, and `fileOr(path, placeholder)` loads `assets/<path>` instead when that file exists. Missing files are never an error. `AquariumTextures`:

| Part | Size | Placeholder | File |
|---|---|---|---|
| body | 120 x 64 | orange ellipse | `assets/fish/body.png` |
| head | 44 x 44 | darker orange circle | `assets/fish/head.png` |
| tail | 48 x 56 | triangle, tip right | `assets/fish/tail.png` |
| fin | 36 x 24 | triangle, tip up | `assets/fish/fin.png` |
| pellet | 12 x 12 | brown circle | `assets/pellet.png` |

All fish images are drawn for a fish that faces right.

### 10.4 FishActor (skeleton: body and head exist)

A `Group` whose own position is the body center. Children are added back to front: tail, body, fin, head. Offsets are from the body center to the part center, for a fish facing right.

| Part | Size | Offset (x, y) | Pivot (relative to the part) | Animation |
|---|---|---|---|---|
| tail | 48 x 56 | (-62, 0) | front edge center (48, 28) | angle = 22° · sin(2π · f · t) |
| body | 120 x 64 | (0, 0) | center | none |
| fin | 36 x 24 | (-6, 34) | base center (18, 0) | angle = 12° · sin(2π · f · t + π/2) |
| head | 44 x 44 | (52, 0) | center | none |

Swim frequency f in Hz by action: IDLE 0.8, SLOW_MOVE 1.5, DASH 3.0, EAT 1.0. t is a per-actor running time in seconds. The math lives in `FishAnimationMath` (three `TODO` methods, three waiting tests) so it can be tested without a window.

Mirroring: `setScaleX(fish.state().facing())`. Because offsets and pivots are local to the group, nothing is recomputed when the fish turns.

### 10.5 Screens

| Screen | Feature | State | Content and behavior |
|---|---|---|---|
| Main screen | core + all | Complete frame | Top bar: "Coins: N", "Food: N"; buttons "Feed" (toggle), "Tasks", "Parents". Aquarium below. F3 toggles the debug overlay: FPS, game time, credited gap with source and capped flag, fish count, pellet count. |
| Feed mode | aquarium | Complete | While "Feed" is on, a tap in the water calls `world.dropPellet(x)`. If it returns empty, show "No food available" for 2 s. |
| `ChildTasksScreen` | tasks | Skeleton | ACTIVE tasks with a button "Done" (calls `reportDone`). PENDING tasks with the text "Waiting for parents". A count of APPROVED tasks. Back button. |
| `ParentGateScreen` | security | Skeleton | If no PIN is set: ask for a new PIN twice ("Set a PIN"). Otherwise "Enter PIN". On-screen number pad (0 to 9, clear, OK) and keyboard digits; typed digits shown as dots. Show "Wrong PIN, N attempts left" or "Locked, M seconds left" (counting down). On success open the screen whose id was passed to `SecurityGameFeature`. |
| `ParentScreen` | tasks | Skeleton | New-task form (title, coins, food, button "Create task"). List of all tasks; ACTIVE tasks have "Edit" and "Delete"; PENDING tasks have "Approve" and "Reject". Button "Lock and go back" calls `parent.lock()` and returns to the main screen. Leaving the screen in any way locks the session. |

Open point for the food display: show the stock with the free amount, for example "Food: 5 (3 free)", once `FoodService.available()` exists.

All texts are in the `...Strings` class of their feature. Show domain errors (validation, locked, no food) as short messages, never as stack traces.

Screen ids: `"main"`, `"tasks"`, `"parent-area"`, `"parent-gate"`.

### 10.6 Saving

The UI does not call `saveNow()` after actions. Domain code requests saves (section 6.1); the session writes at the end of the frame, every `autosaveIntervalS`, and in `dispose()`.

## 11. Example save file (schema version 1)

```json
{
  "schemaVersion": 1,
  "time": { "gameMillis": 7200000, "monotonicNanos": 123456789000,
            "wallMillis": 1763000000000, "bootId": "" },
  "sections": {
    "economy": { "version": 1, "data": { "ledger": [
      { "refId": "init:food", "currency": "FOOD", "delta": 5, "reason": "initial",
        "gameMillis": 0 },
      { "refId": "task:7f3c:coin", "currency": "COIN", "delta": 10, "reason": "task",
        "gameMillis": 3600000 } ] } },
    "security": { "version": 1, "data": { "pin": {
      "saltB64": "...", "hashB64": "...", "iterations": 120000,
      "failedAttempts": 0, "lockoutStage": 0, "lockedUntilGameMillis": 0 } } },
    "tasks": { "version": 1, "data": { "tasks": [
      { "id": "7f3c", "title": "Tidy your room", "coinReward": 10, "foodReward": 2,
        "status": "APPROVED" } ] } },
    "aquarium": { "version": 1, "data": { "fish": [
      { "id": "a1", "feedCount": 3, "aquariumMillis": 7200000 } ] } }
  }
}
```

## 12. Testing requirements

- JUnit Jupiter, one test class per production class (`XTest`). No `Thread.sleep`. No real clock, no real randomness, no network. File tests use `@TempDir`.
- Name tests after the behavior, for example `approveTwicePaysOnce`.
- Every "Required tests" list in section 9 is mandatory. The test methods exist already by name; enable the class and write them.
- When a package is finished, add it to `coverageGatedPackages` in `domain/build.gradle`. Gated packages need at least 70 % line coverage. Currently gated: `domain/core/**`, `domain/feature/economy/**`.
- The `game` module has no mandatory tests beyond `GameArchitectureTest`. Keep pure math in small classes that can be tested without OpenGL.

Test helpers (in `domain/src/test`):

| Helper | Use |
|---|---|
| `TestGame` | A complete fake device. `game.start(features...)` starts a session with exactly those features; calling it again simulates closing and reopening the app. `startFullGame()` uses `DomainFeatures.all()`. |
| `game.clock` (`FakeClock`) | `advanceMillis(..)`: real time passes. `shiftWallMillis(..)`: the user changes the date. `setMonotonicNanos(..)`: reboot. |
| `game.boot` (`FakeBootId`) | `set(..)` simulates a reboot; `FakeBootId.unknown()` a platform without boot id. |
| `game.random` (`ScriptedRandom`) | `ints(..)`, `doubles(..)` script the values; running out of values fails the test. |
| `game.ids` (`SequentialIds`) | Ids are `id-1`, `id-2`, ... |
| `game.store` (`InMemorySaveStore`) | `saveCount()`, `lastSaved()`, `setFailing(true)`. |
| `ParentSessions.forceUnlock(parent)` | Opens the parent area without a PIN, so task tests do not depend on the PIN logic. |
| `Fishes.at(..)`, `eating(..)`, `moving(..)`, `facing(..)` | Build a fish in an exact state. |

## 13. Build and CI

| Command | Result |
|---|---|
| `./gradlew build` | Compiles everything, runs all tests, the architecture tests and the coverage gate |
| `./gradlew test` | Tests only |
| `./gradlew :desktop:run` | Starts the game window |

On Windows use `.\gradlew.bat` instead of `./gradlew`.

GitHub Actions workflow `.github/workflows/build.yml`: triggers on push and pull request to `main`; Temurin JDK 17; runs `./gradlew build`; uploads the test reports when the build fails. No secrets. Repository settings: protected `main`, pull request required, one approval, the `build` check required.

## 14. How to add a feature

This section is the procedure for a **new** feature. Finishing a skeleton is steps 3 to 6 only. A complete, compiled example (an energy feature) is in `docs/ADDING_A_FEATURE.md`.

### 14.1 Before writing code

1. Name the feature: a short lower-case id such as `energy`. It becomes the package name and the key of the save section.
2. Decide what it **provides** (its service), what it **requires** (services of other features) and what it **publishes** (events). Write these three lines into the Javadoc of the feature class.
3. Decide what must be **saved**. Only facts; nothing that can be recalculated; nothing that exists only while the game runs.
4. Check whether you need something another feature does not offer yet. If yes, ask its owner to add it to its `api` package or to publish an event. Do not edit their code.

### 14.2 Domain

Create `domain/.../feature/<id>/` with:

| File | Content |
|---|---|
| `<Name>Config.java` | Record with every tunable number and a `defaults()` factory |
| `api/<Name>Service.java` | The rules. Constructor arguments instead of global access. |
| `api/...` | Records, enums and events others may use |
| `<Name>Feature.java` | `implements DomainFeature`; shape below |

```java
public final class EnergyFeature implements DomainFeature {

    public static final String ID = "energy";         // never change it
    private static final int SECTION_VERSION = 1;

    private final EnergyConfig config;
    private EnergyService service;

    public EnergyFeature(EnergyConfig config) { this.config = config; }

    @Override public String id() { return ID; }

    @Override public void install(FeatureContext ctx) {
        double saved = ctx.savedSection().map(s -> s.data().getAsDouble()).orElse(0.0);  // 1
        service = new EnergyService(saved, config);                                      // 2
        service.refill(ctx.creditedGap().millis() / 3_600_000.0);                        // 3
        ctx.provide(EnergyService.class, service);                                       // 4
    }

    @Override public void tick(double dtSeconds) { service.refill(dtSeconds / 3600.0); }

    @Override public Optional<SaveSection> save() {
        return Optional.of(new SaveSection(SECTION_VERSION,
                new JsonPrimitive((double) service.energy())));
    }
}
```

1 saved state or default; 2 build the service; 3 apply what happened while the app was closed; 4 let others use it.

Add one line to `DomainFeatures.all()`, below every feature it requires:

```java
new EnergyFeature(EnergyConfig.defaults())
```

### 14.3 Tests

Same package under `domain/src/test`. Test the service directly where possible, and the feature through `TestGame`:

```java
TestGame game = new TestGame();
GameSession session = game.start(new EnergyFeature(EnergyConfig.defaults()));
session.saveNow();
game.clock.advanceMillis(2 * 3_600_000L);                                      // two hours pass
GameSession later = game.start(new EnergyFeature(EnergyConfig.defaults()));    // reopen the app
assertThat(later.get(EnergyService.class).energy()).isEqualTo(20);
```

Always test: new game defaults; save, restart, load; what happens with credited offline time; every rule and its boundaries.

### 14.4 UI

Create `game/.../feature/<id>/` with a `<Name>GameFeature implements GameFeature`, a `<Name>Strings` class for all user-visible text, and screens if needed.

```java
public final class EnergyGameFeature implements GameFeature {
    @Override public String id() { return "energy"; }
    @Override public void install(UiContext ui) {
        EnergyService energy = ui.session().get(EnergyService.class);
        ui.main().addStatus(() -> EnergyStrings.ENERGY + energy.energy());
    }
}
```

Add one line to `GameFeatures.all()`. For a screen: extend `BaseScreen`, register it with `ui.nav().register("energy", () -> new EnergyScreen(ui))`, open it with `ui.nav().show("energy")`.

### 14.5 Shared files you may edit

| File | What you add |
|---|---|
| `domain/.../DomainFeatures.java` | One line for your feature |
| `game/.../GameFeatures.java` | One line for your UI feature |
| `domain/build.gradle`, `coverageGatedPackages` | One line for your finished package |
| `docs/DECISIONS.md` | One row per decision that differs from this document |

Nothing else outside your feature. Merge conflicts in these files are resolved by keeping both lines.

### 14.6 Checklist before the pull request

- `./gradlew build` is green.
- No `TODO` left in what you claim to have finished; its tests are enabled and written.
- Your packages are in `coverageGatedPackages`.
- No file outside your feature changed, except the lines of section 14.5.
- You started the game and tried your part by hand.

### 14.7 Recipes

| You want to | Do this |
|---|---|
| React to something another feature does | `ctx.events().subscribe(TaskApproved.class, e -> ...)` in `install` |
| Store extra data about fish (mood, name, accessory) | Keep a map from fish id to your data in your own feature and save it in your own section. Do not add fields to `FishData`. |
| Make sure a change is saved | Call `ctx.requestSave()` after it (pass `ctx::requestSave` into your service as a `Runnable`) |
| Change the format of your save section | Raise `SECTION_VERSION`, register `new SaveMigrator(2).step(1, json -> ...)`, add a test that loads a version 1 section |
| Pay or charge exactly once | Give the wallet transaction a `refId` that names its cause |
| Do something while the app was closed | Use `ctx.creditedGap().millis()` in `install` |
| Do something every frame | Override `tick(dtSeconds)` |
| Draw something in the aquarium | `ui.main().addWorldLayer(z, actor)`; the actor reads domain state in `act` |
| React to taps into the water | `ui.main().addWorldTapHandler((x, y) -> ...)`; return true if you used the tap |
| Open a screen of another feature | `ui.nav().show(id)`; get the id as a constructor argument set in `GameFeatures` |

## 15. Work packages (replaces the implementation steps of version 1.0)

The base corresponds to steps S1 to S5 of version 1.0, plus the wiring of S6 to S14. The rest is split so that six people can work in parallel.

| # | Package | Domain | UI | Tests | Needs |
|---|---|---|---|---|---|
| 1 | Parent PIN | `PinService.setPin`, `verify`; `ParentSession.unlock` | `ParentGateScreen` | `PinServiceTest` 13, `ParentSessionTest` 2 | nothing |
| 2 | Tasks | `TaskService`: 6 methods | `ChildTasksScreen`, `ParentScreen` | `TaskServiceTest` 14 | nothing for the rules (`ParentSessions.forceUnlock`); package 1 to reach the parent screen in the game |
| 3 | Food | `FoodField`: 5 methods; `FoodService`: 3 methods | food display "stock (free)" | `FoodFieldTest` 12, `FoodServiceTest` 13 | nothing; do `FoodField` first |
| 4 | Fish behavior | `LevelRules.levelOf`; `FishBrain.update` | | `LevelRulesTest` 6, `FishBrainTest` 18 | `FoodField` from package 3 |
| 5 | World and fish drawing | `AquariumWorld.update`, `dropPellet` | `FishActor`, `FishAnimationMath` | `AquariumWorldTest` 7, `FishAnimationMathTest` 3 | `update` needs packages 3 and 4; the UI parts need nothing |
| 6 | Integration | | review of all screens | demo script | packages 1 to 5 |

Done when: package 1, demo steps 2 and 4; package 2, demo steps 2 to 4; package 3, a dropped pellet sinks and dissolves without cost; package 4, all its tests; package 5, three four-part fish swim, turn and animate, and a pellet is eaten by exactly one fish; package 6, the whole demo script.

## 16. Demo script (final acceptance)

1. Delete the `.mini-akvaryum` folder. Start the game. Three fish swim; coins show 0, food shows 5.
2. Open "Parents", set the PIN 1234 (twice). Create the task "Tidy your room" with 10 coins and 2 food. Lock and go back.
3. Open "Tasks", press "Done". The task shows "Waiting for parents". Coins are still 0.
4. Open "Parents", enter a wrong PIN five times. The gate shows a lockout. Wait 30 s, enter 1234, press "Approve". Coins 10, food 7. Press nothing else: the reward does not repeat.
5. Press "Feed", click in the water three times. Fish swim to the pellets and each pellet is eaten by exactly one fish. Food drops by 3.
6. Close the window. Start again. Coins, food, the task state, the PIN and the feed counts are unchanged.
7. Set the system date back one day and start again. No extra progress is granted. Set the date forward 3 days: at most 24 hours are credited (visible in the F3 overlay).

## 17. Forbidden

- No Android code, no network or backend code, no analytics, no extra dependencies.
- No features from the out-of-scope list in section 2.3, and no placeholders for them.
- No reflection tricks, no global static state, no `System.exit`.
- Do not store the PIN, log it, or print it.
- Do not change `core`, another feature, or a public signature from sections 6, 8 and 9 without the agreement of the team and a row in `docs/DECISIONS.md`.
- Do not disable or weaken an architecture test or the coverage gate.

## 18. Decisions: what changed since version 1.0 and why

### 18.1 The main idea

Version 1.0 had five places that every new feature would have had to edit: the `SaveData` record, the `GameSession` fields and factory, the single `GameConfig`, the single `Strings` class, and the main screen. With six people adding features at the same time, each of these is a permanent merge conflict. All structural decisions below remove one of those shared places and replace it by something a feature brings along itself:

| Shared place in 1.0 | Replaced by | Decision |
|---|---|---|
| `SaveData(ledger, tasks, pin, fish)` | one save section per feature | D-004 |
| `GameSession` fields, hand-written wiring | `DomainFeature` list and `session.get(Type.class)` | D-007, D-008 |
| one `GameConfig` | one config record per feature | D-009 |
| one `Strings` | one `...Strings` class per feature | D-013 |
| one fixed `MainScreen` | `MainLayout` extension points and `GameFeature` list | D-022 |
| direct calls for everything | services downward, events upward | D-007 |
| `saveNow()` calls in the UI | `requestSave()` in the domain | D-010 |
| steps S1 to S15 in order | parallel work packages | section 15 |

The second idea: rules are enforced by the build, not by agreement (D-018, D-021, coverage gate D-003).

### 18.2 Decision log

The authoritative list is `docs/DECISIONS.md`; new decisions are added there.

| ID | Decision | Reason |
|---|---|---|
| D-001 | Java 17 is enforced with `options.release = 17` instead of a Gradle toolchain. | A toolchain fails on machines that only have a newer JDK unless an extra plugin downloads JDK 17. `release = 17` gives the same guarantee with any JDK 17+. |
| D-002 | The project skeleton is written by hand instead of generated with gdx-liftoff. | Same layout; avoids generated files nobody on the team understands. |
| D-003 | The 70 % coverage gate applies only to the packages listed in `coverageGatedPackages`. | Skeleton features have empty method bodies; a gate over everything would fail from day one. Each feature is added when it is implemented. |
| D-004 | `SaveData` is `(schemaVersion, time, sections)` with one `SaveSection(version, data)` per feature id. | With fixed fields every new feature must edit `SaveData` and the schema version. With sections a feature owns and versions its own data. Atomic write, `.bak`, `.corrupt` and refusing newer versions are unchanged. |
| D-005 | `SaveMigrator` works on any JSON element and is used for the file frame and for each section. | One mechanism to learn instead of two. |
| D-006 | The `<millis>` in `save.json.corrupt-<millis>` is the last-modified time of the damaged file. | The save store has no `Clock`, and reading the system time is forbidden in the domain. |
| D-007 | New core packages `core.events` (`EventBus`) and `core.module` (`DomainFeature`, `FeatureContext`, `ServiceRegistry`). | They are the extension mechanism: a feature registers itself with one line instead of editing shared classes. |
| D-008 | `GameSession` has no field per service; services are looked up with `session.get(Type.class)`. The factory takes `CoreConfig` and the feature list. | Fixed fields would have to be edited for every new feature. |
| D-009 | No single `GameConfig`; `CoreConfig` plus one config record per feature. | One shared config file would be edited by everyone. Values are unchanged. |
| D-010 | The UI does not call `saveNow()` after each action; domain code calls `requestSave()`. | Saving is a domain concern; a forgotten call in one screen would lose data. |
| D-011 | Domain packages are `domain.core.*` and `domain.feature.<name>.*`. | Makes the boundary between the shared core and the independent features visible and checkable. |
| D-012 | The window title is "Mini-Aquarium", not "Mini-Akvaryum". Package and project names keep `miniakvaryum`. | It is the spelling of the player-facing language; see D-025. |
| D-013 | One `...Strings` class per feature plus `CoreStrings`. | A single class would be edited by everyone. |
| D-014 | `Wallet` has a second constructor with a listener called after every recorded transaction. | Lets `EconomyFeature` publish `LedgerChanged` and request a save while `Wallet` stays free of core dependencies. |
| D-015 | `Wallet` keeps running balances in memory, rebuilt from the entries on load. | The HUD reads balances every frame and the ledger grows with every pellet. Balances are still never saved. |
| D-016 | Service constructors take the feature's config instead of `GameConfig`, and where needed an `EventBus` and an `onChange` callback. `PinService.state()` returns `Optional<PinState>`. | Consequence of D-007, D-009 and D-010. |
| D-017 | `fish`, `food` and `world` are sub-packages of one feature `aquarium`. | They depend on each other in both directions, so they cannot be separate features. |
| D-018 | `FishState` and `Pellet` have public getters and package-private setters; `FishEntity` changes only through `recordFeeding()` and `addAquariumMillis()`. | Enforces "the UI never edits domain objects" by the compiler. |
| D-019 | Skeleton methods throw `UnsupportedOperationException("TODO: ...")`, except `AquariumWorld.update`, which does nothing. | `update` runs every frame; throwing there would make the game unstartable for everyone. |
| D-020 | `PinService.setPin` refuses to replace an existing PIN. | Changing the PIN is out of scope. |
| D-021 | Architecture rules are checked by plain JUnit tests that scan the source files. | No extra dependencies are allowed. |
| D-022 | The UI has the same plug-in structure as the domain; `MainScreen` contains no feature code. | Lets several people add buttons, status texts, layers and screens without editing a shared screen class. |
| D-023 | `PlaceholderTextures` is split into `ShapeTextures` (core) and `AquariumTextures` (aquarium UI). | The core must not know fish; later features need placeholder shapes too. |
| D-024 | UI actions run through `BaseScreen.guarded`. | Keeps the game usable for everyone while features are still being written. |
| D-025 | All user-visible text is English, not German. | Team decision. Because every text is in a `Strings` class, switching the language means editing only those classes. |

### 18.3 Known limits

- Inside `aquarium`, the `FishBrain` tests need a working `FoodField`. Implement `FoodField` first.
- Two later features, sleep at 21:00 and bubbles on tap, change how a fish chooses its action and will need a small change inside `FishBrain`. No extension point was built for them on purpose (no placeholders).
- On Windows and macOS the boot id is unknown, so the time between two sessions is always measured with the wall clock and protected by the 24 h cap only.

## 19. Glossary

| UI term | Code name | Notes |
|---|---|---|
| Task | task | Status ACTIVE, PENDING, APPROVED |
| Parent area | parent area | PIN protected; `ParentSession` |
| Food | food | A currency in the ledger |
| Pellet | pellet | One piece of food in the water |
| Coin | coin | A currency in the ledger |
| Game time | game time | Trusted clock counter |
| Credited gap | credited gap | Offline time accepted at start-up, at most 24 h |
| Claim | claim | A fish claims a pellet when it starts eating |
| Feature | feature | One self-contained piece of rules (`DomainFeature`) with its UI (`GameFeature`) |
| Save section | section | The part of the save file that belongs to one feature |
| Skeleton | skeleton | Wired and documented code whose rule methods are still `TODO` |
| Trophy road | trophy road | Not in this milestone |
