package campus.test;

import campus.dao.Db;
import campus.dao.JdbcRegistrationDao;
import campus.model.RegistrationRow;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Task 6 - "load all 100,000 registrations at once" versus "paginated loading with SQL LIMIT / OFFSET".
 * Measures loading time, heap memory and window responsiveness (the longest time the Event Dispatch Thread was blocked).
 * Run seed_100000.sql first. Numbers depend on your computer, MySQL and JVM: quote your own results.
 */
public class LoadBenchmark {

    /** Ticks every 10 ms on the EDT; the largest gap between two ticks shows how long the EDT was blocked. */
    static final class EdtProbe {
        volatile long maxGapMs = 0;
        private long last = 0;
        private final Timer timer = new Timer(10, e -> {
            long now = System.nanoTime();
            if (last != 0) maxGapMs = Math.max(maxGapMs, (now - last) / 1_000_000);
            last = now;
        });
        void start() throws Exception { maxGapMs = 0; last = 0; SwingUtilities.invokeAndWait(timer::start); Thread.sleep(100); maxGapMs = 0; }
        long stop() throws Exception { Thread.sleep(50); SwingUtilities.invokeAndWait(timer::stop); return maxGapMs; }
    }

    static long usedHeapMb() {
        for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(50); } catch (InterruptedException e) { } }
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
    }

    public static void main(String[] args) throws Exception {
        Db db = new Db();
        JdbcRegistrationDao dao = new JdbcRegistrationDao(db);
        System.out.println("Database: " + db.describe() + "   Java " + System.getProperty("java.version"));
        System.out.println("Rows in table: " + dao.count(null));
        System.out.println();
        EdtProbe probe = new EdtProbe();
        dao.findPage(null, 25, 0);              // warm-up: JDBC driver, JIT

        // ---------- A. everything at once, ON the EDT (what a careless handler does) ----------
        System.out.println("A. Load ALL rows ON the Event Dispatch Thread (window blocked):");
        long base = usedHeapMb();
        probe.start();
        long t0 = System.nanoTime();
        final List<RegistrationRow>[] all = new List[1];
        SwingUtilities.invokeAndWait(() -> { try { all[0] = dao.findAll(null); } catch (Exception e) { throw new RuntimeException(e); } });
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long gap = probe.stop();
        long heap = usedHeapMb() - base;
        System.out.printf("   rows=%d  time=%d ms  heap held=%d MB  longest EDT freeze=%d ms%n", all[0].size(), ms, heap, gap);
        all[0] = null;

        // ---------- B. everything at once, in a background thread (SwingWorker style) ----------
        System.out.println("B. Load ALL rows in a BACKGROUND thread (SwingWorker), then hand them to the table:");
        base = usedHeapMb();
        probe.start();
        t0 = System.nanoTime();
        final List<RegistrationRow> bg = dao.findAll(null);
        ms = (System.nanoTime() - t0) / 1_000_000;
        long t1 = System.nanoTime();
        final Object[] keep = new Object[1];
        SwingUtilities.invokeAndWait(() -> { javax.swing.table.DefaultTableModel m = new javax.swing.table.DefaultTableModel(new String[]{"Reg", "Student", "Name", "Event", "At"}, 0);
            for (RegistrationRow r : bg) m.addRow(new Object[]{r.regId, r.studentId, r.studentName, r.eventTitle, r.registeredAt}); keep[0] = m; });
        long modelMs = (System.nanoTime() - t1) / 1_000_000;
        gap = probe.stop();
        heap = usedHeapMb() - base;
        System.out.printf("   rows=%d  query+read time=%d ms  filling a table model on the EDT=%d ms  heap held (list + table model)=%d MB  longest EDT freeze=%d ms%n", bg.size(), ms, modelMs, heap, gap);
        keep[0] = null;

        // ---------- C. one page of 25 rows with LIMIT/OFFSET, background ----------
        System.out.println("C. Paginated: one page of 25 rows (SELECT ... ORDER BY reg_id LIMIT 25 OFFSET n), average of 30 calls, includes COUNT(*) separately:");
        base = usedHeapMb();
        probe.start();
        int[] offsets = {0, 50_000, 99_975};
        for (int off : offsets) {
            long sum = 0;
            for (int i = 0; i < 30; i++) { long s = System.nanoTime(); dao.findPage(null, 25, off); sum += System.nanoTime() - s; }
            System.out.printf("   OFFSET %6d : %7.2f ms per page%n", off, sum / 30 / 1e6);
        }
        long sum = 0;
        for (int i = 0; i < 30; i++) { long s = System.nanoTime(); dao.count(null); sum += System.nanoTime() - s; }
        System.out.printf("   COUNT(*) all events : %7.2f ms%n", sum / 30 / 1e6);
        gap = probe.stop();
        List<RegistrationRow> page = dao.findPage(null, 25, 50_000);
        heap = usedHeapMb() - base;
        System.out.printf("   rows held at a time=%d  heap held=%d MB (compared with the full load)  longest EDT freeze=%d ms%n", page.size(), Math.max(heap, 0), gap);

        // ---------- D. keyset pagination (alternative to OFFSET) ----------
        System.out.println("D. Alternative: keyset pagination (WHERE reg_id > last ORDER BY reg_id LIMIT 25), average of 30 calls:");
        for (long last : new long[]{0, 50_000, 99_975}) {
            long s2 = 0;
            for (int i = 0; i < 30; i++) { long s = System.nanoTime(); dao.findPageAfter(null, last, 25); s2 += System.nanoTime() - s; }
            System.out.printf("   after reg_id %6d : %7.2f ms per page%n", last, s2 / 30 / 1e6);
        }

        // ---------- E. streaming CSV export of all rows (memory stays small) ----------
        System.out.println("E. Streaming CSV export of ALL rows (no list is built):");
        base = usedHeapMb();
        Path csv = Files.createTempFile("export_all", ".csv");
        t0 = System.nanoTime();
        int[] n = {0};
        try (java.io.BufferedWriter w = Files.newBufferedWriter(csv)) {
            dao.forEach(null, r -> { try { w.write(r.regId + "," + r.studentId + "," + r.studentName + "," + r.eventTitle + "," + r.registeredAt + "\n"); n[0]++; } catch (java.io.IOException e) { throw new RuntimeException(e); } });
        }
        System.out.printf("   rows written=%d  time=%d ms  file size=%d KB%n", n[0], (System.nanoTime() - t0) / 1_000_000, Files.size(csv) / 1024);
        Files.deleteIfExists(csv);
        System.exit(0);
    }
}
