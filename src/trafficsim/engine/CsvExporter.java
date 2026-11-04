package trafficsim.engine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class CsvExporter {
    private CsvExporter() { }
    public static void write(Path path, List<SimulationMetrics> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("tick,seed,timing,spawn_probability,vehicles,average_speed,maximum_speed,stopped,spawned,completed,mean_completed_trip_ticks,cars,buses,trucks,emergency_vehicles\n");
            for (SimulationMetrics m : rows) {
                Statistics.Summary s = m.statistics();
                writer.write(String.format(Locale.ROOT, "%d,%d,%s,%.6f,%d,%.6f,%.6f,%d,%d,%d,%.6f,%d,%d,%d,%d%n",
                        m.tick(), m.seed(), m.timing(), m.spawnProbability(), s.count(), s.averageSpeed(), s.maximumSpeed(),
                        s.stopped(), m.spawned(), m.completed(), m.meanCompletedTripTicks(),
                        count(s, "Car"), count(s, "Bus"), count(s, "Truck"), count(s, "EmergencyVehicle")));
            }
        }
    }
    private static long count(Statistics.Summary summary, String type) {
        return summary.byType().getOrDefault(type, new Statistics.TypeSummary(0, 0)).count();
    }
}
