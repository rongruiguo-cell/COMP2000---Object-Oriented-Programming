package trafficsim.engine;

import trafficsim.SimConstants;
import trafficsim.model.road.Intersection;
import trafficsim.model.road.Lane;
import trafficsim.model.road.Road;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.road.SignalisedIntersection;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.strategy.SignalTimingStrategy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;

/** Original fixed-tick phases with additional metrics and guarded lifecycle operations. */
public class SimulationEngine {
    private final RoadNetwork network;
    private final List<SimulationObserver> observers = new CopyOnWriteArrayList<>();
    private final VehicleSpawner spawner;
    private final long runSeed;
    private final int tickRate;
    private volatile boolean running;
    private volatile boolean paused;
    private Thread worker;
    private long tickCount;
    private long spawned;
    private long completed;
    private long totalCompletedTripTicks;
    private String timingName = "Adaptive";
    private final Map<Vehicle, Long> birthTicks = new IdentityHashMap<>();
    private volatile SimulationMetrics metrics;

    public SimulationEngine(RoadNetwork network, int tickRate, VehicleSpawner spawner, long runSeed) {
        this.network = Objects.requireNonNull(network);
        if (tickRate < 1) throw new IllegalArgumentException("Tick rate must be positive");
        this.tickRate = tickRate; this.spawner = Objects.requireNonNull(spawner); this.runSeed = runSeed;
        synchronized (network) {
            for (Vehicle vehicle : collectVehicles()) birthTicks.put(vehicle, 0L);
            spawned = birthTicks.size(); updateMetrics();
        }
    }

    public void addObserver(SimulationObserver observer) { observers.add(Objects.requireNonNull(observer)); }
    public void removeObserver(SimulationObserver observer) { observers.remove(observer); }
    private void notifyObservers() { for (SimulationObserver observer : observers) observer.onSimulationStep(); }

    public void step() {
        synchronized (network) {
            if (paused) return;
            doStep();
        }
        notifyObservers();
    }

    /** Advance exactly once while paused; uses the same complete-update lock. */
    public boolean stepOnce() {
        synchronized (network) {
            if (!paused) return false;
            doStep();
        }
        notifyObservers();
        return true;
    }

    /** Caller holds the network lock. The original update order is preserved. */
    private void doStep() {
        tickCount++;
        updateLights();
        List<Vehicle> vehicles = collectVehicles();
        Map<Vehicle, SensorReading> readings = readSensors(vehicles);
        moveLaneVehicles(readings);
        spawner.despawnOffMap(network);
        List<Vehicle> remaining = collectVehicles();
        for (Vehicle vehicle : vehicles) if (!remaining.contains(vehicle)) {
            Long birth = birthTicks.remove(vehicle);
            if (birth != null) { completed++; totalCompletedTripTicks += tickCount - birth; }
        }
        // Keep the original cap check and spawning order for seeded compatibility.
        if (vehicles.size() < SimConstants.SPAWN_CAP) spawner.tick(network);
        for (Vehicle vehicle : collectVehicles()) if (!birthTicks.containsKey(vehicle)) {
            birthTicks.put(vehicle, tickCount); spawned++;
        }
        updateMetrics();
    }

    private void updateLights() {
        for (Intersection intersection : network.getIntersections()) intersection.update();
    }
    private Map<Vehicle, SensorReading> readSensors(List<Vehicle> vehicles) {
        Map<Vehicle, SensorReading> readings = new LinkedHashMap<>();
        for (Vehicle vehicle : vehicles) readings.put(vehicle, TrafficSensor.read(vehicle, network));
        return readings;
    }
    private void moveLaneVehicles(Map<Vehicle, SensorReading> readings) {
        for (Map.Entry<Vehicle, SensorReading> entry : readings.entrySet()) entry.getKey().move(entry.getValue());
    }
    private List<Vehicle> collectVehicles() {
        List<Vehicle> all = new ArrayList<>();
        for (Road road : network.getRoads())
            for (Lane lane : road.getLanes()) all.addAll(lane.getVehicles());
        return all;
    }
    private void updateMetrics() {
        Statistics.Summary summary = Statistics.summarise(Statistics.snapshot(collectVehicles()));
        metrics = new SimulationMetrics(tickCount, runSeed, timingName, spawner.getSpawnProbability(),
                spawned, completed, completed == 0 ? 0 : (double) totalCompletedTripTicks / completed, summary);
    }

    public void setTimingStrategy(SignalTimingStrategy strategy) {
        Objects.requireNonNull(strategy);
        synchronized (network) {
            for (Intersection intersection : network.getIntersections())
                if (intersection instanceof SignalisedIntersection signal) signal.setTimingStrategy(strategy);
            timingName = strategy.name(); updateMetrics();
        }
    }
    public void setSpawnProbability(double probability) {
        synchronized (network) { spawner.setSpawnProbability(probability); updateMetrics(); }
    }

    /** Repeated calls cannot start competing simulation loops. */
    public synchronized void run() {
        if (worker != null && worker.isAlive()) return;
        running = true;
        worker = new Thread(() -> {
            long nextTick = System.nanoTime();
            while (running) {
                long tickNanos = 1_000_000_000L / tickRate;
                step(); nextTick += tickNanos;
                long waitNanos = nextTick - System.nanoTime();
                if (waitNanos > 0) LockSupport.parkNanos(waitNanos);
                else nextTick = System.nanoTime();
            }
        }, "sim-loop");
        worker.setDaemon(true); worker.start();
    }
    public void shutdown() {
        Thread stopping;
        synchronized (this) { running = false; stopping = worker; if (stopping != null) LockSupport.unpark(stopping); }
        if (stopping != null && stopping != Thread.currentThread()) {
            try { stopping.join(1000); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }
    }
    public void setPaused(boolean paused) { synchronized (network) { this.paused = paused; } }
    public boolean isPaused() { return paused; }
    public long getTickCount() { return metrics.tick(); }
    public long getRunSeed() { return runSeed; }
    public RoadNetwork getNetwork() { return network; }
    public SimulationMetrics getMetrics() { return metrics; }
}
