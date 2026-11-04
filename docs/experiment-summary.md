# Fixed and adaptive timing: recorded comparison

Executed on 4 November 2026 (Sydney date), with OpenJDK 17.0.20. Each run starts with the original six vehicles, uses the unchanged nine-junction map and lasts 5000 ticks. Seeds: 1, 42 and 48977152. There are 18 runs: three arrival probabilities, two policies and three seeds.

```text
java -cp dist/traffic-simulation.jar trafficsim.ComparisonRunner 5000 comparison.csv
```

Whole simulations execute sequentially because the inherited driver-noise random generator is static. They are reseeded before each run. The custom collector's independent parallel-data test is separate from these simulations.

| Arrival probability / tick | Policy | Mean completed trips | Mean stopped vehicles | Mean speed | Mean completed-trip ticks |
|---|---|---:|---:|---:|---:|
| 0.03 | Fixed | 137.67 | 3.422 | 1.878 | 406.28 |
| 0.03 | Adaptive | 137.67 | 3.422 | 1.878 | 406.28 |
| 0.06 | Fixed | 274.00 | 7.511 | 1.796 | 428.82 |
| 0.06 | Adaptive | 274.00 | 7.501 | 1.797 | 428.73 |
| 0.14 | Fixed | 404.67 | 11.457 | 1.758 | 440.55 |
| 0.14 | Adaptive | 403.00 | 11.575 | 1.746 | 442.74 |

The adaptive rule has little effect in the recorded lower-demand runs and slightly lower completed throughput in the high-demand comparison. This does not establish a statistically significant difference or a universal ranking. The rule only extends green after a queue threshold is reached.

Speed is measured in abstract world units per tick. Mean stopped and speed values first average tick-level values across each run, then average the three runs equally. Mean completed-trip ticks is an unweighted mean of the three per-run means, not a pooled vehicle-level estimate.

Limitations: only three seeds; one run duration; no discarded warm-up; the forty-vehicle cap and entry-gap rejection affect realised demand; completed-trip means exclude unfinished trips. Once policies change traffic, random draws may be consumed differently, so equal seeds do not ensure identical realised arrivals. A future experiment should use a common pre-generated arrival schedule and more seeds. These are educational-model results, not real-road predictions.
