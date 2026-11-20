import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Task 4 - cost of one repaint: redraw n stored Points (paint() loop) versus copy one off-screen BufferedImage.
 * Drawing is done onto an off-screen image, so no window is needed. Results depend on the computer: re-run and quote yours.
 */
public class PaintCostBench {
    static long usedMb() {
        for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(50); } catch (InterruptedException e) { } }
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;      // KB
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        BufferedImage screen = new BufferedImage(420, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = screen.createGraphics();
        Random rnd = new Random(1);
        System.out.println("Java " + System.getProperty("java.version") + " - cost of ONE repaint of a 420x320 area (average of 20 repaints after warm-up)");
        System.out.printf("%-12s %-22s %-18s %-22s%n", "Stored dots", "Point list: repaint", "Point list: memory", "BufferedImage: repaint");

        BufferedImage buffer = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = buffer.createGraphics();
        for (int i = 0; i < 1000; i++) bg.fillOval(rnd.nextInt(1000), rnd.nextInt(800), 10, 10);
        for (int w = 0; w < 5; w++) g.drawImage(buffer, 0, 0, null);
        long t0 = System.nanoTime();
        for (int i = 0; i < 20; i++) g.drawImage(buffer, 0, 0, null);
        double imageMs = (System.nanoTime() - t0) / 1e6 / 20;

        for (int n : new int[]{1_000, 10_000, 100_000, 1_000_000}) {
            long before = usedMb();
            List<Point> pts = new ArrayList<>(n);
            for (int i = 0; i < n; i++) pts.add(new Point(rnd.nextInt(420), rnd.nextInt(320)));
            long memKb = usedMb() - before;
            for (int w = 0; w < 3; w++) for (Point p : pts) g.fillOval(p.x - 5, p.y - 5, 10, 10);
            t0 = System.nanoTime();
            for (int i = 0; i < 20; i++) for (Point p : pts) g.fillOval(p.x - 5, p.y - 5, 10, 10);
            double ms = (System.nanoTime() - t0) / 1e6 / 20;
            System.out.printf("%-12d %-22s %-18s %-22s%n", n, String.format("%.3f ms", ms), String.format("%d KB", memKb), String.format("%.3f ms (fixed)", imageMs));
        }
        System.out.println("BufferedImage memory is fixed: 1000 x 800 x 4 bytes = " + (1000 * 800 * 4 / 1024) + " KB, whatever the number of dots.");
    }
}
