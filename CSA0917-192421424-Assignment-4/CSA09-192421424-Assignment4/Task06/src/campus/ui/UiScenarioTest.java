package campus.ui;

import campus.dao.Db;
import campus.dao.JdbcRegistrationDao;
import campus.service.RegistrationService;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/**
 * Task 6 - drives the real window (events are dispatched on the Event Dispatch Thread, as a user would trigger them)
 * and saves screenshots of: normal registration, invalid input, duplicate registration, the registrations table
 * with filter and paging, the File menu, and the CSV export. Run seed_100000.sql first to see paging.
 */
public class UiScenarioTest {

    static void edt(Runnable r) throws Exception { EventQueue.invokeAndWait(r); }

    static boolean onEdt(BooleanSupplier s) throws Exception {
        boolean[] b = new boolean[1];
        EventQueue.invokeAndWait(() -> b[0] = s.getAsBoolean());
        return b[0];
    }

    static void waitFor(String what, BooleanSupplier cond) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        while (System.currentTimeMillis() < end) {
            if (onEdt(cond)) return;
            Thread.sleep(50);
        }
        throw new IllegalStateException("timeout waiting for " + what);
    }

    static void fill(MainFrame f, String id, String name, String email, String dept, int eventIndex) throws Exception {
        edt(() -> {
            f.registerPanel.txtId.setText(id); f.registerPanel.txtName.setText(name); f.registerPanel.txtEmail.setText(email);
            f.registerPanel.cmbDept.setSelectedItem(dept); f.registerPanel.cmbEvent.setSelectedIndex(eventIndex);
        });
    }

    static String clickRegister(MainFrame f) throws Exception {
        edt(() -> f.registerPanel.btnRegister.doClick());
        long t0 = System.nanoTime();
        waitFor("registration to finish", () -> f.registerPanel.btnRegister.isEnabled());
        long ms = (System.nanoTime() - t0) / 1_000_000;
        String[] s = new String[1];
        edt(() -> s[0] = f.registerPanel.lblStatus.getText());
        return s[0] + "   [" + ms + " ms]";
    }

    static void reloadAndWait(MainFrame f, Runnable trigger) throws Exception {
        edt(trigger);
        waitFor("table to load", () -> !f.registrationsPanel.busy.isVisible() && f.registrationsPanel.model.getRowCount() > 0);
        Thread.sleep(200);
    }

    public static void main(String[] args) throws Exception {
        Db db = new Db();
        RegistrationService svc = new RegistrationService(new JdbcRegistrationDao(db));
        System.out.println("Database: " + db.describe());
        MainFrame[] h = new MainFrame[1];
        edt(() -> { h[0] = new MainFrame(svc); h[0].setAlwaysOnTop(true); h[0].setLocation(120, 60); h[0].setVisible(true); h[0].start(); });
        MainFrame f = h[0];
        waitFor("events to load", () -> f.registerPanel.cmbEvent.getItemCount() > 0 && !f.registrationsPanel.busy.isVisible() && f.registrationsPanel.model.getRowCount() > 0);
        Thread.sleep(300);

        System.out.println("=== Screen 1: Register ===");
        edt(() -> f.tabs.setSelectedIndex(0));
        Shot.save(f, "output/task06_01_register_screen.png");

        fill(f, "U100001", "Meena Iyer", "meena@college.edu", "CSE", 1);
        System.out.println("Normal registration  : " + clickRegister(f));
        Shot.save(f, "output/task06_02_normal_registration.png");

        fill(f, "U100002", "", "x@college.edu", "ECE", 1);
        System.out.println("Invalid (empty name) : " + clickRegister(f));
        Shot.save(f, "output/task06_03_invalid_input.png");

        fill(f, "U100003", "Ravi Kumar", "ravi-at-college", "ECE", 1);
        System.out.println("Invalid (bad e-mail) : " + clickRegister(f));

        fill(f, "U100001", "Meena Iyer", "meena@college.edu", "CSE", 1);
        System.out.println("Duplicate            : " + clickRegister(f));
        Shot.save(f, "output/task06_04_duplicate_registration.png");

        System.out.println("=== Screen 2: Registrations (committee view) ===");
        reloadAndWait(f, () -> { f.tabs.setSelectedIndex(1); f.registrationsPanel.reload(0); });
        String[] st = new String[2];
        edt(() -> { st[0] = f.registrationsPanel.lblStatus.getText(); st[1] = f.registrationsPanel.lblPage.getText(); });
        System.out.println("All events, page 1   : " + st[1] + " | " + st[0]);
        Shot.save(f, "output/task06_05_registrations_all_events.png");

        reloadAndWait(f, () -> f.registrationsPanel.btnNext.doClick());
        edt(() -> { st[0] = f.registrationsPanel.lblStatus.getText(); st[1] = f.registrationsPanel.lblPage.getText(); });
        System.out.println("Next page            : " + st[1] + " | " + st[0]);

        reloadAndWait(f, () -> f.registrationsPanel.cmbFilter.setSelectedIndex(2));          // 2 = "Hackathon 2026"
        edt(() -> { st[0] = f.registrationsPanel.lblStatus.getText(); st[1] = f.registrationsPanel.lblPage.getText(); });
        System.out.println("Filter Hackathon 2026: " + st[1] + " | " + st[0]);
        Shot.save(f, "output/task06_06_registrations_filtered.png");

        edt(() -> f.getJMenuBar().getMenu(0).doClick());
        Thread.sleep(400);
        Shot.save(f, "output/task06_07_file_menu.png");
        edt(() -> MenuSelectionManager.defaultManager().clearSelectedPath());

        Path csv = Path.of("output/export_hackathon.csv");
        edt(() -> f.exportTo(csv));
        waitFor("export", () -> { String t = f.registrationsPanel.lblStatus.getText(); return t.startsWith("Exported") || t.startsWith("Export failed"); });
        edt(() -> st[0] = f.registrationsPanel.lblStatus.getText());
        long lines = Files.lines(csv).count();
        System.out.println("Export CSV           : " + st[0] + " | file lines (incl. header) = " + lines);
        System.out.println("First CSV lines      : ");
        try (var s = Files.lines(csv)) { s.limit(3).forEach(l -> System.out.println("     " + l)); }
        Shot.save(f, "output/task06_08_export_done.png");

        edt(f::dispose);
        System.exit(0);
    }
}
