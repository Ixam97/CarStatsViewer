# Plans

Implementation plans for changes to the app. Plans for UI features target the **CarCompose UI**
(`carCompose/…`), which is active in both AAOS flavors (`legacy`, `carapp`) via
`CarComposeActivity`. The old View-based activities (`ui/activities/…`) and the Car App
Library screens (`src/carapp/…/carApp`) are not extended unless explicitly stated.

| # | Feature | Plan | Effort (rough) | DB migration |
|---|---------|------|----------------|--------------|
| 4 | **Bugfix:** `loadSessionsToMemory()` loads oldest instead of newest points; memory buffer for the 100 km/mi plot | [04-fix-load-sessions-to-memory.md](04-fix-load-sessions-to-memory.md) | S | no |

## Relevant facts from the codebase

- A `DrivingPoint` is written every `Defines.PLOT_DISTANCE_INTERVAL` = **25 m**
  (`DataProcessor.speedUpdate()` → `updateDrivingDataPoint()`), with
  `energy_delta` in **Wh**, `distance_delta` in **m**, `state_of_charge` as a fraction 0..1,
  optional `lat/lon/alt` and `point_marker_type` (`PlotLineMarkerType`: 1 = BEGIN_SESSION,
  2 = END_SESSION, 3 = SINGLE_SESSION, cf. the filter `point_marker_type == 2` in
  `MapboxInterface.createStateFromTrip`).
- Live values: `CarStatsViewer.dataProcessor.realTimeDataFlow` (`RealTimeData`:
  `batteryLevel` in Wh, `stateOfCharge` 0..1, `speed` in m/s, `power` in mW),
  battery capacity in `dataProcessor.staticVehicleData.batteryCapacity` (Wh).
- Room DB `TripDataDatabase` version 6, migrations in `CarStatsViewer.kt`
  (`MIGRATION_5_6`). No exported schema. Robolectric unit tests live in `automotive/src/test` (since Plan 4).
- Preferences: `AppPreferences` with `AppPreference<T>` delegates backed by SharedPreferences.
- Map: only in builds with `useMapbox=true` (`src/mapbox/java`), otherwise a dummy
  (`src/mapboxdummy/java`). Rendering via Mapbox Compose layers in
  `MapTripAnnotations.kt`.
- Strings: `res/values*/strings.xml` (en, de and others – maintained via Lokalise).
