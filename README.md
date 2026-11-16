# Mini-Aquarium

A game for children aged 7 to 11. Parents create real-life tasks in a PIN-protected parent
area, the child reports a task as done, the parent approves it, and the reward (coins and fish
food) is paid once. The child feeds the fish in a virtual aquarium. Everything works offline.

Code, comments and everything the player sees are in English.

## Getting started

You need a **JDK 17 or newer** (not just a JRE). Check with `javac -version`.
If it is missing, install "Temurin 17 (LTS)" from https://adoptium.net and tick
"Set JAVA_HOME variable" in the installer. Gradle itself needs no installation.

| What | Windows (PowerShell) | macOS / Linux |
|---|---|---|
| Build and run all checks | `.\gradlew.bat build` | `./gradlew build` |
| Run the tests only | `.\gradlew.bat test` | `./gradlew test` |
| Start the game | `.\gradlew.bat :desktop:run` | `./gradlew :desktop:run` |

The first run downloads Gradle and the libraries and takes a few minutes.

While `:desktop:run` shows `EXECUTING`, the game is running; the command ends when you close
the window. Press **F3** in the game for the debug overlay.

### If it does not start

| Message | Cause | Fix |
|---|---|---|
| `An unexpected error occurred while trying to open file ...gradle-wrapper.jar` | The project path contains non-English letters (for example `Masaüstü`) or lies inside OneDrive. | Move the project to a plain path such as `C:\dev\mini-akvaryum`. |
| `No Java compiler found, please ensure you are running Gradle with a JDK` | Only a JRE is installed. | Install a JDK (see above) and open a new terminal. |
| `Cannot move item because the item ... is in use` | The terminal or an editor is inside the folder you are moving. | `cd C:\` first, close the editor, then move. |
| `'Windows:' is not recognized` or similar | Text after the command was pasted too. | Type only the command from the table. |

### Save file

The game saves to `.mini-akvaryum/save.json` in your user folder
(`C:\Users\<you>\.mini-akvaryum\`). Delete that folder to start a new game. To use another
folder, start with `-Dakvaryum.home=<path>`.

## How the project is organized

```
mini-akvaryum/
  domain/     game rules and state. Plain Java, no libGDX. Fast unit tests.
  game/       everything visible: screens, drawing, input (libGDX).
  desktop/    opens the window on a laptop; real clock.
  assets/     optional images; without them placeholders are drawn in code.
  docs/       SPECIFICATION.md, DECISIONS.md, ADDING_A_FEATURE.md, WORK_PACKAGES.md
```

Dependencies go one way only: `desktop -> game -> domain`. Gradle enforces it: the `domain`
module is not given libGDX, so UI code cannot even compile there.

### Core and features

Each of `domain` and `game` has a small **core** and a number of **features**.

```
domain/.../domain/
  core/                        knows no game concept; changes rarely
    util/     Vec2, RandomSource, IdGenerator
    time/     Clock, GameTime (trusted game time)
    save/     SaveStore, JsonFileSaveStore, SaveMigrator
    events/   EventBus
    module/   DomainFeature, FeatureContext
    session/  GameSession, GameSessionFactory
  feature/
    economy/   Wallet (coins, food) as a ledger          COMPLETE - the reference example
    security/  parent PIN, ParentSession                 complete
    tasks/     TaskService                               complete
    aquarium/  fish, food pellets, world simulation      complete except LevelRules
  DomainFeatures.java          the list of features: one line each

game/.../game/
  core/        GameFeature, UiContext
    ui/        MainScreen (empty frame), Navigator, BaseScreen, UiSkin, ShapeTextures
  feature/
    economy/   coin and food display                     complete
    aquarium/  water, sand, fish, pellets, feed mode     complete
    security/  PIN gate screen                           complete
    tasks/     child task list, parent area              complete
  GameFeatures.java            the list of UI features: one line each
```

A **feature** is one self-contained piece of the game. It connects to the rest in exactly one
place, its `...Feature` class, and is switched on by one line in `DomainFeatures`
(rules) and one line in `GameFeatures` (UI). Delete the two lines and the game runs without it.

Inside a domain feature:

```
feature/tasks/
  api/               what other features and the UI may use: service, records, enums, events
  internal/          optional helpers nobody else may import
  TasksConfig.java   the tunable numbers of this feature
  TasksFeature.java  reads its save section, builds the service, publishes it, saves
```

### How features talk to each other

- **You need something from another feature: call its service.**
  `ctx.require(Wallet.class)` in the domain, `ui.session().get(Wallet.class)` in the UI.
  Only the `api` package of the other feature may be used.
- **You only announce that something happened: publish an event.**
  `events.publish(new TaskApproved(task))`. Whoever is interested subscribes; the publisher
  never changes when listeners come or go.

### What happens at start-up and in every frame

1. `DesktopLauncher` creates `MiniAkvaryumGame` with the real clock.
2. `GameSessionFactory` loads the save, starts the trusted game time and calls `install` on
   every domain feature in list order. Each feature gets its own section of the save file.
3. The UI core builds the empty main screen; every UI feature adds its buttons, status texts,
   drawing layers and screens.
4. Every frame: `session.tick(dt)` advances the rules, then the active screen draws. The UI
   reads domain state and changes it only by calling services.
5. Saving: domain code calls `requestSave()` after a change that must not be lost; the session
   writes once at the end of the frame, every 30 seconds, and when the game closes.

### Things that keep the code honest

| Rule | Enforced by |
|---|---|
| `domain` has no UI code | Gradle (no libGDX on its classpath) |
| Time only from `GameTime`, randomness only from `RandomSource` | `ArchitectureTest` |
| The core knows no feature | `ArchitectureTest`, `GameArchitectureTest` |
| Features use each other only through `api`; UI features never import each other | same tests |
| The UI cannot move a fish or change a pellet | package-private setters (compiler) |
| At least 70 % of the lines of finished packages are tested | JaCoCo gate in `domain/build.gradle` |
| All of the above on every pull request | `.github/workflows/build.yml` |

## Current state

The base is complete: the game starts, shows the aquarium with three fish, saves and loads,
and all four features are installed. Packages 1 (parent PIN), 2 (tasks), 3 (food rules) and
5 (world and fish drawing) are written. Still open: `LevelRules.levelOf` (package 4) and the
food display "stock (free)" (package 3, `EconomyGameFeature`). What is still to be written is
marked `TODO` in the code and listed in [docs/WORK_PACKAGES.md](docs/WORK_PACKAGES.md). Calling
unfinished code from the UI shows "Not implemented yet" instead of crashing.

- The full specification of the project as built (version 2.0): [docs/SPECIFICATION.md](docs/SPECIFICATION.md)
- How to add or finish a feature: [docs/ADDING_A_FEATURE.md](docs/ADDING_A_FEATURE.md)
- Why something differs from the specification: [docs/DECISIONS.md](docs/DECISIONS.md)

## Reports

After `build`, open in a browser:

- Test results: `domain/build/reports/tests/test/index.html`
- Coverage: `domain/build/reports/jacoco/test/html/index.html` (red lines are not tested)
