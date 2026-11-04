package trafficsim;

import trafficsim.engine.*;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.road.RoadNetwork;
import trafficsim.model.vehicle.Vehicle;
import trafficsim.strategy.AdaptiveSignalTiming;
import trafficsim.strategy.FixedSignalTiming;
import trafficsim.view.SimulationControls;
import trafficsim.view.SimulationDisplay;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

public class Main {
    private static final String DEFAULT_GRID = "networks/grid.txt";

    public static void main(String[] args) {
        try { launch(args); }
        catch (Exception error) { System.err.println("Traffic Simulation: " + error.getMessage()); System.exit(1); }
    }
    private static void launch(String[] args) throws Exception {
        String networkPath = null;
        Long seed = null;
        boolean headless = false;
        boolean adaptive = true;
        int ticks = 3000;
        double probability = SimConstants.SPAWN_PROBABILITY;
        Path csv = null;
        for (String argument : args) {
            if (argument.equals("--help")) { printHelp(); return; }
            else if (argument.startsWith("--seed=")) seed = Long.parseLong(argument.substring(7));
            else if (argument.equals("--headless")) headless = true;
            else if (argument.startsWith("--ticks=")) ticks = Integer.parseInt(argument.substring(8));
            else if (argument.startsWith("--spawn=")) probability = Double.parseDouble(argument.substring(8));
            else if (argument.startsWith("--csv=")) csv = Path.of(argument.substring(6));
            else if (argument.equals("--timing=fixed")) adaptive = false;
            else if (argument.equals("--timing=adaptive")) adaptive = true;
            else if (argument.startsWith("--")) throw new IllegalArgumentException("Unknown option: " + argument);
            else if (networkPath == null) networkPath = argument;
            else throw new IllegalArgumentException("Only one network file can be supplied");
        }
        if (ticks < 0 || ticks > 1_000_000) throw new IllegalArgumentException("Ticks must be from 0 to 1000000");
        long runSeed = seed != null ? seed : new SecureRandom().nextLong();
        SimulationEngine engine = createEngine(networkPath, runSeed, probability, adaptive);
        MetricsRecorder recorder = new MetricsRecorder(engine, 10, headless ? 100_001 : 6000);
        engine.addObserver(recorder);
        if (headless) {
            for (int i = 0; i < ticks; i++) engine.step();
            SimulationMetrics m = engine.getMetrics();
            System.out.printf(Locale.ROOT,
                    "ticks=%d seed=%d timing=%s vehicles=%d meanSpeed=%.6f stopped=%d spawned=%d completed=%d meanTripTicks=%.3f%n",
                    m.tick(), m.seed(), m.timing(), m.statistics().count(), m.statistics().averageSpeed(),
                    m.statistics().stopped(), m.spawned(), m.completed(), m.meanCompletedTripTicks());
            if (csv != null) CsvExporter.write(csv, recorder.snapshot());
            return;
        }
        if (csv != null) throw new IllegalArgumentException("--csv is for --headless; use the GUI Export CSV button in desktop mode");
        if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("Desktop unavailable; use --headless");
        SwingUtilities.invokeLater(() -> {
            SimulationDisplay display = new SimulationDisplay(engine);
            engine.addObserver(display);
            display.setPreferredSize(new java.awt.Dimension(1100, 700));
            SimulationControls controls = new SimulationControls(engine, recorder, display);
            JPanel content = new JPanel(new BorderLayout());
            content.add(controls, BorderLayout.NORTH); content.add(display, BorderLayout.CENTER);
            JFrame frame = new JFrame("Traffic Simulation - Rongrui Guo - Week 13");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); frame.setContentPane(content);
            frame.pack(); frame.setLocationRelativeTo(null); frame.setResizable(false);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent event) { engine.shutdown(); controls.dispose(); }
                @Override public void windowClosed(WindowEvent event) { engine.shutdown(); controls.dispose(); }
            });
            frame.setVisible(true); engine.run();
        });
    }

    /** Preserves original network loading, two RNG streams, six initial vehicles and 30 Hz. */
    public static SimulationEngine createEngine(String networkPath, long runSeed, double probability, boolean adaptive) {
        Vehicle.seedNoise(runSeed ^ 0x9E3779B97F4A7C15L);
        RoadNetwork network;
        if (networkPath != null) network = NetworkLoader.loadFromFile(networkPath);
        else if (new File(DEFAULT_GRID).exists()) network = NetworkLoader.loadFromFile(DEFAULT_GRID);
        else network = NetworkLoader.buildDefault();
        VehicleSpawner spawner = new VehicleSpawner(new Random(runSeed), probability);
        for (int i = 0; i < 6; i++) spawner.spawnOne(network);
        SimulationEngine engine = new SimulationEngine(network, 30, spawner, runSeed);
        engine.setTimingStrategy(adaptive ? new AdaptiveSignalTiming() : new FixedSignalTiming());
        return engine;
    }
    private static void printHelp() {
        System.out.println("""
                Java 17+ Traffic Simulation (original repository, Week 13 extension)
                java -jar dist/traffic-simulation.jar [network-file] [options]
                  --seed=LONG         Repeat original spawning and driver-noise streams
                  --timing=adaptive   Original rule (default); or --timing=fixed
                  --spawn=NUMBER      Arrival probability per tick, between 0 and 1
                  --headless          Run without a desktop window
                  --ticks=INTEGER     Headless ticks (default 3000)
                  --csv=PATH          Headless metric export
                GUI: SPACE pause/resume; N step while paused; toolbar changes timing/demand.
                """);
    }
}
