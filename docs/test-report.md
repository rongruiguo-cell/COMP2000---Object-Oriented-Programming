# Validation report - original-source revision

Verified on 4 November 2026 (Sydney date) using Linux and OpenJDK 17.0.20. The untouched original compiled successfully first. The updated project compiles with `--release 17 -encoding UTF-8 -Xlint:all` without compiler warnings, using only Java standard-library classes.

## Executed regression suite

```text
Built dist/traffic-simulation.jar
PASS Original topology, signal State and vehicle behaviours
PASS Custom collector, empty input and immutable summaries
PASS Parallel reduction of immutable snapshots
PASS Strategy selection and unchanged adaptive limits
PASS Observer removal and CSV recording
PASS Seeded repeatability
PASS Vehicle conservation and following distances
PASS Background-loop lifecycle, pause and single step
PASS Original loader and invalid probability errors
PASS Swing toolbar and original renderer integration
PASS: 10 groups; 769104 assertions.
```

The assertion count comes largely from checking invariants repeatedly over ticks, not that many distinct test cases. The model sweep covers two policies, three demand probabilities and two seeds, each for 2500 ticks. It checks vehicle conservation, the population cap, same-lane non-overlap and mutually exclusive green signals. It does not constitute a proof of all geometric collision cases.

Additional checks cover original bus dwell and signal transitions, immutable metric records, independently computed collector results, parallel reductions, observer removal, recorder capacity/final-tick inclusion, CSV columns, repeatability, invalid arguments, a single background worker, pause/single-step/shutdown, and Swing toolbar actions on the EDT.

## Original-versus-updated comparison

`tests/BaselineTrace.java` was compiled against both the unmodified original and the updated source. The three seeds 1, 42 and 48977152 each produced identical SHA-256 traces over 3000 ticks. The trace includes ordered vehicle types, positions and speeds, and all signal phases, active axes and timers. Exact hashes are in `baseline-verification.json`.

## Recorded integration run

```text
java -jar dist/traffic-simulation.jar --headless --ticks=3000 --seed=48977152 --csv=docs/sample-statistics.csv
```

Result: 26 vehicles, mean speed 2.365874, 2 stopped, 189 spawned, 163 completed, mean completed-trip duration 426.380 ticks. The eighteen 5000-tick policy comparisons are recorded in `comparison.csv`.

## Visual and platform scope

The actual original city renderer and added toolbar were painted to `preview.png` and inspected. Controls were exercised headlessly on the EDT. Native Windows window creation, Windows batch scripts and file-chooser interaction were not executed on a Windows machine. The supplied JAR was compiled for Java 17.

The diagram PDF was rendered and visually checked against the current source. Technical worksheet examples describe this revision; personal logbook entries and real Week 7 feedback remain user-supplied evidence.
