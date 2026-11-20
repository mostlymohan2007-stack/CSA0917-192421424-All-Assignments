import java.awt.*;
import java.awt.event.*;

/**
 * Task 4 - THE BROKEN PROGRAM (reconstructed: the assignment PDF does not print the supplied code).
 * Symptoms: clicking draws nothing and the click counter never changes.
 *   BUG 1: the handler is named mouseclicked (small c). MouseAdapter has mouseClicked, so this is just a new,
 *          unrelated method; it compiles (no abstract method is missing) but is never called.
 *   BUG 2: even if it were called, the counter is a local variable that is never shown: label.setText() is missing.
 *   BUG 3: the dot would be painted with getGraphics() inside the handler and not remembered, so repaint() loses it.
 */
public class BuggyDrawingPad extends Frame {
    final Label label = new Label("Clicks: 0");
    final Canvas pad = new Canvas();
    int clicks = 0;

    public BuggyDrawingPad() {
        super("Buggy Drawing Pad");
        pad.setBackground(Color.WHITE);
        add(label, BorderLayout.NORTH);
        add(pad, BorderLayout.CENTER);
        pad.addMouseListener(new MouseAdapter() {
            public void mouseclicked(MouseEvent e) {                // BUG 1: wrong name (no @Override to warn us)
                int count = ++clicks;                                // BUG 2: label never updated
                Graphics g = pad.getGraphics();                      // BUG 3: temporary drawing, not stored
                g.fillOval(e.getX() - 5, e.getY() - 5, 10, 10);
            }
        });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
        setSize(420, 320);
    }

    public static void main(String[] args) { new BuggyDrawingPad().setVisible(true); }
}
