package trafficsim.strategy;

import trafficsim.SimConstants;

/** The original project's adaptive rule, extracted without changing its defaults. */
public final class AdaptiveSignalTiming implements SignalTimingStrategy {
    @Override
    public int extensionTicks(int remainingGreenTicks, int queuedVehicles, int extensionsUsed) {
        if (remainingGreenTicks > 4 || extensionsUsed >= SimConstants.ADAPTIVE_MAX_EXTENSIONS
                || queuedVehicles < SimConstants.ADAPTIVE_QUEUE_HIGH) return 0;
        return SimConstants.ADAPTIVE_EXTEND_TICKS;
    }
    @Override public String name() { return "Adaptive"; }
}
