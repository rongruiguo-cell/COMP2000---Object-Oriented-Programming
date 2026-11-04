# Week 13 architecture - extension of the supplied repository

This version starts from `COMP2000---Object-Oriented-Programming-main.zip`. The original 33 Java files remain at their original paths. Six are changed and nine production Java files are added. Twenty-seven original Java files are byte-for-byte unchanged. This is a revision of the supplied implementation; it does not use the earlier independently rebuilt 2-by-2 simulation.

## What stays the same

The map still contains six roads and nine signalised intersections, with Central Station, Museum and University bus stops. The original vehicle hierarchy, directional lane placement, sensor rules, signal State classes, factory loader and weighted selection are retained. The engine still updates signals, reads every sensor, moves all vehicles, removes departures and then attempts a spawn. It still runs at 30 updates per second in the desktop application.

The original emergency rule is retained: siren-on emergency vehicles are exempt from red lights but follow the sensor's occupied-junction and emergency-priority rules. Do not describe this version as requiring emergency vehicles to wait for a priority green. The State classes retain their timer-based all-red interval; clearance checks are in `TrafficSensor`, not in `RedState`.

## What the individual extension adds

| Area | Implementation |
|---|---|
| Timing Strategy | `SignalTimingStrategy`, `AdaptiveSignalTiming`, `FixedSignalTiming`; context is `SignalisedIntersection` |
| Statistical reduction | Original `Statistics` methods retained; immutable `VehicleMetric` samples and a custom `Collector` added |
| Published metrics | `SimulationMetrics` is stored in a volatile engine field after a complete tick |
| A second Observer | `MetricsRecorder` implements the original no-argument `SimulationObserver` interface |
| CSV | `CsvExporter` writes a copied list of metric records, including type counts and completed-trip measurements |
| Controls | `SimulationControls` adds pause, one-step, policy switching, arrival probability and export above the original 1100-by-700 renderer |
| Repeatable evaluation | Headless CLI and sequential `ComparisonRunner`, plus tests and source-provenance evidence |

The adaptive Strategy is an extraction of the previous rule: during the last four ticks of green, if at least four slow vehicles are counted near that green axis and fewer than three extensions have been used, extend by twelve ticks. The existing proximity-based queue counter is also retained. Fixed timing returns no extension. Policy changes retain the phase timer and the extension count, so repeatedly switching modes cannot reset the current phase's budget.

## Threading and snapshots

The original background simulation worker remains. A complete tick holds `synchronized (network)`. The original renderer takes the same lock while reading the model. Pause, single-step, demand changes and policy changes use this lock too. The worker lifecycle now rejects duplicate `run()` starts and is explicitly stopped when the window closes.

While holding the network lock, the engine copies vehicle type and speed into `VehicleMetric` records and reduces them. The finished `SimulationMetrics` record contains only scalars and an immutable statistics map. Publishing it through a volatile field makes a completed record visible to the UI and observers. Observers are notified after the tick lock is released. `CopyOnWriteArrayList` allows registration/removal without corrupting an in-progress notification traversal.

`MetricsRecorder` synchronises its own short list operations. It does not take the network lock; it reads the published metrics record. CSV export takes an immutable copy of the recorder's list. The GUI retains up to 6000 regular samples, one per ten ticks, and appends the latest tick on export if necessary. Long sessions can therefore export only the retained history.

The custom collector creates an independent accumulator for each partition. Its merge operation combines counters, speed sums, maximum speed and per-type buckets; it does not share a mutable bucket with the other accumulator. The collector is not marked `CONCURRENT`. The parallel test reduces copied immutable values, never live vehicles. Floating-point addition order can differ, so tests use a tolerance.

The production simulation uses sequential statistics for at most forty vehicles. Complete comparison runs are also sequential because the original `Vehicle` has a static process-wide driver-noise generator. Concurrent simulations in one JVM would share that generator and compromise seeded independence. A future change could make noise an engine-owned dependency. This limitation is explicitly retained rather than silently changing the original motion code.

## Original behaviour regression

For seeds 1, 42 and 48977152, both the unmodified original and this version were run for 3000 ticks at the original default arrival probability. A SHA-256 trace of ordered vehicle types, positions, speeds and every signal phase, axis and timer matched for each run. The results are in `baseline-verification.json`; `tests/BaselineTrace.java` contains the shared trace program.

This provides evidence for the tested default runs. It does not prove equivalence for every possible network or input. Fixed timing and changed arrival probabilities are deliberate new options.

## Modelling limits

Roads are straight and vehicles do not turn or change lanes. Motion uses abstract units per tick and simplified immediate braking. The inherited geometric sensor heuristics are an educational model, not a formal traffic-safety proof. The population cap is forty and busy entry lanes can reject spawn attempts. A higher requested arrival probability does not guarantee proportionally more admitted vehicles. Completed-trip means exclude unfinished journeys and should be read together with queue measurements.
