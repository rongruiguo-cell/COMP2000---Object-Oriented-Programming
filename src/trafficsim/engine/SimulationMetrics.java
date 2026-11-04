package trafficsim.engine;

/** Safely published completed-tick metrics; no references to live model objects. */
public record SimulationMetrics(long tick, long seed, String timing, double spawnProbability,
                                long spawned, long completed, double meanCompletedTripTicks,
                                Statistics.Summary statistics) { }
