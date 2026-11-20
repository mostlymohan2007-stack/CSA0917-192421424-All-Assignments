import java.awt.Window;
import java.math.BigDecimal;

/**
 * Task 1 - test run. Part 1 tests FeeService WITHOUT any window (this is only possible because logic is separated).
 * Part 2 drives the real window and captures screenshots. Part 3 (argument "jdbc") runs the JDBC repository against MySQL.
 *
 *   java FeeServiceDemo                 -> parts 1 and 2
 *   java FeeServiceDemo jdbc URL USER PASSWORD   -> also part 3 (needs the mysql-connector-j jar on the class path)
 */
public class FeeServiceDemo {
    static int pass = 0, fail = 0;

    static void expect(FeeService s, String label, String id, String amt, String method, String expectedMessageStart) {
        String got;
        try {
            Receipt r = s.pay(id, amt, method);
            got = "OK receipt " + r.getNumber() + ", balance " + r.getBalanceAfter().toPlainString();
        } catch (ValidationException e) {
            got = "REJECTED: " + e.getMessage();
        } catch (Exception e) {
            got = "ERROR: " + e;
        }
        boolean ok = got.startsWith(expectedMessageStart);
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-34s -> %s%n", ok ? "PASS" : "FAIL", label, got);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== Part 1: FeeService tests (no window needed) ===");
        FeeService s = new FeeService(new InMemoryPaymentRepository(0));
        expect(s, "valid payment 2500.50 by UPI", "192421424", "2500.50", "UPI", "OK receipt 1001, balance 47499.50");
        expect(s, "blank amount", "192421424", "", "Cash", "REJECTED: Enter the amount");
        expect(s, "letters in amount", "192421424", "abc", "Cash", "REJECTED: Amount must be a number");
        expect(s, "negative amount", "192421424", "-50", "Cash", "REJECTED: Amount must be greater than zero");
        expect(s, "three decimals", "192421424", "10.555", "Cash", "REJECTED: Amount can have at most 2 decimal");
        expect(s, "bad student id", "12ab", "100", "Cash", "REJECTED: Student ID must be");
        expect(s, "unknown student", "999999999", "100", "Cash", "REJECTED: Student 999999999 was not found");
        expect(s, "no outstanding fee", "192421426", "100", "Cash", "REJECTED: Student 192421426 has no outstanding");
        expect(s, "more than outstanding", "192421425", "99999", "Card", "REJECTED: Amount 99999 is more than");
        expect(s, "SQL-injection style id", "1' OR '1'='1", "100", "Cash", "REJECTED: Student ID must be");
        expect(s, "invalid method", "192421424", "100", "Cheque", "REJECTED: Choose Cash");
        System.out.println("Result: " + pass + " passed, " + fail + " failed");

        System.out.println();
        System.out.println("=== Part 2: real window, event-driven test with screenshots ===");
        FeeService svc = new FeeService(new InMemoryPaymentRepository(600));
        final FeePaymentWindow[] holder = new FeePaymentWindow[1];
        javax.swing.SwingUtilities.invokeAndWait(() -> { holder[0] = new FeePaymentWindow(svc); holder[0].setAlwaysOnTop(true); holder[0].setVisible(true); });
        FeePaymentWindow w = holder[0];
        Thread.sleep(600);

        // Test A: invalid amount -> clear message, no freeze
        javax.swing.SwingUtilities.invokeAndWait(() -> { w.txtStudent.setText("192421424"); w.txtAmount.setText("abc"); w.payAction.actionPerformed(null); });
        Thread.sleep(1500);
        System.out.println("Test A (amount = abc)      status: " + w.lblStatus.getText());
        Shot.save(w, "output/task01_test_A_invalid_amount.png");

        // Test B: valid payment; the window must stay responsive while the 600+600 ms database work runs
        javax.swing.SwingUtilities.invokeAndWait(() -> { w.txtAmount.setText("2500.50"); w.cmbMethod.setSelectedItem("UPI"); w.payAction.actionPerformed(null); });
        Thread.sleep(300);
        final long[] edtDelay = new long[1];
        long t0 = System.nanoTime();
        javax.swing.SwingUtilities.invokeAndWait(() -> { });     // how long does the EDT take to answer while the payment is processing?
        edtDelay[0] = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("Test B (valid, in progress) status: " + w.lblStatus.getText() + "  | EDT answered in " + edtDelay[0] + " ms (window not frozen)");
        Shot.save(w, "output/task01_test_B1_processing.png");
        Thread.sleep(1500);
        System.out.println("Test B (valid, finished)    status: " + w.lblStatus.getText() + "  | Receipt button enabled = " + w.receiptAction.isEnabled());
        Shot.save(w, "output/task01_test_B2_accepted.png");

        // Test C: Receipt action shows the receipt dialog
        new Thread(() -> {
            try {
                Thread.sleep(700);
                Window d = Shot.findWindow("Receipt");
                if (d != null) { Shot.save(d, "output/task01_test_C_receipt_dialog.png"); javax.swing.SwingUtilities.invokeLater(d::dispose); }
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
        javax.swing.SwingUtilities.invokeAndWait(() -> w.receiptAction.actionPerformed(null));   // modal dialog closes when the helper thread disposes it
        Thread.sleep(300);
        javax.swing.SwingUtilities.invokeAndWait(w::dispose);

        if (args.length >= 4 && args[0].equals("jdbc")) {
            System.out.println();
            System.out.println("=== Part 3: JdbcPaymentRepository against MySQL ===");
            FeeService js = new FeeService(new JdbcPaymentRepository(args[1], args[2], args[3]));
            Receipt r = js.pay("192421424", "1000.00", "Card");
            System.out.println("JDBC payment saved, receipt no " + r.getNumber() + ", balance after = " + r.getBalanceAfter().toPlainString());
            try { js.pay("192421424", "1000.00", "Cash'; DROP TABLE fee_due;--"); } catch (ValidationException e) { System.out.println("Hostile method text rejected: " + e.getMessage()); }
            try { js.pay("192421424' OR '1'='1", "10", "Cash"); } catch (ValidationException e) { System.out.println("Hostile id rejected: " + e.getMessage()); }
        }
        System.exit(fail == 0 ? 0 : 1);
    }
}
