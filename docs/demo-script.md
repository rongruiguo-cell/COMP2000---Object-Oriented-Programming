# Short demonstration of the individual extension

1. Start the application with a fixed seed.

   "This is the same nine-junction traffic simulation from the earlier project. I kept the original roads, vehicles, bus stops and emergency behaviour. My individual extension adds measurement and policy comparison."

2. Pause and press Step.

   "I can inspect one complete update at a time. The simulation and the original renderer use the same network lock. The new single-step operation uses that lock too."

3. Select Fixed and Adaptive.

   "I extracted the existing green-extension rule into a Strategy. Adaptive keeps the original rule. Fixed disables extensions. The original State classes still handle green, yellow and red transitions."

4. Export a CSV and point to the metrics.

   "A new recorder implements the same Observer interface as the display. It reads immutable completed-tick metrics. A custom stream collector computes several statistics and vehicle-type summaries in one pass."

5. Open the test and experiment summaries.

   "The default traffic traces match the original for three tested seeds. Separate comparisons show little difference at low demand and no guaranteed adaptive advantage at higher demand. Whole simulations are sequential because the original code shares a static noise generator. Parallel reduction is tested on copied immutable data."

Use this after running and understanding the project. It is a demonstration guide, not evidence that a class showcase has already taken place.
