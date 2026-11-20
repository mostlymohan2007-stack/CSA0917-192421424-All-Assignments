import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Task B1 - pass-search time as the file grows from 1,000 to 100,000 records, and
 * scan-per-find (O(q*n)) versus load-into-HashMap-once (O(n + q)).
 */
public class SearchBench {
    static long usedKb() {
        for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(40); } catch (InterruptedException e) { } }
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }

    static Path generate(int n) throws IOException {
        Path p = Path.of("passes_" + n + ".txt");
        try (var w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= n; i++) {
                BusPass bp = new BusPass("P" + i, String.valueOf(192000000 + i), "Student " + i, "R" + (1 + i % 40), "Stop " + (1 + i % 25),
                        LocalDate.of(2026, 12, 31), new java.math.BigDecimal(1000 + i % 1000 + ".00"));
                w.write(bp.toLine()); w.write('\n');
            }
        }
        return p;
    }

    static double median(double[] a) { double[] c = a.clone(); Arrays.sort(c); return c[c.length / 2]; }

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version"));
        int[] sizes = {1_000, 5_000, 10_000, 50_000, 100_000};
        int q = 1000;
        Random rnd = new Random(42);
        System.out.printf("%-9s %-10s %-22s %-22s %-18s %-14s %-16s%n", "records", "file KB", "scan: find LAST (ms)", "scan: find FIRST (ms)", "HashMap load (ms)", "lookup (ns)", "HashMap heap (KB)");
        StringBuilder totals = new StringBuilder();
        for (int n : sizes) {
            Path p = generate(n);
            PassStore store = new PassStore(p);
            for (int w = 0; w < 40; w++) { store.find("P" + n); store.loadAll(); }       // warm-up: JIT compilation + OS file cache
            double[] last = new double[7], first = new double[7];
            for (int i = 0; i < 7; i++) {
                long t0 = System.nanoTime(); store.find("P" + n); last[i] = (System.nanoTime() - t0) / 1e6;
                t0 = System.nanoTime(); store.find("P1"); first[i] = (System.nanoTime() - t0) / 1e6;
            }
            long heapBefore = usedKb();
            long t0 = System.nanoTime();
            Map<String, BusPass> map = store.loadAll();
            double loadMs = (System.nanoTime() - t0) / 1e6;
            long heapKb = usedKb() - heapBefore;
            String[] ids = new String[q];
            for (int i = 0; i < q; i++) ids[i] = "P" + (1 + rnd.nextInt(n));
            for (int w = 0; w < 3; w++) for (String id : ids) map.get(id);
            t0 = System.nanoTime();
            int found = 0;
            for (int rep = 0; rep < 100; rep++) for (String id : ids) if (map.get(id) != null) found++;
            double lookupNs = (System.nanoTime() - t0) / (100.0 * q);

            // q random searches by scanning (limit the work for the largest sizes by measuring 50 scans and scaling)
            int scans = Math.min(q, n >= 50_000 ? 50 : 200);
            t0 = System.nanoTime();
            for (int i = 0; i < scans; i++) store.find(ids[i]);
            double perScanMs = (System.nanoTime() - t0) / 1e6 / scans;
            double scanTotal = perScanMs * q;
            double mapTotal = loadMs + q * lookupNs / 1e6;
            System.out.printf("%-9d %-10d %-22.2f %-22.2f %-18.1f %-14.1f %-16d%n", n, Files.size(p) / 1024, median(last), median(first), loadMs, lookupNs, Math.max(heapKb, 0));
            totals.append(String.format("   n=%-7d q=%d random searches: scan-per-find = %10.1f ms  (%.2f ms per search)   HashMap (load + lookups) = %8.1f ms   speed-up %.0fx%n", n, q, scanTotal, perScanMs, mapTotal, scanTotal / mapTotal));
            Files.deleteIfExists(p);
        }
        System.out.println();
        System.out.println("q = " + q + " random searches (scan time measured on a sample of searches and scaled to q):");
        System.out.print(totals);
    }
}
