import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Test helper: captures a window from the real screen with java.awt.Robot and saves it as a PNG file. */
public class Shot {
    /** Brings the window to the front, lets it repaint, and writes its pixels to the PNG file. */
    public static void save(Window w, String fileName) throws Exception {
        SwingUtilities.invokeAndWait(() -> { w.toFront(); w.repaint(); });
        Thread.sleep(500);
        Robot robot = new Robot();
        robot.waitForIdle();
        Rectangle r = w.getBounds();
        BufferedImage img = robot.createScreenCapture(r);
        File f = new File(fileName);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
        System.out.println("   [screenshot saved] " + fileName + "  (" + r.width + "x" + r.height + ")");
    }

    /** Finds a visible window (for example a JOptionPane dialog) whose title contains the text. */
    public static Window findWindow(String titlePart) {
        for (Window w : Window.getWindows()) {
            String t = (w instanceof Frame) ? ((Frame) w).getTitle() : (w instanceof Dialog) ? ((Dialog) w).getTitle() : "";
            if (w.isShowing() && t != null && t.contains(titlePart)) return w;
        }
        return null;
    }
}
