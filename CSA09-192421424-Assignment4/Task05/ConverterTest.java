import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import javax.swing.*;

/**
 * Task 5 - runs the same three tests on every version:
 *   T1 valid amount + Convert button, T2 Enter key in the amount field, T3 invalid amount, T4 close with WindowAdapter.
 * Screenshots of the dialogs are saved for the lambda version. Also counts the wiring lines of each version.
 */
public class ConverterTest {
    static String shotFor = null;      // name of the screenshot the dialog closer should take (null = none)

    /** Runs on a helper thread: waits for a modal dialog, optionally photographs it, then closes it. Returns its title. */
    static String[] dialogTitle = new String[1];
    static Thread dialogCloser() {
        Thread t = new Thread(() -> {
            try {
                for (int i = 0; i < 40; i++) {
                    Thread.sleep(100);
                    for (Window w : Window.getWindows()) {
                        if (w instanceof JDialog && w.isShowing()) {
                            dialogTitle[0] = ((JDialog) w).getTitle();
                            if (shotFor != null) Shot.save(w, shotFor);
                            SwingUtilities.invokeLater(w::dispose);
                            return;
                        }
                    }
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    static String runAction(ConverterBase f, Runnable trigger, String shot) throws Exception {
        dialogTitle[0] = "(no dialog)";
        shotFor = shot;
        Thread closer = dialogCloser();
        EventQueue.invokeAndWait(trigger);      // returns when the modal dialog has been closed by the helper
        closer.join(3000);
        return dialogTitle[0];
    }

    static void testVersion(String label, ConverterBase f, String shotPrefix) throws Exception {
        System.out.println("== " + label);
        EventQueue.invokeAndWait(() -> { f.setAlwaysOnTop(true); f.setVisible(true); });
        Thread.sleep(500);

        EventQueue.invokeAndWait(() -> { f.txtAmount.setText("100"); f.cmbFrom.setSelectedItem("USD"); f.cmbTo.setSelectedItem("INR"); });
        String d1 = runAction(f, f.btnConvert::doClick, shotPrefix == null ? null : shotPrefix + "_valid_conversion_dialog.png");
        System.out.println("   T1 valid, Convert button   : label='" + f.lblResult.getText() + "' dialog='" + d1 + "'");
        if (shotPrefix != null) Shot.save(f, shotPrefix + "_valid_conversion_window.png");

        EventQueue.invokeAndWait(() -> { f.txtAmount.setText("2500.50"); f.cmbFrom.setSelectedItem("INR"); f.cmbTo.setSelectedItem("USD"); });
        String d2 = runAction(f, f.txtAmount::postActionEvent, null);
        System.out.println("   T2 valid, Enter in field   : label='" + f.lblResult.getText() + "' dialog='" + d2 + "'");

        EventQueue.invokeAndWait(() -> f.txtAmount.setText("abc"));
        String d3 = runAction(f, f.btnConvert::doClick, shotPrefix == null ? null : shotPrefix + "_invalid_amount_dialog.png");
        System.out.println("   T3 invalid amount 'abc'    : label='" + f.lblResult.getText() + "' dialog='" + d3 + "'");
        if (shotPrefix != null) Shot.save(f, shotPrefix + "_invalid_amount_window.png");

        EventQueue.invokeAndWait(() -> f.dispatchEvent(new java.awt.event.WindowEvent(f, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        Thread.sleep(300);
        System.out.println("   T4 window closing (WindowAdapter): window still displayable = " + f.isDisplayable());
    }

    /** Lines between the WIRING-START and WIRING-END markers (code, not blank lines or comments). */
    static int wiringLines(String file) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(file));
        int n = 0; boolean in = false;
        for (String s : lines) {
            String t = s.trim();
            if (t.contains("WIRING-START")) { in = true; continue; }
            if (t.contains("WIRING-END")) { in = false; continue; }
            if (in && !t.isEmpty() && !t.startsWith("//")) n++;
        }
        return n;
    }

    public static void main(String[] args) throws Exception {
        testVersion("Style 1: frame implements ActionListener", new ConverterFrameListener(), null);
        testVersion("Style 2: named inner class", new ConverterInnerClass(), null);
        testVersion("Style 3: anonymous inner class", new ConverterAnonymous(), null);
        testVersion("Style 4: lambda (screenshots saved)", new ConverterLambda(), "output/task05_lambda");
        testVersion("Extra: method reference + shared Action", new ConverterMethodRef(), null);

        System.out.println();
        System.out.println("Sample conversion check: 100 USD -> INR = " + ConverterBase.convert(100, "USD", "INR") + ", 8300 INR -> USD = " + ConverterBase.convert(8300, "INR", "USD"));
        System.out.println();
        System.out.println("Wiring code per version (non-blank, non-comment lines between WIRING-START and WIRING-END):");
        String[] files = {"ConverterFrameListener.java", "ConverterInnerClass.java", "ConverterAnonymous.java", "ConverterLambda.java", "ConverterMethodRef.java"};
        for (String f : files) System.out.printf("   %-30s %2d lines%n", f, wiringLines(f));
        System.exit(0);
    }
}
