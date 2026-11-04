package trafficsim;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import trafficsim.engine.SimulationEngine;
import trafficsim.engine.SimulationMetrics;

/** Runs complete simulations sequentially because original Vehicle noise is static. */
public final class ComparisonRunner {
    private ComparisonRunner() { }
    public static void main(String[] args) throws Exception {
        int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        if (ticks < 1 || ticks > 100000) throw new IllegalArgumentException("Use 1-100000 ticks");
        Path output = Path.of(args.length > 1 ? args[1] : "comparison.csv");
        StringBuilder csv = new StringBuilder("seed,spawn_probability,timing,ticks,completed,mean_stopped,mean_speed,mean_completed_trip_ticks\n");
        for (double probability : new double[]{0.03, SimConstants.SPAWN_PROBABILITY, 0.14})
            for (long seed : new long[]{1, 42, 48977152}) for (boolean adaptive : new boolean[]{false, true}) {
                SimulationEngine engine = Main.createEngine(null, seed, probability, adaptive);
                long stopped = 0; double speed = 0;
                for (int tick = 0; tick < ticks; tick++) {
                    engine.step(); stopped += engine.getMetrics().statistics().stopped();
                    speed += engine.getMetrics().statistics().averageSpeed();
                }
                SimulationMetrics last = engine.getMetrics();
                csv.append(String.format(Locale.ROOT, "%d,%.2f,%s,%d,%d,%.6f,%.6f,%.6f%n", seed, probability,
                        last.timing(), ticks, last.completed(), (double) stopped / ticks, speed / ticks, last.meanCompletedTripTicks()));
            }
        Files.writeString(output, csv, StandardCharsets.UTF_8);
        System.out.println("Wrote 18 sequential seeded runs to " + output);
    }
}
