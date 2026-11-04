import java.util.Random;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
import trafficsim.engine.*;
import trafficsim.factory.NetworkLoader;
import trafficsim.model.road.*;
import trafficsim.model.vehicle.*;
public final class BaselineTrace {
 public static void main(String[] args) throws Exception {
  long seed=Long.parseLong(args[0]);
  Vehicle.seedNoise(seed ^ 0x9E3779B97F4A7C15L);
  RoadNetwork network=NetworkLoader.loadFromFile("networks/grid.txt");
  VehicleSpawner spawner=new VehicleSpawner(new Random(seed),0.06);
  for(int i=0;i<6;i++)spawner.spawnOne(network);
  SimulationEngine engine=new SimulationEngine(network,30,spawner,seed);
  MessageDigest digest=MessageDigest.getInstance("SHA-256");
  for(int tick=0;tick<3000;tick++) {
   engine.step();
   digest.update(ByteBuffer.allocate(8).putLong(engine.getTickCount()).array());
   for(Road r:network.getRoads())for(Lane lane:r.getLanes()) {
    digest.update(ByteBuffer.allocate(4).putInt(lane.getVehicles().size()).array());
    for(Vehicle v:lane.getVehicles()) {
     digest.update(v.getClass().getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
     digest.update(ByteBuffer.allocate(24).putDouble(v.getX()).putDouble(v.getY()).putDouble(v.getSpeed()).array());
    }
   }
   for(Intersection i:network.getIntersections()) {
    digest.update((byte)i.getLight().getPhase().ordinal());
    digest.update((byte)i.getLight().getGreenAxis().ordinal());
    digest.update(ByteBuffer.allocate(4).putInt(i.getLight().getTimer()).array());
   }
  }
  System.out.println(seed+" "+java.util.HexFormat.of().formatHex(digest.digest()));
 }
}
