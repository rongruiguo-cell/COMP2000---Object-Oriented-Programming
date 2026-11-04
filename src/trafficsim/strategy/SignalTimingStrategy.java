package trafficsim.strategy;

/** Policy for extending a green phase; state transitions remain in LightState. */
public interface SignalTimingStrategy {
    int extensionTicks(int remainingGreenTicks, int queuedVehicles, int extensionsUsed);
    String name();
}
