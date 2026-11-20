import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Task 4 - evidence run. For each version: send three mouse clicks, screenshot, minimise, restore, screenshot.
 * (Clicks are delivered with dispatchEvent so the real mouse pointer is not moved.)
 */
public class DrawingPadTest {
    static void click(Component c, int x, int y) throws Exception {
        EventQueue.invokeAndWait(() -> c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false)));
        Thread.sleep(150);
    }

    static void minimiseAndRestore(Frame f) throws Exception {
        EventQueue.invokeAndWait(() -> f.setExtendedState(Frame.ICONIFIED));
        Thread.sleep(900);
        EventQueue.invokeAndWait(() -> f.setExtendedState(Frame.NORMAL));
        Thread.sleep(900);
    }

    /** Counts non-white pixels in the pad area of a real screenshot: proves whether dots are visible. */
    static int coloured(Frame f, Component pad) throws Exception {
        Point p = pad.getLocationOnScreen();
        java.awt.image.BufferedImage img = new Robot().createScreenCapture(new Rectangle(p.x, p.y, pad.getWidth(), pad.getHeight()));
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) if ((img.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) n++;
        return n;
    }

    static void run(String name, Frame f, Component pad, Label label, String tag) throws Exception {
        EventQueue.invokeAndWait(() -> { f.setAlwaysOnTop(true); f.setLocation(150, 100); f.setVisible(true); });
        Thread.sleep(700);
        click(pad, 60, 60); click(pad, 160, 110); click(pad, 260, 180);
        System.out.println("== " + name);
        System.out.println("   after 3 clicks : label = '" + label.getText() + "', non-white pixels in pad = " + coloured(f, pad));
        Shot.save(f, "output/task04_" + tag + "_1_before_minimise.png");
        minimiseAndRestore(f);
        EventQueue.invokeAndWait(() -> { f.setAlwaysOnTop(true); f.toFront(); });
        Thread.sleep(500);
        System.out.println("   after restore  : label = '" + label.getText() + "', non-white pixels in pad = " + coloured(f, pad));
        Shot.save(f, "output/task04_" + tag + "_2_after_restore.png");
        EventQueue.invokeAndWait(f::dispose);
    }

    public static void main(String[] args) throws Exception {
        BuggyDrawingPad b = new BuggyDrawingPad();
        run("Version 1: BuggyDrawingPad (misnamed handler, no label update)", b, b.pad, b.label, "v1_buggy");
        HalfFixedDrawingPad h = new HalfFixedDrawingPad();
        run("Version 2: HalfFixedDrawingPad (draws with getGraphics in the handler)", h, h.pad, h.label, "v2_halffixed");
        DrawingPad d = new DrawingPad();
        run("Version 3: DrawingPad (points stored, paint() redraws) - TODO 1 and TODO 2 completed", d, d.pad, d.label, "v3_fixed");
        DrawingPadBuffered bb = new DrawingPadBuffered();
        run("Version 4: DrawingPadBuffered (off-screen BufferedImage)", bb, bb.pad, bb.label, "v4_buffered");
        System.exit(0);
    }
}
