package trafficsim.engine;

import java.util.Objects;

/** A copied value for analysis; never exposes a mutable Vehicle to worker threads. */
public record VehicleMetric(String type, double speed) {
    public VehicleMetric {
        Objects.requireNonNull(type, "Vehicle type is required");
        if (type.isBlank() || !Double.isFinite(speed) || speed < 0)
            throw new IllegalArgumentException("Invalid vehicle metric");
    }
}
