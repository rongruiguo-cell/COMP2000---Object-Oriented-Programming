# COMP2000 Worksheet 2 — Final Submission

**Student name:** Rongrui Guo

**Student ID:** 48977152

**GitHub repo URL:** https://github.com/rongruiguo-cell/COMP2000---Object-Oriented-Programming

**Version:** Week 13 individual extension of the uploaded original repository. This updated source has not been pushed to GitHub yet. The technical answers correspond to this package; the personal logbook and actual Week 7 feedback still require personal evidence.

**Pattern diagrams:** [design-patterns.pdf](design-patterns.pdf)

---

## 1. Design Patterns

### 1.1. Which design pattern(s) did you implement? Name them and state where each appears in your codebase.

The final project uses Observer, State and Strategy. Observer and State were already present in the shared starting code. My individual extension adds a second observer and an explicit, interchangeable signal-timing Strategy, while preserving the existing classes and map.

| Pattern | Participants | Role in this version |
|---|---|---|
| Observer | `engine/SimulationEngine`, `SimulationObserver`, `MetricsRecorder`; `view/SimulationDisplay` | The engine notifies the existing display and the new recorder after each completed tick. |
| State | `model/light/TrafficLight`, `LightState`, `GreenState`, `YellowState`, `RedState` | The original timer-based phase transitions remain unchanged. |
| Strategy | `strategy/SignalTimingStrategy`, `AdaptiveSignalTiming`, `FixedSignalTiming`; `model/road/SignalisedIntersection` | The intersection delegates its green-extension decision to a replaceable policy. |

All paths are under `src/trafficsim`. `factory/NetworkLoader` is an existing factory-style utility with two creation methods. I do not classify its static creation methods as a GoF Factory Method hierarchy.

### 1.2. For each pattern, paste a code snippet showing the key interaction between participants.

**Observer.** The original interface is retained:

```java
public interface SimulationObserver {
    void onSimulationStep();
}
```

`SimulationEngine` calls each observer after releasing the complete-update network lock:

```java
private void notifyObservers() {
    for (SimulationObserver observer : observers) observer.onSimulationStep();
}
```

`SimulationDisplay.onSimulationStep()` calls `repaint()`. The new `MetricsRecorder.onSimulationStep()` gets `engine.getMetrics()` and stores a sample at its configured interval. A volatile metrics field publishes an immutable completed-tick record, so the recorder does not need to read mutable vehicles. This adds another consumer without changing the observer method signature.

**State.** `TrafficLight.update()` delegates to the current state:

```java
public void update() {
    timer++;
    state.update(this);
}
```

The inherited `RedState` contains the transition back to green:

```java
if (light.getTimer() >= light.getRedDuration()) {
    light.swapAxis();
    light.setState(new GreenState());
}
```

Green changes to yellow after its duration; yellow changes to all-red; all-red swaps the active axis. These State classes are unchanged from the original project. The separate `TrafficSensor` handles conflicting traffic in occupied junctions. It would be inaccurate to claim that `RedState` itself checks junction occupancy.

**Strategy.** In `SignalisedIntersection.maybeExtendGreen()`:

```java
int remaining = light.getGreenDuration() - light.getTimer();
int extension = timingStrategy.extensionTicks(remaining, queuedOnGreenAxis(), extensionsThisPhase);
if (extension <= 0) return;
light.extendCurrentPhase(extension);
extensionsThisPhase++;
```

The adaptive implementation retains the original threshold logic:

```java
if (remainingGreenTicks > 4 || extensionsUsed >= SimConstants.ADAPTIVE_MAX_EXTENSIONS
        || queuedVehicles < SimConstants.ADAPTIVE_QUEUE_HIGH) return 0;
return SimConstants.ADAPTIVE_EXTEND_TICKS;
```

`FixedSignalTiming` always returns zero. The toolbar selects a strategy through `SimulationEngine.setTimingStrategy()`. Changing the strategy does not reset the light state, phase timer or extension budget.

### 1.3. For each pattern, explain why you chose it over an alternative. What trade-off did you make?

**Observer:** The existing interface is a useful extension point. Calling a recorder directly inside the engine would couple the engine to that recorder. Implementing a second observer keeps its sampling policy separate. The trade-off is listener registration and synchronous callbacks. Notifications still run on the caller's thread, so an expensive observer could slow down the simulation. The recorder does not perform file I/O in its callback.

**State:** I retained the team's State implementation. A switch over an enum would also work for three phases, but the small state classes keep each transition rule easy to locate. The cost is extra files. I did not move unrelated vehicle conflict decisions into these state classes.

**Strategy:** The previous adaptive calculation was embedded inside `SignalisedIntersection`. A boolean branch could select fixed or adaptive behaviour, but an interface makes policies independently testable and substitutable. The trade-off is three additional small files. The adaptive rule itself is preserved, making it possible to verify that default traffic traces remain unchanged.

---

## 2. Streams / Lambdas / Parallelism

### 2.1. List every file in your project that uses lambda expressions or the Streams API.

| Production file | Usage |
|---|---|
| `src/trafficsim/Main.java` | `SwingUtilities.invokeLater` lambda creates the GUI on the EDT. |
| `src/trafficsim/engine/SimulationEngine.java` | Lambda for the existing background simulation loop. |
| `src/trafficsim/engine/Statistics.java` | Existing `mapToDouble`, `average`, `filter` and `count`; new snapshot mapping, collector method references, bucket lambdas and reduction. |
| `src/trafficsim/engine/VehicleSpawner.java` | Existing constructor lambdas in weighted `Function` factories and a removal predicate for off-map vehicles. |
| `src/trafficsim/factory/NetworkLoader.java` | Existing streams parse directions and validate whether a bus stop serves a configured lane. |
| `src/trafficsim/view/SimulationControls.java` | Action/change listeners and a small Swing timer callback for the toolbar. |

`tests/trafficsim/tests/RegressionTests.java` also uses method references, lambdas, sequential streams and a parallel stream to verify reduction behaviour. `tests/trafficsim/tests/PreviewRenderer.java` uses an EDT lambda. `tests/BaselineTrace.java` uses ordinary loops.

Switch arrows in `Axis`, `Direction` and `NetworkLoader` are not themselves lambdas. `Lane` accepts a `Predicate<Vehicle>` but declares no lambda expression. `SimulationDisplay` retains anonymous `AbstractAction` classes rather than introducing lambda expressions.

### 2.2. For the most sophisticated use, paste the code and explain: what does it do, and why was a stream/lambda better than a loop?

The most substantial addition is a custom collector in `Statistics`:

```java
public static Collector<VehicleMetric, ?, Summary> summaryCollector() {
    return Collector.of(Accumulator::new, Accumulator::accept, Accumulator::merge, Accumulator::finish);
}
public static Summary summarise(List<VehicleMetric> metrics) {
    return metrics.stream().collect(summaryCollector());
}
```

The engine first copies type and speed while holding the network lock:

```java
return vehicles.stream()
        .map(v -> new VehicleMetric(v.getClass().getSimpleName(), v.getSpeed()))
        .toList();
```

The accumulator calculates count, speed sum, stopped count, maximum speed and separate count/speed buckets for each vehicle type. The finisher calculates averages and publishes an immutable map. The merge operation combines independent partial results:

```java
Accumulator merge(Accumulator other) {
    count += other.count; stopped += other.stopped; sum += other.sum;
    maximum = Math.max(maximum, other.maximum);
    other.byType.forEach((type, bucket) ->
            byType.computeIfAbsent(type, key -> new Bucket()).merge(bucket));
    return this;
}
```

A loop would be sufficient for one statistic. The collector is useful here because it packages a multi-field reduction with an explicit combiner and finisher. Several statistics are obtained in one pass, and the same definition can reduce an immutable list sequentially or in parallel. I do not claim it is faster for the small live population. The original simple `averageSpeed()` and `stoppedCount()` methods are retained for compatibility.

### 2.3. Did you use parallel streams, custom collectors, or the Fork/Join framework? If yes, how did you ensure thread safety? If no, where could parallelism be introduced?

I added a custom collector. A regression test uses `parallelStream()` on copied immutable `VehicleMetric` values and compares it with the sequential result. The production GUI uses the sequential collector, and the project does not directly create Fork/Join tasks.

Each collector partition has its own accumulator and type buckets. The combiner merges numbers into the receiving accumulator rather than sharing the other accumulator's mutable buckets. The collector is not marked `CONCURRENT`. The result map is copied to an immutable map. Floating-point means are compared with a tolerance because parallel addition can group values differently.

The existing simulation worker and renderer share the same network lock. New controls use that lock for model changes. Completed metrics are published through a volatile reference, and the recorder protects its own list. File writing uses an independent list of immutable metric records.

Complete comparison simulations run **sequentially**. The original `Vehicle` class has a static driver-noise `Random`; parallel simulation instances would share it. A future improvement would inject an engine-owned noise generator, then run independent experiments in parallel. Parallelising the current mutable world without first addressing ownership would be unsafe.

---

## 3. Uniqueness and Creativity

### 3.1. What did you add to your simulation that went beyond the in-class activities? Describe the feature and what inspired it.

The starting repository already contains a nine-junction traffic map, several vehicle types, three bus stops, emergency yielding rules and adaptive green extension. Those are retained shared-project features.

The individual extension makes the existing simulation easier to inspect and evaluate: it adds interchangeable fixed/adaptive policies, a multi-statistic collector, an independent metrics observer, CSV export, single-step controls, adjustable arrival demand, headless execution and repeatable comparisons. The motivating design question is whether the original adaptive extension actually improves a measurable outcome under different arrival demand.

The exact comparison with classroom exercises should be checked against my own activity records. `source-changes.md` and `source-provenance.json` distinguish inherited files from this extension so existing team work is not presented as newly added individual work.

### 3.2. Paste a code snippet or describe a design decision that demonstrates emergent behaviour, non-trivial complexity, or a genuinely surprising outcome.

Traffic queues emerge from the original combination of random arrivals, vehicle speeds, bus dwell, signals, following gaps and emergency yielding. There is no script that directly places a completed traffic jam on the map. The engine still reads all sensor observations before moving any vehicle, preserving a consistent pre-movement basis for each tick.

The extension makes those interactions measurable. It records completed vehicles, stopped counts, speed and vehicle-type summaries, then compares fixed and adaptive policies over repeated seeded runs. In the supplied 18-run comparison, each run lasts 5000 ticks. Mean completed trips were identical at the lowest arrival probability (137.67) and at the original probability (274.00). At probability 0.14, fixed timing averaged 404.67 completed trips and adaptive timing 403.00. These observations do not establish statistical superiority; they show that the original local adaptive rule is not automatically a large improvement.

This is also a useful limitation to discuss: the original threshold triggers only under certain queues, the population cap is forty, and denied spawn attempts affect realised demand. See `experiment-summary.md` and the raw `comparison.csv`.

### 3.3. If you had another 6 weeks, what would you add or change?

I would first separate the static driver-noise generator from the `Vehicle` class so independent simulations can safely own their randomness. Next, I would introduce a pre-generated arrival schedule to make policy comparisons more controlled and increase the number of seeds. I would measure waiting time for vehicles still in the map as well as completed trips. Turning routes and pedestrian crossings could then be added, with explicit intersection reservations and targeted collision tests before increasing the complexity.

---

## 4. Log Book and Final Reflection

### 4.1. Attach or link your log book entries for Weeks 7–13.

[Weeks 7–13 personal logbook](logbook-weeks7-13.md)

**Still to complete:** add genuine dated entries and evidence. The linked file is a template, not a completed historical logbook. The generated test reports prove that this version was tested during the current revision; they do not prove that particular work happened in an earlier teaching week.

### 4.2. Reflect on the semester.

**Technical reflection draft — review these points against my own experience before submission.**

**What was the most important concept you learned?**

The central concept illustrated here is extending a program through stable interfaces. The original Observer interface already allows the display to respond to completed ticks. Implementing that same interface in a metrics recorder adds another use without changing the vehicle hierarchy. Similarly, separating the green-extension rule into a Strategy exposes a real design choice while preserving the established State transitions. I should connect this explanation to an actual learning experience in my logbook.

**What design trade-off did you face, and how did you resolve it?**

The main technical trade-off is between preserving existing behaviour and restructuring for concurrency. The delivered extension retains the original model, package structure and static noise source. It adds safe parallel reduction of copied data but keeps whole simulations sequential. This limits parallel experiment execution, but avoids changing vehicle motion merely to demonstrate another concurrency feature. The copied metric objects also add allocation in exchange for safe, stable observations.

**If you started over, what would you do differently?**

I would make random generators explicit per-engine dependencies and define measurements earlier. I would also capture baseline traces before modifying behaviour, keep small regression tests close to each feature, and separate implementation claims from actual results. The current baseline comparison demonstrates how tests can support a claim that a refactor preserves behaviour.

**What feedback from the Week 7 submission did you act on?**

**[Insert the real Week 7 feedback and your response.]** Include the feedback, the exact change, evidence such as a file or genuine commit, and how it was checked. Neither the uploaded code nor the design PDF identifies the tutor's actual feedback, so this answer must be completed from that evidence.
