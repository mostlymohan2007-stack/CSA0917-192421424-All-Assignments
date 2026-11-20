package campus.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Test helper: captures a window from the real screen with java.awt.Robot and saves it as a PNG file. */
public class Shot {
    public static void save(Window w, String fileName) throws Exception {
        SwingUtilities.invokeAndWait(() -> { w.toFront(); w.repaint(); });
        Thread.sleep(600);
        Robot robot = new Robot();
        robot.waitForIdle();
        Rectangle r = w.getBounds();
        BufferedImage img = robot.createScreenCapture(r);
        File f = new File(fileName);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("   [screenshot saved] " + fileName + "  (" + r.width + "x" + r.height + ")");
    }
}
