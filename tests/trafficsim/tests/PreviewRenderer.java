package trafficsim.tests;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import trafficsim.Main;
import trafficsim.engine.*;
import trafficsim.view.*;

public final class PreviewRenderer {
    private PreviewRenderer() { }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args.length==0?"docs/preview.png":args[0]);Files.createDirectories(output.toAbsolutePath().getParent());
        SwingUtilities.invokeAndWait(()->{
            SimulationEngine engine=Main.createEngine(null,48977152,0.14,true);
            MetricsRecorder recorder=new MetricsRecorder(engine,10,6000);engine.addObserver(recorder);
            SimulationDisplay display=new SimulationDisplay(engine);SimulationControls controls=new SimulationControls(engine,recorder,display);
            JPanel root=new JPanel(new BorderLayout());root.add(controls,BorderLayout.NORTH);root.add(display,BorderLayout.CENTER);
            try {
                for(int i=0;i<2800;i++)engine.step();
                for(Component c:controls.getComponents())
                    if(c instanceof javax.swing.JButton button && "pause".equals(button.getName()))button.doClick();
                root.setSize(1100,746);layout(root);
                BufferedImage image=new BufferedImage(1100,746,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();root.printAll(g);g.dispose();
                ImageIO.write(image,"png",output.toFile());
            } catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
            finally {controls.dispose();engine.shutdown();}
        });
        System.out.println("Rendered original city view with new toolbar: "+output);
    }
    private static void layout(Container parent){parent.doLayout();for(Component c:parent.getComponents())if(c instanceof Container child)layout(child);}
}
