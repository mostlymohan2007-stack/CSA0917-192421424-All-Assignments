import java.awt.*;
import java.awt.event.*;

/**
 * Task 4 - the program after fixing only BUG 1 and BUG 2 (name and label) but NOT the repainting problem.
 * The dot appears when you click, but it is painted with getGraphics() and not stored, so
 * minimising and restoring (or covering the window) makes every dot disappear.
 */
public class HalfFixedDrawingPad extends Frame {
    final Label label = new Label("Clicks: 0");
    final Canvas pad = new Canvas();
    int clicks = 0;

    public HalfFixedDrawingPad() {
        super("Half-fixed Drawing Pad (draws in the handler)");
        pad.setBackground(Color.WHITE);
        add(label, BorderLayout.NORTH);
        add(pad, BorderLayout.CENTER);
        pad.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                clicks++;
                label.setText("Clicks: " + clicks);
                Graphics g = pad.getGraphics();                       // still a temporary drawing surface
                g.fillOval(e.getX() - 5, e.getY() - 5, 10, 10);
                g.dispose();
            }
        });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
        setSize(420, 320);
    }

    public static void main(String[] args) { new HalfFixedDrawingPad().setVisible(true); }
}
