# Work packages

What is left to write after the base, split so that six people can work at the same time.
Each package lists its files, the `TODO` methods and the waiting tests. "Needs" names what
must exist before the package can be finished; where a test helper removes the dependency, it
is named.

| # | Package | Rules (domain) | UI (game) |
|---|---|---|---|
| 1 | Parent PIN | `PinService`, `ParentSession.unlock` | `ParentGateScreen` |
| 2 | Tasks | `TaskService` | `ChildTasksScreen`, `ParentScreen` |
| 3 | Food | `FoodField`, `FoodService` | food display "stock (free)" |
| 4 | Fish behavior | `LevelRules`, `FishBrain` | |
| 5 | World and fish drawing | `AquariumWorld` | `FishActor`, `FishAnimationMath` |
| 6 | Integration | demo script, CI, repository settings | review of all screens |

## 1. Parent PIN

- Files: `domain/feature/security/api/PinService.java`, `ParentSession.java`;
  `game/feature/security/ParentGateScreen.java`.
- TODO methods: `PinService.setPin`, `PinService.verify`, `ParentSession.unlock`.
- Tests: `PinServiceTest` (13), `ParentSessionTest` (2 waiting).
- Needs: nothing.
- Done when: demo script steps 2 and 4 work (set PIN 1234, five wrong PINs lock for 30 s).

## 2. Tasks

- Files: `domain/feature/tasks/api/TaskService.java`;
  `game/feature/tasks/ChildTasksScreen.java`, `ParentScreen.java`.
- TODO methods: `create`, `update`, `delete`, `reportDone`, `approve`, `reject`.
- Tests: `TaskServiceTest` (14).
- Needs: nothing for the rules (`ParentSessions.forceUnlock` opens the parent area in tests).
  The parent screen can be reached in the game only when package 1 is done.
- Done when: demo script steps 2 to 4 work (create, report, approve pays 10 coins and 2 food once).

## 3. Food

- Files: `domain/feature/aquarium/api/food/FoodField.java`, `FoodService.java`;
  `game/feature/economy/EconomyGameFeature.java` (food display).
- TODO methods: `FoodField.nearestFree`, `tryClaim`, `release`, `remove`, `update`;
  `FoodService.available`, `drop`, `completeEating`.
- Tests: `FoodFieldTest` (12), `FoodServiceTest` (13).
- Needs: nothing. **Do `FoodField` first**: package 4 needs it for its tests.
- Done when: with feed mode on, a click drops a pellet that sinks and dissolves after 60 s
  without costing food. (Feed mode and pellet drawing exist already.)

## 4. Fish behavior

- Files: `domain/feature/aquarium/api/fish/LevelRules.java`, `FishBrain.java`.
- TODO methods: `LevelRules.levelOf`, `FishBrain.update`.
- Tests: `LevelRulesTest` (6), `FishBrainTest` (18).
- Needs: `FoodField` from package 3 for the `FishBrain` tests. Start with `LevelRules`
  (needs nothing) and the movement tests that use no pellets.
- Done when: all tests are green. Visible in the game once package 5 has `AquariumWorld.update`.

## 5. World and fish drawing

- Files: `domain/feature/aquarium/api/world/AquariumWorld.java`;
  `game/feature/aquarium/FishActor.java`, `FishAnimationMath.java`.
- TODO methods: `AquariumWorld.update`, `dropPellet`; `FishAnimationMath` (3 methods);
  tail, fin, pivots and animation in `FishActor`.
- Tests: `AquariumWorldTest` (7), `FishAnimationMathTest` (3).
- Needs: `dropPellet` needs only `FoodService.drop`; `update` needs packages 3 and 4.
  `FishActor` and `FishAnimationMath` need nothing: start there.
- Done when: three four-part fish swim, turn by mirroring and animate (spec step S11), and a
  dropped pellet is eaten by exactly one fish (spec step S12).

## 6. Integration

- Create the GitHub repository, protect `main` (pull request required, one approval, the
  `build` check required).
- Review pull requests for the rules in `ADDING_A_FEATURE.md`.
- When packages 1 to 5 are merged: run the demo script of the specification (section 13),
  all seven steps, and fix what does not match.
- Keep `README.md` and `docs/DECISIONS.md` current.

## Suggested order

```
day 1    1: PinService        2: TaskService     3: FoodField        4: LevelRules     5: FishActor, math
then     1: gate screen       2: both screens    3: FoodService      4: FishBrain      5: AquariumWorld
last     6: demo script with everything merged
```

## Later features (not part of the base)

Shop and inventory, energy, mood, dirt and cleaning, bubbles, sleep at 21:00, trophy road,
profile, minigame, music. Each becomes its own feature as described in `ADDING_A_FEATURE.md`.
Two of them will need a small change inside `FishBrain` (sleep and bubbles change how a fish
chooses its action); all others plug in through services, events and the main screen.
