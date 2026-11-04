package trafficsim.view;

import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.*;
import trafficsim.engine.CsvExporter;
import trafficsim.engine.MetricsRecorder;
import trafficsim.engine.SimulationEngine;
import trafficsim.strategy.AdaptiveSignalTiming;
import trafficsim.strategy.FixedSignalTiming;

/** A small toolbar around the unchanged-size original city renderer. */
public final class SimulationControls extends JPanel {
    private static final long serialVersionUID = 1L;
    private final SimulationEngine engine;
    private final MetricsRecorder recorder;
    private final SimulationDisplay display;
    private final JButton pause = new JButton();
    private final JButton step = new JButton("Step (N)");
    private final Timer refresh;

    public SimulationControls(SimulationEngine engine, MetricsRecorder recorder, SimulationDisplay display) {
        this.engine = engine; this.recorder = recorder; this.display = display;
        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10)); setBackground(new Color(231, 236, 238));
        pause.setName("pause"); step.setName("step");
        pause.addActionListener(event -> { engine.setPaused(!engine.isPaused()); refresh(); });
        step.addActionListener(event -> { engine.stepOnce(); refresh(); });
        JComboBox<String> timing = new JComboBox<>(new String[]{"Adaptive", "Fixed"});
        timing.setName("timing"); timing.setSelectedItem(engine.getMetrics().timing());
        timing.addActionListener(event -> {
            engine.setTimingStrategy(timing.getSelectedIndex() == 0 ? new AdaptiveSignalTiming() : new FixedSignalTiming());
            display.repaint();
        });
        JSpinner demand = new JSpinner(new SpinnerNumberModel(engine.getMetrics().spawnProbability(), 0.0, 1.0, 0.01));
        demand.setName("demand"); demand.setEditor(new JSpinner.NumberEditor(demand, "0.00"));
        demand.addChangeListener(event -> { engine.setSpawnProbability(((Number) demand.getValue()).doubleValue()); display.repaint(); });
        JButton export = new JButton("Export CSV"); export.setName("export");
        export.addActionListener(event -> export());
        add(pause); add(step); add(new JLabel("Signals")); add(timing);
        add(new JLabel("Arrival probability / tick")); add(demand); add(export);
        add(new JLabel("Rongrui Guo | Week 13"));
        refresh = new Timer(150, event -> refresh()); refresh.start(); refresh();
    }
    private void refresh() {
        pause.setText(engine.isPaused() ? "Resume" : "Pause"); step.setEnabled(engine.isPaused()); display.repaint();
    }
    private void export() {
        boolean wasPaused = engine.isPaused(); engine.setPaused(true); refresh();
        try {
            JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new java.io.File("traffic-statistics.csv"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path destination = chooser.getSelectedFile().toPath();
            if (Files.exists(destination) && JOptionPane.showConfirmDialog(this, "Replace the existing file?", "CSV export",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            CsvExporter.write(destination, recorder.snapshot());
            JOptionPane.showMessageDialog(this, "Statistics exported.");
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Could not save CSV", JOptionPane.ERROR_MESSAGE);
        } finally { engine.setPaused(wasPaused); refresh(); }
    }
    public void dispose() { refresh.stop(); }
}
