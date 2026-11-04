# COMP2000 — Traffic Simulation

A Java OOP group project simulating road traffic with vehicles, intersections, and traffic lights.

This fork contains the Week 13 individual extensions by **Rongrui Guo (48977152)**, built on the original group project. The original team credits are retained below. Individual additions include simulation controls, fixed/adaptive signal timing selection, statistics recording, CSV export and regression tests.

[Individual repository](https://github.com/rongruiguo-cell/COMP2000---Object-Oriented-Programming) · [Original group repository](https://github.com/CalHo1/COMP2000---Object-Oriented-Programming)

## Team

The following table is preserved from the original group README and records the original project responsibilities.

| Name | Pair | Responsibility |
|------|------|----------------|
| Ali | A | Core Engine + Observer pattern |
| Henry | A | Core Engine + Observer pattern |
| Oscar | A | Core Engine + Observer pattern |
| Callum | B | Road Network + Factory pattern |
| Jubril | B | Road Network + Factory pattern |
| Ben | C | Vehicles |
| Jacob | C | Vehicles |
| Addrita | D | Traffic Lights + Intersections |
| Cam | D | Traffic Lights + Intersections |

## Project Structure

```
src/trafficsim/
  Main.java                  <- GUI/headless entry point; loads networks/grid.txt if present
  ComparisonRunner.java      <- sequential fixed/adaptive policy experiments
  SimConstants.java          <- central tuning knobs
  engine/                    <- SimulationEngine, SimulationObserver, Statistics,
                                SensorReading, TrafficSensor, VehicleSpawner,
                                MetricsRecorder, CsvExporter, SimulationMetrics, VehicleMetric
  view/                      <- SimulationDisplay and SimulationControls toolbar
  model/
    road/                    <- RoadNetwork, Road, Lane, BusStop, Intersection (sealed),
                                SignalisedIntersection
    vehicle/                 <- Vehicle (sealed abstract), Car, Truck, Bus,
                                EmergencyVehicle, DriverProfile
    light/                   <- TrafficLight, LightState, RedState, GreenState, YellowState
  factory/                   <- NetworkLoader
  strategy/                  <- WeightedRandom<T>, SignalTimingStrategy,
                                AdaptiveSignalTiming, FixedSignalTiming
  util/                      <- Direction, LightPhase, Axis enums
  exception/                 <- SimulationException, InvalidNetworkException
networks/
  grid.txt                   <- default demo network (3x3 signalised grid)
tests/                       <- regression tests, baseline trace and preview renderer
docs/                        <- worksheet, logbook template, diagrams and validation reports
```

## How to Run

Requires **JDK 17 or newer**, with `java` and `javac` available on `PATH`. Only Java standard-library classes are used; no external dependencies are required. If downloading a ZIP, extract it fully before running.

From the repo root:

**Command Prompt (Windows):**
```bat
build.bat
run.bat
```

**PowerShell (Windows):**
```powershell
./build.bat
./run.bat
```

**macOS / Linux:**
```sh
sh build.sh
sh run.sh
```

The build produces `dist/traffic-simulation.jar`. Generated `build/` and `dist/` files are excluded from Git. The run script automatically builds a missing JAR; after changing source files, rebuild before running again. Windows users can also double-click `run.bat`.

After building, launch a repeatable run with:

```text
java -jar dist/traffic-simulation.jar --seed=48977152
```

Optional args:
- `<network-file>` — path to a network definition (defaults to `networks/grid.txt`, with a built-in fallback)
- `--seed=<long>` — deterministic RNG seed for spawning and driver slowdown
- `--timing=adaptive` or `--timing=fixed` — signal timing policy; adaptive is the default
- `--spawn=<number>` — arrival probability per tick between 0 and 1; default 0.06
- `--headless` — run without a desktop window
- `--ticks=<integer>` — headless duration; default 3000 ticks
- `--csv=<path>` — export headless statistics

The GUI retains the original 1100×700 top-down city view, with an additional toolbar. Press `Space` to pause or resume, and `N` to advance one tick while paused. The toolbar also allows signal timing selection, arrival probability adjustment and CSV export.

The dashboard displays the generated run seed in hexadecimal; the command-line seed uses decimal notation. Launching normally creates a new pattern; passing `--seed=<long>` deliberately repeats one.

## Design Patterns

The table below retains the original project concepts and submission-stage labels. Observer, State and the original stream/threading code already existed in the group project; their presence is not claimed as a new individual contribution.

| Concept | Where | Submission stage |
|---------|-------|--------|
| Inheritance | `Vehicle` (sealed) → `Car` / `Truck` / `Bus` / `EmergencyVehicle` | Week 7 |
| Generics | `WeightedRandom<T>` | Week 7 |
| Exceptions | `SimulationException` → `InvalidNetworkException` | Week 7 |
| Observer | `SimulationObserver` / `SimulationDisplay`; added `MetricsRecorder` | Week 13 |
| State | `LightState` / `RedState` / `GreenState` / `YellowState` | Week 13 |
| Factory | `NetworkLoader` (file + built-in default) | Week 13 |
| Strategy | Added `SignalTimingStrategy` with adaptive and fixed implementations | Week 13 individual extension |
| Streams / lambdas | `Statistics`, network parsing, vehicle cleanup; added custom statistics collector | Week 13 |
| Threading | Background simulation loop with lock-protected complete updates | Week 13 |
| Sealed hierarchies | `Vehicle`, `Intersection` | Modern Java |
| Records | `SensorReading`; added immutable metric records | Modern Java |

## Features

- **Straight-through traffic** — vehicles stay in their lane through every junction
- **Collision avoidance** — vehicles keep a safe same-lane gap and wait while conflicting traffic clears a junction
- **Emergency priority** — siren-on vehicles may pass red lights but wait for occupied crossings; conflicting traffic yields only when the emergency approach is clear
- **Bus service** — buses dwell briefly at Central Station, Museum, or University when the stop serves their lane, then continue
- **Random traffic** — production runs randomise spawn timing, boundary lane, vehicle type, driver profile, cargo weight, and slowdown behaviour; `--seed` makes it reproducible
- **Smooth updates** — the simulation advances atomically at 30 updates per second so rendering never sees a half-updated world
- **Adaptive signal timing** — extend green if queue exceeds threshold
- **Australian lane placement** — vehicles spawn and travel on the left side of the road

## Week 13 Individual Extensions

The original 33 production Java files are retained at their existing paths: 27 are unchanged, six are modified, and nine new production files are added. The original map, vehicle hierarchy, bus stops, sensors, emergency-vehicle rules and signal State classes are preserved.

- **Interactive controls** — toolbar buttons for pause/resume and single-step execution, plus timing and arrival-probability controls.
- **Signal timing strategy** — the original adaptive green-extension rule is extracted into `AdaptiveSignalTiming`; `FixedSignalTiming` provides a comparison policy.
- **Metrics and CSV** — a new `MetricsRecorder` observer collects immutable snapshots for export and reporting.
- **Stream collector** — aggregates vehicle count, average and maximum speed, stopped vehicles and per-type statistics.
- **Repeatable experiments** — headless runs and `ComparisonRunner` compare fixed and adaptive policies across seeds and demand levels.
- **Regression tests** — cover original behaviour, new controls, metrics, collector results and simulation lifecycle.

The simulation updates remain sequential. Parallel reduction is tested only on copied immutable statistics. Complete experiments run sequentially because the original vehicle model shares a static random source.

## Testing and Experiments

Run tests from an open terminal so the results remain visible:

**Command Prompt:**
```bat
test.bat
```

**PowerShell:** `./test.bat`

**macOS / Linux:** `sh test.sh`

The recorded Linux / OpenJDK 17 validation passed **10 test groups and 769,104 assertions**. Most assertions are repeated invariant checks across ticks, not distinct test cases. Default vehicle and signal traces matched the original source for three seeds over 3,000 ticks each. See [the test report](docs/test-report.md) for the recorded environment and coverage limits.

The project owner also reported successful GUI operation and test execution on Windows with Temurin JDK 17 on 4 November 2026; the archived detailed report covers the earlier Linux validation.

After building:

```text
java -jar dist/traffic-simulation.jar --headless --ticks=3000 --seed=48977152 --csv=metrics.csv
java -jar dist/traffic-simulation.jar --headless --ticks=5000 --seed=42 --timing=fixed --spawn=0.14
java -cp dist/traffic-simulation.jar trafficsim.ComparisonRunner 5000 comparison.csv
```

The comparison runs three seeds × three arrival probabilities × two policies, giving 18 sequential experiments. Adaptive timing does not outperform fixed timing in every scenario; see [the experiment summary](docs/experiment-summary.md).

## Documentation

- [Original README and team credits](docs/README-original.md)
- [Architecture](docs/architecture.md)
- [Source changes](docs/source-changes.md)
- [Observer, State and Strategy diagrams (PDF)](docs/design-patterns.pdf)
- [Week 13 worksheet](docs/worksheet-13.md)
- [Weeks 7–13 logbook template](docs/logbook-weeks7-13.md)
- [Test report](docs/test-report.md) and [baseline comparison](docs/baseline-verification.json)
- [Experiment results](docs/comparison.csv) and [sample statistics](docs/sample-statistics.csv)
- [Demo script](docs/demo-script.md)

The individual extension has been pushed to this fork. The worksheet's personal reflection and logbook still require actual activities, evidence and Week 7 feedback before assessment submission. The source-provenance file records hashes from preparation of the extension package; subsequent documentation edits may differ.

## Limitations

This is a simplified teaching simulation. Vehicles travel straight through intersections without turning or changing lanes. Speeds use simulation units per tick, not km/h. Mean completed-trip duration excludes vehicles still travelling. The tests cover selected behaviours and invariants, rather than proving all possible traffic interactions safe.

## Communication

Original group communication arrangement:

Discord — respond when possible, notify the team if you can't make a deadline.

## AI Use Acknowledgement

Generative AI tools were used to help review, explain, debug, and refine parts of the code and documentation. AI assistance was also used to prepare the individual extensions, tests and documentation. The submitting student remains responsible for checking the work, understanding and explaining it, completing personal reflections accurately and following the unit's disclosure requirements.
