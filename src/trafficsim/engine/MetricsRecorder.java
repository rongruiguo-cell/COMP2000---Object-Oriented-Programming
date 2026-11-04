package trafficsim.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** A second Observer, alongside the original display, recording scalar metrics. */
public final class MetricsRecorder implements SimulationObserver {
    private final SimulationEngine engine;
    private final int sampleInterval;
    private final int capacity;
    private final List<SimulationMetrics> rows = new ArrayList<>();
    public MetricsRecorder(SimulationEngine engine, int sampleInterval, int capacity) {
        this.engine = Objects.requireNonNull(engine);
        if (sampleInterval < 1 || capacity < 1) throw new IllegalArgumentException("Positive interval and capacity required");
        this.sampleInterval = sampleInterval; this.capacity = capacity;
        rows.add(engine.getMetrics());
    }
    @Override public synchronized void onSimulationStep() {
        SimulationMetrics latest = engine.getMetrics();
        if (latest.tick() % sampleInterval != 0) return;
        if (!rows.isEmpty() && rows.get(rows.size() - 1).tick() == latest.tick()) rows.set(rows.size() - 1, latest);
        else rows.add(latest);
        if (rows.size() > capacity) rows.remove(0);
    }
    /** Independent list; includes the current final tick even between samples. */
    public synchronized List<SimulationMetrics> snapshot() {
        List<SimulationMetrics> result = new ArrayList<>(rows);
        SimulationMetrics latest = engine.getMetrics();
        if (!result.isEmpty() && result.get(result.size() - 1).tick() == latest.tick()) result.set(result.size() - 1, latest);
        else result.add(latest);
        return List.copyOf(result);
    }
}
