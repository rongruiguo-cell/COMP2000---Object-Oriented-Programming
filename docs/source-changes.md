# Changes from the uploaded original repository

Source: `COMP2000---Object-Oriented-Programming-main.zip`. This version was prepared on 4 November 2026 (Sydney date). The earlier independently rebuilt delivery has been superseded by this source-based revision.

**33 original Java files retained: 27 byte-for-byte unchanged, 6 modified; 9 Java files added. No original file was removed.** The original network text, team contract and draw.io file remain unchanged. The original README is also preserved as `README-original.md`; the root README now describes this extension.

## Six modified Java files

| File | Change |
|---|---|
| `src/trafficsim/Main.java` | Original network/seed/initial-spawn flow retained; adds headless flags, recorder and toolbar. |
| `src/trafficsim/engine/SimulationEngine.java` | Original movement/sensing order retained; adds metrics, departure tracking, model-control locking and lifecycle guards. |
| `src/trafficsim/engine/Statistics.java` | Retains old API; adds immutable samples and a multi-field custom collector. |
| `src/trafficsim/engine/VehicleSpawner.java` | Retains factory weights, entry selection and spawn order; adds a validated probability setter. |
| `src/trafficsim/model/road/SignalisedIntersection.java` | Extracts the existing extension rule behind a timing Strategy. |
| `src/trafficsim/view/SimulationDisplay.java` | Preserves city rendering; adds a metrics line and paused single-step key. |

## Nine added production Java files

- `src/trafficsim/ComparisonRunner.java`
- `src/trafficsim/engine/CsvExporter.java`
- `src/trafficsim/engine/MetricsRecorder.java`
- `src/trafficsim/engine/SimulationMetrics.java`
- `src/trafficsim/engine/VehicleMetric.java`
- `src/trafficsim/strategy/AdaptiveSignalTiming.java`
- `src/trafficsim/strategy/FixedSignalTiming.java`
- `src/trafficsim/strategy/SignalTimingStrategy.java`
- `src/trafficsim/view/SimulationControls.java`

## Unchanged original Java files

- `src/trafficsim/SimConstants.java`
- `src/trafficsim/engine/SensorReading.java`
- `src/trafficsim/engine/SimulationObserver.java`
- `src/trafficsim/engine/TrafficSensor.java`
- `src/trafficsim/exception/InvalidNetworkException.java`
- `src/trafficsim/exception/SimulationException.java`
- `src/trafficsim/factory/NetworkLoader.java`
- `src/trafficsim/model/light/GreenState.java`
- `src/trafficsim/model/light/LightState.java`
- `src/trafficsim/model/light/RedState.java`
- `src/trafficsim/model/light/TrafficLight.java`
- `src/trafficsim/model/light/YellowState.java`
- `src/trafficsim/model/road/BusStop.java`
- `src/trafficsim/model/road/Intersection.java`
- `src/trafficsim/model/road/Lane.java`
- `src/trafficsim/model/road/Road.java`
- `src/trafficsim/model/road/RoadNetwork.java`
- `src/trafficsim/model/vehicle/Bus.java`
- `src/trafficsim/model/vehicle/Car.java`
- `src/trafficsim/model/vehicle/DriverProfile.java`
- `src/trafficsim/model/vehicle/EmergencyVehicle.java`
- `src/trafficsim/model/vehicle/Truck.java`
- `src/trafficsim/model/vehicle/Vehicle.java`
- `src/trafficsim/strategy/WeightedRandom.java`
- `src/trafficsim/util/Axis.java`
- `src/trafficsim/util/Direction.java`
- `src/trafficsim/util/LightPhase.java`

## Behaviour check

The original and updated default adaptive versions were run for seeds 1, 42 and 48977152, each for 3000 ticks. Per-tick ordered vehicle positions/speeds/types and light phases/axes/timers were hashed; each pair matched. See `baseline-verification.json`. This validates those tested runs, not every possible input.

The individual changes should be described as extensions of the team's code. The original Observer, State, traffic sensors, vehicles and map should not be attributed entirely to this revision. The GitHub ZIP does not include `.git` history; integrate this content into the existing personal fork and make genuine commits. No commits, pushes, tutor feedback or logbook entries have been fabricated.
