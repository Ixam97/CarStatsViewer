# Plan 4: Bugfix `loadSessionsToMemory()` + Memory Buffer for the 100 km Plot

**Status: implemented.** Automated tests: Robolectric unit tests (`automotive/src/test/...`,
`TripDaoTest`, `DefinesTest`) instead of an instrumented test, so they run without an
emulator via `./gradlew :automotive:testDevLegacyDebugUnitTest`.

- **Part A:** `loadSessionsToMemory()` loads the oldest instead of the newest points.
- **Part B:** The memory buffer (50 km) does not cover the 100 km/100 mi option of the live plots.

## Part A: Oldest instead of newest points loaded

### Problem

`DataProcessor.loadSessionsToMemory()` (`dataProcessor/DataProcessor.kt:130`) loads the
driving points of every active session into memory:

```kotlin
session.drivingPoints = CarStatsViewer.tripDataSource.getDrivingPointsSince(
    startTime = loadedSession.start_epoch_time,
    limit = Defines.MAIN_VIEW_DRIVING_POINTS   // 2,000
)
```

The corresponding query (`TripDao.kt:25`):

```sql
SELECT * FROM DRIVINGPOINT WHERE driving_point_epoch_time >= :startTime
ORDER BY driving_point_epoch_time ASC LIMIT :limit
```

`ASC` + `LIMIT` returns the **first** 2,000 points since session start, not the last ones.
During normal operation, `updateDrivingDataPoint()` appends new points at the end and
trims the front to 2,000 (`DataProcessor.kt:559`). That part is correct.

**Introduced in** `498e41e` ("Optimizing the loading of DrivingPoints in the Data Processor
to limit the need of copying and memory usage", Aug. 2024). Before that, all points were
loaded via `getFullDrivingSession()`. The intention to limit to 2,000 points was right;
only the sort direction does not match the trimming.

#### When it occurs

`loadSessionsToMemory()` is called in:
- `checkTrips()` (`DataProcessor.kt:452`) – on **app/service start**,
- `resetTrip()` (`DataProcessor.kt:774`) – on **every trip reset**, including the
  automatic resets of "since charge" and "month". **All** active sessions are reloaded,
  not only the one that was reset.

Affected are sessions with more than 2,000 points (> ~50 km since session start). This
practically always applies to "month" and "manual", often also to "since charge".

#### Impact

After loading, the list contains `[points 1…2000 since session start]`. Then new points
are added and old ones drop off the front. Until 2,000 new points (≈ 50 km) have been
driven, the list consists of an **old block from the session start + the new points**,
without any visible separation.

Consumers of `selectedSessionDataFlow.value.drivingPoints` or `localSessionsState`:

| Location | Symptom |
|----------|---------|
| `ui/activities/MainActivity.kt:342ff`, `:904` (legacy main view, consumption plot) | after restart/reset, the "last 20/40 km" plot shows sections from the session start (e.g. from the beginning of the month) directly before the current data |
| `src/carapp/.../carApp/renderer/DefaultRenderer.kt:429` (Car App plot) | identical |
| `src/carapp/.../carApp/TabsScreen.kt:115` | indirectly via the renderer |

`DataConverters.consumptionPlotLineFromDrivingPoints()` walks the list from the end until
the distance is reached. The old block therefore becomes visible exactly when less than
20/40 km have been driven since loading, i.e. right after starting to drive.

Not affected: trip details/history (load all points via `getFullDrivingSession()`) and
the session totals (distance, energy, time). These are stored in the `DrivingSession`
record, not in the point list.

### Fix

#### 1. New DAO query: the newest N points, sorted ascending

`database/tripData/TripDao.kt`:

```kotlin
@Query("""
    SELECT * FROM (
        SELECT * FROM DrivingPoint
        WHERE driving_point_epoch_time >= :startTime
        ORDER BY driving_point_epoch_time DESC
        LIMIT :limit
    ) ORDER BY driving_point_epoch_time ASC
""")
fun getLatestDrivingPointsSince(startTime: Long, limit: Int): List<DrivingPoint>
```

- The inner query uses the primary key index backwards. The cost is identical to the
  current query.
- The outer sort restores the chronological order that all consumers expect (append at
  the end, `drop()` at the front, plot building).
- Room supports subqueries in `@Query` out of the box; the return type stays `DrivingPoint`.

#### 2. Adjust the data source

`TripDataSource.kt` / `LocalTripDataSource.kt`: add `getLatestDrivingPointsSince(startTime, limit)`.

The old method `getDrivingPointsSince` has **no other callers** (checked via `grep`).
Options:
- **Recommended:** remove it (DAO, interface, implementation). This way the misleading
  variant cannot be used again by accident.
- Alternatively keep it and clarify via KDoc: "returns the *oldest* `limit` points since `startTime`".

Not recommended: silently changing the semantics of the existing method. The name would
then no longer match the behavior.

#### 3. Switch the caller

`DataProcessor.loadSessionsToMemory()`:

```kotlin
session.drivingPoints = CarStatsViewer.tripDataSource.getLatestDrivingPointsSince(
    startTime = loadedSession.start_epoch_time,
    limit = Defines.MAIN_VIEW_DRIVING_POINTS
)
```

Additionally, a debug log line for verification (stays in, level `d`):

```kotlin
InAppLogger.d("[NEO] Loaded ${points.size} driving points for session ${loadedSession.driving_session_id} " +
    "(${points.firstOrNull()?.driving_point_epoch_time} … ${points.lastOrNull()?.driving_point_epoch_time})")
```

#### 4. Update comments

The KDoc comments in `loadSessionsToMemory()` / `updateDrivingDataPoint()` refer to each
other. Add one sentence: "The list always contains the newest `MAIN_VIEW_DRIVING_POINTS`
points in chronological order." This is the invariant both functions must maintain together.

## Part B: Enlarge the memory buffer for the 100 km/100 mi option

### Problem

The live plots offer 20 / 40 / **100** units as range
(`mainPrimaryDimensionRestriction`; legacy: `MainActivity.kt:367ff`, button "100 km" in
`activity_main.xml:371`; Car App: `DefaultRenderer.kt:290/389/433`, button in
`RealTimeDataTemplate.kt:142`). Memory, however, only holds
`MAIN_VIEW_DRIVING_POINTS = 2,000` × 25 m = **50 km**. The 100 option therefore never
shows more than 50 km, even after the fix from part A.

**Caution, miles:** the values are meant in *user units*.
`distanceUnit.asUnit(100_000L)` yields 100,000 × 1.60934 = **160,934 m** for `MILES`, and
the Car App button then reads "100 mi". The buffer must therefore cover **~161 km**, not 100 km.

### Required number of points

```
max. plot distance   = 100 × 1,609.34 m          = 160,934 m   (miles, worst case)
+ rebuild margin      (DataConverters: +200 m)    =     200 m
÷ point spacing       (PLOT_DISTANCE_INTERVAL)    =      25 m
                                                  ≈   6,446 points
+ ~10 % reserve                                   ≈   7,100 points
```

The reserve is needed because not every point covers a full 25 m. Every trip end,
parking event or reset creates a marker point with a partial distance
(`updateDrivingDataPoint(END_SESSION)`, `resetTrip()`). In city traffic with many stops,
the average distance per point drops.

### Implementation

1. **Centralize the range values.** The values `20_000L / 40_000L / 100_000L` are currently
   duplicated **five times** (1× `MainActivity`, 3× `DefaultRenderer`, plus labels). New in
   `Defines` (`CarStatsViewer.kt`):
   ```kotlin
   /** Selectable live plot ranges in user distance units (km or mi), as meters-equivalent of the unit. */
   val MAIN_VIEW_DISTANCES = listOf(20_000L, 40_000L, 100_000L)
   private const val MAX_UNIT_FACTOR = 1.60934 // miles
   private const val PLOT_REBUILD_MARGIN = 200.0
   /** Newest driving points kept in memory per active session; covers the largest range in miles plus 10 % reserve. */
   val MAIN_VIEW_DRIVING_POINTS: Int =
       ((MAIN_VIEW_DISTANCES.max() * MAX_UNIT_FACTOR + PLOT_REBUILD_MARGIN) / PLOT_DISTANCE_INTERVAL * 1.1).toInt()  // ≈ 7,090
   ```
   Replace the `when` blocks with `Defines.MAIN_VIEW_DISTANCES[mainPrimaryDimensionRestriction]`.
   This way the buffer can no longer fall behind a new range option by accident.
   (`const val` becomes `val` because the value is computed. All usages are runtime
   accesses, not annotations; this is harmless.)
2. **Nothing else** needs to change. Loading (`loadSessionsToMemory`, part A) and trimming
   (`updateDrivingDataPoint`, `DataProcessor.kt:559`) use the same constant.

### Does the rest of the in-memory behavior need to change?

**Functionally no.** There are exactly two places that determine the size of the point
lists, and both depend on `MAIN_VIEW_DRIVING_POINTS`:

| Location | Behavior | Change needed? |
|----------|----------|----------------|
| `loadSessionsToMemory()` | loads the newest N points (after part A) | no, constant |
| `updateDrivingDataPoint()` | appends, `drop(size − N)` at the front | no, constant |
| `MainActivity` / `DefaultRenderer`: plot building | walks from the end up to the selected distance | no, benefits directly |
| `MainActivity:363` incremental update | `withIndex().find` for the last plot point | no, see costs |
| Trip details, history, export | load all points from the DB | not affected |

The statement "only the last 50 km in memory" results solely from the constant.
After the change, ~161 km (miles) or ~177 km (km users, same number of points) are kept in memory.

**Costs that grow linearly with N (checked, not critical):**

- **Copying per data point:** `updateDrivingDataPoint()` copies the list of every active
  session on every new point (`toMutableList()` + `drop()`). That is 4 sessions ×
  ~7,100 references ≈ 57,000 reference copies, every ~0.7 s on the highway. This is in the
  range of tenths of a millisecond on `Dispatchers.IO` and negligible next to the DB upsert
  that runs at the same rate.
- **Linear search** in `MainActivity` (`withIndex().find`) per update: it starts at the
  beginning of the list, while the point it looks for is almost always at the end. With
  7,100 elements this is harmless. Trivial fix in the same PR: `indexOfLast { … }` instead
  of `withIndex().find`. It searches from the end and is effectively O(1).
- **Memory:** a `DrivingPoint` with boxed `Float?`/`Int?` fields takes an estimated
  ~100 B. After loading, every session has its own instances (4 separate queries):
  4 × 7,100 × 100 B ≈ **2.8 MB** in the worst case. Points added while driving already
  share one instance across all sessions, so the value decreases over time.
  Not critical for an AAOS head unit.

**Optional optimization (only if memory becomes relevant):** in `loadSessionsToMemory()`,
run only **one** query for the session with the earliest `start_epoch_time` and build the
lists of the other sessions as subsets of it (`points.filter { it.time >= session.start }.takeLast(N)`).
All sessions then share the same instances (≈ 0.7 MB), and there is 1 instead of 4 DB
accesses on start/reset. This works because active sessions share all ongoing points and
only differ in their start time.

### Alternatives considered

Measured against the estimated worst case (2.8 MB). The optional optimization above
(one query, shared instances) already reduces it to ~0.7 MB without changing behavior.

#### A1: Keep only the currently selected plot range in memory, load the rest on range change – rejected

Idea: couple N to `mainPrimaryDimensionRestriction` (20/40/100) and load the missing
points from the DB when switching to a larger range.

- **Small savings:** with the "100" setting (the case at hand) it saves nothing. With 20/40
  it saves only fractions of a MB compared to the shared-instances variant.
- **Shared state:** the lists live in the `DataProcessor` (service) and are read by two
  UIs (`MainActivity`, Car App `DefaultRenderer`). The `DataProcessor` would have to react
  to the preference, load asynchronously and prepend to the lists.
- **The plot would stay short:** the update logic in `MainActivity` (~lines 360–395,
  analogous in `DefaultRenderer`) only rebuilds if the last plot point is missing or
  > 100 new points were added. If points are prepended, the last point is still present,
  so only the small incremental update at the end runs. Both UIs would need an explicit
  rebuild path after loading.
- **Delay when switching:** today a click redraws immediately from memory. The Car App
  button cycles 20 → 40 → 100 → 20, so two out of three clicks would trigger a load.
- **Concurrency risk:** `loadSessionsToMemory()`/`updateDrivingDataPoint()` are already
  guarded by the busy-wait on `localSessionsAccess` and comments about
  `ConcurrentModificationException`. Another writer that loads independently of the point
  cadence increases the risk.

#### A2: Keep points only for the selected session – deferred (preferred if memory becomes relevant)

Idea: only the session that is `selectedSessionData` gets a point list. The other active
sessions only keep their totals (`drivingPoints = null`/empty).

- **~75 % savings** (1 instead of 4 lists), independent of the selected range.
- The lists of the non-selected sessions are currently only used by `changeSelectedTrip()`.
  That is a rare, deliberate user action during which both UIs fully rebuild the plot
  anyway (`neoSelectedTripId != session.driving_session_id` → reset). Briefly loading the
  points for the new session (`getLatestDrivingPointsSince`) is unproblematic there.
- Implementation: `loadSessionsToMemory()` loads points only for the selected session;
  `updateDrivingDataPoint()` only appends there; `changeSelectedTrip()` becomes `suspend`
  or starts a load job and only sets `selectedSessionData` after loading.
- Deferred because 2.8 MB or 0.7 MB (with shared instances) are not a problem on an AAOS
  head unit, and every change to this concurrent code carries risk. Implement it if
  profiling shows a memory problem.

#### A3: Distance-based trimming – rejected

Remove points from the front until the sum of `distance_delta` matches the target
distance, instead of using a fixed number of points. This would be more precise with many
stops, but requires a running sum per session and changes the invariant between loading
and trimming. Not worth it for a ~10 % reserve.

## Affected files

| File | Change |
|------|--------|
| `database/tripData/TripDao.kt` | new query `getLatestDrivingPointsSince`, remove the old one |
| `database/tripData/TripDataSource.kt` | adjust interface |
| `database/tripData/LocalTripDataSource.kt` | adjust implementation |
| `dataProcessor/DataProcessor.kt` | call in `loadSessionsToMemory()`, log line, comment |
| `CarStatsViewer.kt` (`Defines`) | `MAIN_VIEW_DISTANCES`, computed `MAIN_VIEW_DRIVING_POINTS` (part B) |
| `ui/activities/MainActivity.kt` | `when` blocks → `Defines.MAIN_VIEW_DISTANCES`, `indexOfLast` (part B) |
| `src/carapp/.../carApp/renderer/DefaultRenderer.kt` | 3× `when` block → `Defines.MAIN_VIEW_DISTANCES` (part B) |
| `CHANGES.md` | entries "Fixed: consumption graph showed old data after app restart or trip reset" and "Fixed: 100 km/mi graph range only showed 50 km" |

No DB schema change, no migration.

## Tests / verification

The project has no tests yet. Two levels:

1. **Manual (required)**, dev flavor in the emulator (`emulatorMode`) or in the car:
   - Create a session with > 2,000 points (drive > 50 km or simulate it in the emulator)
     or use an existing DB with a long "month" session.
   - Restart the app/service → check the log line: the last loaded timestamp must match
     the one from `getLatestDrivingPoint()` (before the fix: a timestamp shortly after session start).
   - Main view/Car App plot right after the restart: no data from the session start may
     appear before the current data.
   - Manually reset the "since charge" trip → the plot of the other trips (e.g. "month") stays correct.
   - **Part B:** after driving > 100 km (or > 100 mi with the miles setting) with a restart
     in between: the 100 option shows the full range, the log line reports ~7,090 loaded points.
     Plot updates while driving stay smooth (no stutter on point updates).
2. **Automated (optional, recommended)**: instrumented test with
   `Room.inMemoryDatabaseBuilder` (`automotive/src/androidTest/...`, dependencies
   `androidx.test.ext:junit`, `androidx.room:room-testing`): insert 3,000 points and call
   `getLatestDrivingPointsSince(start, 2000)`. Expected: size 2,000, first timestamp =
   point 1,001, last = point 3,000, sorted ascending. This would be the first test in the
   project and a basis for the DAO extensions from Plans 1 and 3.

## Risk

Low.
- **Part A** only changes which points are loaded. Format, size and order of the list stay
  the same. The only behavioral change is the intended one: after a restart/reset, memory
  holds the current data instead of the oldest.
- **Part B** increases the list size (2,000 → ~7,100 points per session) with the costs
  estimated above (copying, memory ≈ 2.8 MB worst case). Verify that plot updates stay
  smooth while driving (see manual tests).
