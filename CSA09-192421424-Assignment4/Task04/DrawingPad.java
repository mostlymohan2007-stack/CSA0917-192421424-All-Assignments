import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Task 4 - repaired drawing pad (Complete the Code: TODO 1 and TODO 2 are completed below).
 * Points are STORED; paint() draws every stored point, so the picture survives minimise/restore, resize and overlap.
 */
public class DrawingPad extends Frame {
    final Label label = new Label("Clicks: 0");
    final List<Point> points = new ArrayList<>();

    /** The drawing surface: paint() is called by AWT whenever the area must be redrawn. */
    final Canvas pad = new Canvas() {
        @Override public void paint(Graphics g) {
            // TODO 2 (completed): draw every stored point inside paint()
            g.setColor(Color.BLUE);
            for (Point p : points) {
                g.fillOval(p.x - 5, p.y - 5, 10, 10);
            }
        }
    };

    public DrawingPad() {
        super("Drawing Pad");
        pad.setBackground(Color.WHITE);
        add(label, BorderLayout.NORTH);
        add(pad, BorderLayout.CENTER);
        pad.addMouseListener(new MouseAdapter() {
            @Override                                              // @Override: a wrong method name would be a compile-time error
            public void mouseClicked(MouseEvent e) {
                // TODO 1 (completed): store the point, update the counter label, ask AWT to repaint
                points.add(e.getPoint());
                label.setText("Clicks: " + points.size());
                pad.repaint();
            }
        });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
        setSize(420, 320);
    }

    public static void main(String[] args) { new DrawingPad().setVisible(true); }
}
