package trafficsim.engine;

import trafficsim.model.vehicle.Vehicle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public final class Statistics {
    private Statistics() { }

    // Original Week 7 API retained for existing callers.
    public static double averageSpeed(List<Vehicle> vehicles) {
        return vehicles.stream().mapToDouble(Vehicle::getSpeed).average().orElse(0.0);
    }
    public static long stoppedCount(List<Vehicle> vehicles) {
        return vehicles.stream().filter(v -> v.getSpeed() == 0.0).count();
    }

    /** Call with the network locked; later reductions use immutable copied values. */
    public static List<VehicleMetric> snapshot(List<Vehicle> vehicles) {
        return vehicles.stream().map(v -> new VehicleMetric(v.getClass().getSimpleName(), v.getSpeed())).toList();
    }
    public record TypeSummary(long count, double averageSpeed) { }
    public record Summary(long count, double averageSpeed, long stopped, double maximumSpeed,
                          Map<String, TypeSummary> byType) {
        public Summary { byType = Map.copyOf(byType); }
    }
    private static final class Bucket {
        long count;
        double speedSum;
        void accept(double speed) { count++; speedSum += speed; }
        void merge(Bucket other) { count += other.count; speedSum += other.speedSum; }
    }
    /** Each stream partition owns its accumulator and all of its buckets. */
    private static final class Accumulator {
        long count;
        long stopped;
        double sum;
        double maximum;
        final Map<String, Bucket> byType = new LinkedHashMap<>();
        void accept(VehicleMetric metric) {
            count++; sum += metric.speed(); maximum = Math.max(maximum, metric.speed());
            if (metric.speed() == 0.0) stopped++;
            byType.computeIfAbsent(metric.type(), key -> new Bucket()).accept(metric.speed());
        }
        Accumulator merge(Accumulator other) {
            count += other.count; stopped += other.stopped; sum += other.sum;
            maximum = Math.max(maximum, other.maximum);
            other.byType.forEach((type, bucket) -> byType.computeIfAbsent(type, key -> new Bucket()).merge(bucket));
            return this;
        }
        Summary finish() {
            Map<String, TypeSummary> types = new LinkedHashMap<>();
            byType.forEach((type, bucket) -> types.put(type, new TypeSummary(bucket.count, bucket.speedSum / bucket.count)));
            return new Summary(count, count == 0 ? 0 : sum / count, stopped, maximum, types);
        }
    }
    public static Collector<VehicleMetric, ?, Summary> summaryCollector() {
        return Collector.of(Accumulator::new, Accumulator::accept, Accumulator::merge, Accumulator::finish);
    }
    public static Summary summarise(List<VehicleMetric> metrics) {
        return metrics.stream().collect(summaryCollector());
    }
}
