package trafficsim.tests;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import trafficsim.Main;
import trafficsim.SimConstants;
import trafficsim.engine.*;
import trafficsim.exception.InvalidNetworkException;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.light.TrafficLight;
import trafficsim.model.road.*;
import trafficsim.model.vehicle.*;
import trafficsim.strategy.*;
import trafficsim.util.*;
import trafficsim.view.*;

/** Meaningful integration and regression checks; uses only the Java standard library. */
public final class RegressionTests {
    private static int assertions;
    private static int groups;
    private RegressionTests() { }
    public static void main(String[] args) throws Exception {
        group("Original topology, signal State and vehicle behaviours", RegressionTests::originalModel);
        group("Custom collector, empty input and immutable summaries", RegressionTests::collector);
        group("Parallel reduction of immutable snapshots", RegressionTests::parallelCollector);
        group("Strategy selection and unchanged adaptive limits", RegressionTests::strategies);
        group("Observer removal and CSV recording", RegressionTests::recording);
        group("Seeded repeatability", RegressionTests::repeatability);
        group("Vehicle conservation and following distances", RegressionTests::trafficInvariants);
        group("Background-loop lifecycle, pause and single step", RegressionTests::lifecycle);
        group("Original loader and invalid probability errors", RegressionTests::validation);
        group("Swing toolbar and original renderer integration", RegressionTests::swingControls);
        System.out.println("PASS: " + groups + " groups; " + assertions + " assertions.");
    }
    @FunctionalInterface private interface Task { void run() throws Exception; }
    private static void group(String name, Task task) throws Exception { task.run(); groups++; System.out.println("PASS " + name); }
    private static void check(boolean condition, String message) {
        assertions++; if (!condition) throw new AssertionError(message);
    }
    private static void near(double a, double b, double tolerance, String message) { check(Math.abs(a-b)<=tolerance,message+": "+a+" vs "+b); }
    private static void fails(Class<? extends Throwable> type, Task task) throws Exception {
        try { task.run(); } catch (Throwable error) { check(type.isInstance(error), "Unexpected exception " + error); return; }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static List<Vehicle> vehicles(RoadNetwork network) {
        return network.getRoads().stream().flatMap(r -> r.getLanes().stream()).flatMap(l -> l.getVehicles().stream()).toList();
    }
    private static void originalModel() {
        RoadNetwork n = NetworkLoader.loadFromFile("networks/grid.txt");
        check(n.getRoads().size()==6 && n.getIntersections().size()==9 && n.getBusStops().size()==3,"Original map preserved");
        TrafficLight light=new TrafficLight(4,3,2,Axis.HORIZONTAL);
        for(int i=0;i<4;i++)light.update(); check(light.getPhase()==LightPhase.YELLOW,"Green -> yellow");
        for(int i=0;i<3;i++)light.update(); check(light.getPhase()==LightPhase.RED,"Yellow -> all-red");
        for(int i=0;i<2;i++)light.update(); check(light.getGreenAxis()==Axis.VERTICAL&&light.getPhase()==LightPhase.GREEN,"Red -> opposite green");
        Bus bus=new Bus(430,341,Direction.EAST); bus.addStop(n.getBusStops().get(0));
        for(int i=0;i<30;i++) { bus.move(SensorReading.clear()); near(430,bus.getX(),0,"Original 30-tick bus dwell"); }
        bus.move(SensorReading.clear()); check(bus.getX()>430,"Bus departs after dwell");
        check(!new EmergencyVehicle(0,0,Direction.EAST).respectsRedLight(),"Original emergency exemption preserved");
    }
    private static List<VehicleMetric> samples() {
        List<VehicleMetric> result=new ArrayList<>();
        for(int i=0;i<12000;i++)result.add(new VehicleMetric(i%2==0?"Car":"Bus",(i%13)*0.1));
        return List.copyOf(result);
    }
    private static void collector() throws Exception {
        List<VehicleMetric> values=samples(); double sum=0; long stopped=0;
        for(VehicleMetric v:values){sum+=v.speed();if(v.speed()==0)stopped++;}
        Statistics.Summary s=Statistics.summarise(values);
        check(s.count()==values.size()&&s.stopped()==stopped,"Independent count and stopped count");
        near(sum/values.size(),s.averageSpeed(),1e-12,"Mean"); near(1.2,s.maximumSpeed(),1e-12,"Maximum");
        check(s.byType().get("Car").count()==6000&&s.byType().get("Bus").count()==6000,"Type buckets");
        fails(UnsupportedOperationException.class,()->s.byType().clear());
        Statistics.Summary empty=Statistics.summarise(List.of());
        check(empty.count()==0&&empty.averageSpeed()==0&&empty.maximumSpeed()==0,"Empty input");
    }
    private static void parallelCollector() {
        List<VehicleMetric> values=samples();
        Statistics.Summary serial=values.stream().collect(Statistics.summaryCollector());
        Statistics.Summary parallel=values.parallelStream().collect(Statistics.summaryCollector());
        check(serial.count()==parallel.count()&&serial.stopped()==parallel.stopped(),"Parallel counts");
        near(serial.averageSpeed(),parallel.averageSpeed(),1e-12,"Parallel sum tolerance");
        for(String type:serial.byType().keySet()) {
            check(serial.byType().get(type).count()==parallel.byType().get(type).count(),"Parallel type count");
            near(serial.byType().get(type).averageSpeed(),parallel.byType().get(type).averageSpeed(),1e-12,"Parallel type mean");
        }
    }
    private static void strategies() {
        SignalTimingStrategy adaptive=new AdaptiveSignalTiming(),fixed=new FixedSignalTiming();
        check(adaptive.extensionTicks(4,4,0)==12,"Original threshold and extension");
        check(adaptive.extensionTicks(5,4,0)==0&&adaptive.extensionTicks(4,3,0)==0,"Only near end with queue");
        check(adaptive.extensionTicks(4,4,3)==0,"Original per-phase cap");
        check(fixed.extensionTicks(1,100,0)==0,"Fixed never extends");
        SimulationEngine e=Main.createEngine(null,42,0.06,true);
        for(int i=0;i<15;i++)e.step();
        Intersection first=e.getNetwork().getIntersections().get(0); int timer=first.getLight().getTimer();
        e.setTimingStrategy(fixed);
        check(first.getLight().getTimer()==timer&&e.getMetrics().timing().equals("Fixed"),"Policy swap preserves phase timer");
    }
    private static void recording() throws Exception {
        SimulationEngine e=Main.createEngine(null,42,0.06,true);
        MetricsRecorder recorder=new MetricsRecorder(e,10,5); e.addObserver(recorder);
        AtomicInteger calls=new AtomicInteger(); SimulationObserver observer=()->calls.incrementAndGet(); e.addObserver(observer);
        SimulationMetrics first=e.getMetrics();
        for(int i=0;i<57;i++)e.step();
        check(calls.get()==57,"Observer per completed tick");e.removeObserver(observer); e.step();check(calls.get()==57,"Remove observer");
        check(first.tick()==0&&first.statistics().count()==6,"Published old metrics stable");
        List<SimulationMetrics> rows=recorder.snapshot(); check(rows.size()==6&&rows.get(5).tick()==58,"Bounded samples plus latest tick");
        fails(UnsupportedOperationException.class,()->rows.clear());
        Path out=Files.createTempFile("traffic-regression-", ".csv");
        try {CsvExporter.write(out,rows);List<String> lines=Files.readAllLines(out);
            check(lines.size()==rows.size()+1,"CSV rows");check(lines.get(1).split(",").length==15,"CSV columns");
        } finally {Files.deleteIfExists(out);}
    }
    private static List<SimulationMetrics> run(long seed) {
        SimulationEngine e=Main.createEngine(null,seed,0.06,true);List<SimulationMetrics> frames=new ArrayList<>();
        for(int i=0;i<1500;i++){e.step();frames.add(e.getMetrics());}return frames;
    }
    private static void repeatability() {
        // Runs are sequential: original Vehicle has a process-wide static noise RNG.
        List<SimulationMetrics> a=run(99),b=run(99);
        check(a.equals(b),"Same seed matches every recorded tick");check(!a.equals(run(100)),"Different seed changes frames");
    }
    private static void trafficInvariants() {
        for(boolean adaptive:new boolean[]{false,true})for(double demand:new double[]{0.03,0.06,0.14})for(long seed:new long[]{1,48977152}) {
            SimulationEngine e=Main.createEngine(null,seed,demand,adaptive);
            for(int t=0;t<2500;t++) {
                e.step();List<Vehicle> all=vehicles(e.getNetwork());
                check(e.getMetrics().spawned()-e.getMetrics().completed()==all.size(),"Vehicle conservation");
                check(all.size()<=SimConstants.SPAWN_CAP,"Original population cap");
                for(Road road:e.getNetwork().getRoads())for(Lane lane:road.getLanes()) {
                    Direction d=lane.getDirection();
                    List<Vehicle> order=lane.getVehicles().stream().sorted(Comparator.comparingDouble(v->v.getX()*d.dx()+v.getY()*d.dy())).toList();
                    for(int i=1;i<order.size();i++) {
                        Vehicle rear=order.get(i-1),front=order.get(i);
                        double distance=(front.getX()-rear.getX())*d.dx()+(front.getY()-rear.getY())*d.dy();
                        double gap=distance-(front.getLength()+rear.getLength())/2;
                        check(gap>=-1e-8,"Same-lane vehicle overlap, seed="+seed+" tick="+t);
                    }
                }
                for(Intersection intersection:e.getNetwork().getIntersections())
                    check(!(intersection.getLight().phaseFor(Axis.HORIZONTAL)==LightPhase.GREEN
                            &&intersection.getLight().phaseFor(Axis.VERTICAL)==LightPhase.GREEN),"No conflicting green signals");
            }
            check(e.getMetrics().completed()>0,"Traffic makes progress");
        }
    }
    private static void lifecycle() throws Exception {
        SimulationEngine e=Main.createEngine(null,7,0.06,true);e.setPaused(true);
        try {
            e.run();e.run(); Thread.sleep(60);
            check(Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("sim-loop")).count()==1,"One worker only");
            check(e.getTickCount()==0,"Paused loop does not tick"); check(e.stepOnce()&&e.getTickCount()==1,"One manual tick while paused");
            e.setPaused(false);Thread.sleep(90);e.setPaused(true);long tick=e.getTickCount();
            Thread.sleep(60);check(e.getTickCount()==tick&&tick>1,"Pause establishes stable state");
        } finally {e.shutdown();}
        long tick=e.getTickCount();Thread.sleep(40);check(e.getTickCount()==tick,"Stopped worker stays stopped");
    }
    private static void validation() throws Exception {
        fails(InvalidNetworkException.class,()->NetworkLoader.loadFromFile("missing-network-test.txt"));
        fails(IllegalArgumentException.class,()->new VehicleSpawner(new Random(1),Double.NaN));
        fails(IllegalArgumentException.class,()->new VehicleSpawner(new Random(1),1.1));
        fails(IllegalArgumentException.class,()->new VehicleMetric("Car",Double.POSITIVE_INFINITY));
    }
    private static void swingControls() throws Exception {
        SwingUtilities.invokeAndWait(()->{
            SimulationEngine e=Main.createEngine(null,42,0.06,true);MetricsRecorder recorder=new MetricsRecorder(e,10,100);
            SimulationDisplay display=new SimulationDisplay(e);SimulationControls controls=new SimulationControls(e,recorder,display);
            try {
                JButton pause=(JButton)find(controls,"pause"),step=(JButton)find(controls,"step");
                pause.doClick();check(e.isPaused(),"Toolbar pause");step.doClick();check(e.getTickCount()==1,"Toolbar step");
                ((JComboBox<?>)find(controls,"timing")).setSelectedItem("Fixed");check(e.getMetrics().timing().equals("Fixed"),"Toolbar policy");
                ((JSpinner)find(controls,"demand")).setValue(0.14);near(0.14,e.getMetrics().spawnProbability(),0,"Toolbar demand");
                display.getActionMap().get("single-step").actionPerformed(null);check(e.getTickCount()==2,"N binding");
            } finally {controls.dispose();e.shutdown();}
        });
    }
    private static Component find(Container parent,String name) {
        for(Component c:parent.getComponents()) {
            if(name.equals(c.getName()))return c;
            if(c instanceof Container child){Component found=find(child,name);if(found!=null)return found;}
        }
        return null;
    }
}
