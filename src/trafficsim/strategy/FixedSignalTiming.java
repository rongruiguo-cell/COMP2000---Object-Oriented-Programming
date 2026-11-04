package trafficsim.strategy;

/** Uses the original green/yellow/red durations with no adaptive extensions. */
public final class FixedSignalTiming implements SignalTimingStrategy {
    @Override
    public int extensionTicks(int remainingGreenTicks, int queuedVehicles, int extensionsUsed) { return 0; }
    @Override public String name() { return "Fixed"; }
}
