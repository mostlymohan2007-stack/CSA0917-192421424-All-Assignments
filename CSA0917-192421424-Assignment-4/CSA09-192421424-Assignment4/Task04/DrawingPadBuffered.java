import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Task 4 - alternative concept: draw each dot ONCE onto an off-screen BufferedImage; paint() only copies the image.
 * Memory is fixed (width x height pixels) however many dots are drawn; the individual points are not remembered,
 * so the picture cannot be re-scaled or edited point by point.
 */
public class DrawingPadBuffered extends Frame {
    final Label label = new Label("Clicks: 0");
    BufferedImage image = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
    int clicks = 0;

    final Canvas pad = new Canvas() {
        @Override public void paint(Graphics g) {
            g.drawImage(image, 0, 0, null);                         // O(width x height) copy, independent of the number of dots
        }
    };

    public DrawingPadBuffered() {
        super("Drawing Pad (BufferedImage)");
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.dispose();
        add(label, BorderLayout.NORTH);
        add(pad, BorderLayout.CENTER);
        pad.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Graphics2D g2 = image.createGraphics();
                g2.setColor(Color.RED);
                g2.fillOval(e.getX() - 5, e.getY() - 5, 10, 10);      // O(1) per click
                g2.dispose();
                label.setText("Clicks: " + (++clicks));
                pad.repaint();
            }
        });
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
        setSize(420, 320);
    }

    public static void main(String[] args) { new DrawingPadBuffered().setVisible(true); }
}
