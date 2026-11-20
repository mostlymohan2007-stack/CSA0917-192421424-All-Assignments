import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/**
 * Task B4 - evidence for the final engineering decision. The expanded system: 12 campuses, 100 x the current
 * 8,000 passes = 800,000 passes. Measures text file, Java serialization and MySQL on: loading, search, listing a route,
 * concurrent counters, and the "12 separate serialized files versus one shared database" question.
 *   java -Xmx4g -cp mysql-connector-j-8.4.0.jar;. DecisionBench
 */
public class DecisionBench {
    static final int N = 800_000;
    static final int CAMPUSES = 12;

    static long usedMb() {
        for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(50); } catch (InterruptedException e) { } }
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
    }

    static BusPass make(int i) {
        return new BusPass("P" + i, String.valueOf(192000000 + i), "Student " + i, "R" + (1 + i % 40), "Stop " + (1 + i % 25),
                LocalDate.of(2026, 12, 31), new BigDecimal((1000 + i % 1000) + ".00"));
    }

    static double ms(long t0) { return (System.nanoTime() - t0) / 1e6; }

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version") + ", max heap " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB, cores " + Runtime.getRuntime().availableProcessors());
        System.out.println("Expanded system: " + CAMPUSES + " campuses, " + String.format("%,d", N) + " passes");
        Path txt = Path.of("passes_800k.txt"), ser = Path.of("passes_800k.ser");
        Random rnd = new Random(7);

        // ---------------------------------------------------------------- build the three stores
        System.out.println();
        System.out.println("=== 1. Building the stores ===");
        long t0 = System.nanoTime();
        try (BufferedWriter w = Files.newBufferedWriter(txt, StandardCharsets.UTF_8)) { for (int i = 1; i <= N; i++) { w.write(make(i).toLine()); w.write('\n'); } }
        System.out.printf("text file written       : %8.0f ms, size %,d KB%n", ms(t0), Files.size(txt) / 1024);

        List<BusPass> list = new ArrayList<>(N);
        for (int i = 1; i <= N; i++) list.add(make(i));
        t0 = System.nanoTime();
        try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(ser)))) { o.writeObject(list); }
        double serWriteMs = ms(t0);
        System.out.printf("serialized file written : %8.0f ms, size %,d KB   <- this is also the cost of EVERY change if the whole list is re-saved%n", serWriteMs, Files.size(ser) / 1024);
        list = null;

        double dbLoadMs;
        try (Connection c = PassData.open(true)) {
            PassData.truncate(c);
            c.setAutoCommit(false);
            t0 = System.nanoTime();
            PassLoader.load(c, PassData.generate(N));
            c.commit();
            dbLoadMs = ms(t0);
            c.setAutoCommit(true);
            System.out.printf("MySQL table loaded      : %8.0f ms (batches of 1,000, one transaction), rows = %,d%n", dbLoadMs, PassData.count(c));
        }

        // ---------------------------------------------------------------- start-up cost and memory
        System.out.println();
        System.out.println("=== 2. Start-up: what must be done before the FIRST search can be answered? ===");
        long base = usedMb();
        t0 = System.nanoTime();
        Map<String, BusPass> map = new PassStore(txt).loadAll();
        double mapLoad = ms(t0);
        long mapMb = usedMb() - base;
        System.out.printf("text file -> HashMap          : %8.0f ms, heap held %,d MB%n", mapLoad, mapMb);
        map = null;
        base = usedMb();
        t0 = System.nanoTime();
        List<BusPass> back;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(ser)))) { back = (List<BusPass>) in.readObject(); }
        double serLoad = ms(t0);
        long serMb = usedMb() - base;
        System.out.printf("serialized file -> ArrayList  : %8.0f ms, heap held %,d MB (%,d objects)%n", serLoad, serMb, back.size());
        back = null;
        System.out.println("MySQL                         :        0 ms (data stays on the server; the first query can run immediately)");

        // ---------------------------------------------------------------- single search
        System.out.println();
        System.out.println("=== 3. Searching ===");
        PassStore store = new PassStore(txt);
        for (int w = 0; w < 2; w++) store.find("P" + N);
        double scanLast = 0, scanMid = 0;
        for (int i = 0; i < 5; i++) { t0 = System.nanoTime(); store.find("P" + N); scanLast += ms(t0) / 5; t0 = System.nanoTime(); store.find("P" + (N / 2)); scanMid += ms(t0) / 5; }
        System.out.printf("text file scan, find the middle record : %9.1f ms per search%n", scanMid);
        System.out.printf("text file scan, find the LAST record   : %9.1f ms per search%n", scanLast);
        Map<String, BusPass> hm = new PassStore(txt).loadAll();
        String[] ids = new String[1000];
        for (int i = 0; i < ids.length; i++) ids[i] = "P" + (1 + rnd.nextInt(N));
        for (int w = 0; w < 3; w++) for (String id : ids) hm.get(id);
        t0 = System.nanoTime();
        for (int rep = 0; rep < 200; rep++) for (String id : ids) hm.get(id);
        System.out.printf("HashMap lookup (after loading)         : %9.4f ms per search (%.0f ns)%n", ms(t0) / 200 / ids.length, ms(t0) * 1e6 / 200 / ids.length);
        double listScan; int listRows;
        t0 = System.nanoTime();
        listRows = store.listByRoute("R12").size();
        listScan = ms(t0);
        hm = null;
        try (Connection c = PassData.open(false); PreparedStatement ps = c.prepareStatement("SELECT * FROM bus_pass WHERE pass_id = ?")) {
            for (int w = 0; w < 200; w++) { ps.setString(1, ids[w % ids.length]); try (ResultSet rs = ps.executeQuery()) { rs.next(); } }
            t0 = System.nanoTime();
            int found = 0;
            for (int i = 0; i < ids.length; i++) { ps.setString(1, ids[i]); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) found++; } }
            System.out.printf("MySQL, primary-key lookup (shared conn): %9.4f ms per search (%d of %d found)%n", ms(t0) / ids.length, found, ids.length);
            try (PreparedStatement lp = c.prepareStatement("SELECT * FROM bus_pass WHERE route = ?")) {
                lp.setString(1, "R12");
                t0 = System.nanoTime();
                int rows = 0;
                try (ResultSet rs = lp.executeQuery()) { while (rs.next()) rows++; }
                System.out.printf("list route R12: text scan %,.1f ms (%,d records)   MySQL with index idx_route %,.1f ms (%,d rows)%n", listScan, listRows, ms(t0), rows);
            }
        }

        // ---------------------------------------------------------------- concurrent counters
        System.out.println();
        System.out.println("=== 4. Concurrent counters: 8 counter threads x 100 passes issued = 800 issues ===");
        int threads = 8, each = 100;
        Path live = Path.of("issued_live.txt");
        Files.deleteIfExists(live);
        PassStore liveStore = new PassStore(live);
        Object lock = new Object();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int id = t;
            fs.add(pool.submit(() -> {
                go.await();
                for (int i = 0; i < each; i++) {
                    BusPass p = new BusPass("T" + id + "N" + i, "1920" + String.format("%05d", id * each + i), "Counter pass", "R1", "Main Gate", LocalDate.of(2026, 12, 31), new BigDecimal("1500.00"));
                    synchronized (lock) { liveStore.add(p, true); }          // one lock for the file + force to disk
                }
                return null;
            }));
        }
        t0 = System.nanoTime(); go.countDown();
        for (Future<?> f : fs) f.get();
        double fileMs = ms(t0);
        long fileLines = Files.readAllLines(live).size();
        System.out.printf("text file (append + sync, one JVM lock) : %8.0f ms  -> %,.0f passes/s   (lines written: %d)%n", fileMs, threads * each * 1000.0 / fileMs, fileLines);
        pool.shutdown();

        double rewrite = serWriteMs;
        System.out.printf("serialized file (whole list re-saved per issue): one save = %.0f ms -> at most %.1f passes/s, and concurrent writers would overwrite each other%n", rewrite, 1000.0 / rewrite);

        pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go2 = new CountDownLatch(1);
        List<Connection> conns = new ArrayList<>();
        for (int t = 0; t < threads; t++) conns.add(PassData.open(false));
        fs = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int id = t; final Connection c = conns.get(t);
            fs.add(pool.submit(() -> {
                go2.await();
                try (PreparedStatement ps = c.prepareStatement(PassLoader.INSERT_SQL)) {
                    for (int i = 0; i < each; i++) {
                        ps.setString(1, "T" + id + "N" + i); ps.setString(2, "1920" + String.format("%05d", id * each + i)); ps.setString(3, "Counter pass");
                        ps.setString(4, "R1"); ps.setString(5, "Main Gate"); ps.setDate(6, java.sql.Date.valueOf(LocalDate.of(2026, 12, 31))); ps.setBigDecimal(7, new BigDecimal("1500.00"));
                        ps.executeUpdate();                                     // auto-commit: each counter's issue is its own safe transaction
                    }
                }
                return null;
            }));
        }
        t0 = System.nanoTime(); go2.countDown();
        for (Future<?> f : fs) f.get();
        double dbMs = ms(t0);
        long dbRows;
        try (Connection c = PassData.open(false)) { try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass WHERE pass_id LIKE 'T%N%'")) { rs.next(); dbRows = rs.getLong(1); }
            // duplicate pass id from two counters: the database decides, exactly one wins
            try (PreparedStatement ps = c.prepareStatement(PassLoader.INSERT_SQL)) {
                int ok = 0, dup = 0;
                for (int k = 0; k < 2; k++) {
                    ps.setString(1, "T0N0"); ps.setString(2, "192099999"); ps.setString(3, "Duplicate"); ps.setString(4, "R1"); ps.setString(5, "Gate");
                    ps.setDate(6, java.sql.Date.valueOf(LocalDate.of(2026, 12, 31))); ps.setBigDecimal(7, new BigDecimal("1500.00"));
                    try { ps.executeUpdate(); ok++; } catch (SQLIntegrityConstraintViolationException e) { dup++; }
                }
                System.out.printf("MySQL, 8 connections, auto-commit per issue : %8.0f ms  -> %,.0f passes/s   (rows stored: %d)  | issuing an existing pass id again: accepted=%d, rejected by PRIMARY KEY=%d%n", dbMs, threads * each * 1000.0 / dbMs, dbRows, ok, dup);
            }
        }
        for (Connection c : conns) c.close();
        pool.shutdown();
        Files.deleteIfExists(live);

        // ---------------------------------------------------------------- one shared database versus 12 serialized files
        System.out.println();
        System.out.println("=== 5. One shared database versus separate serialized files for each campus ===");
        int per = N / CAMPUSES;
        List<Path> campusFiles = new ArrayList<>();
        t0 = System.nanoTime();
        for (int cpus = 0; cpus < CAMPUSES; cpus++) {
            List<BusPass> part = new ArrayList<>(per);
            for (int i = 1; i <= per; i++) part.add(make(cpus * per + i));
            Path p = Path.of("campus_" + (cpus + 1) + ".ser");
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(p)))) { o.writeObject(part); }
            campusFiles.add(p);
        }
        System.out.printf("12 campus files created: %.0f ms, about %,d KB each (%,d passes each)%n", ms(t0), Files.size(campusFiles.get(0)) / 1024, per);
        t0 = System.nanoTime();
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(campusFiles.get(0))))) { ((List<?>) in.readObject()).size(); }
        double oneLoad = ms(t0);
        System.out.printf("one campus: open + deserialize its file  : %8.0f ms%n", oneLoad);
        String target = "P" + (N - 5);                       // the pass lives in campus 12: a cross-campus search must open files one after another
        t0 = System.nanoTime();
        boolean found = false;
        for (Path p : campusFiles) {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(p)))) {
                for (Object o : (List<?>) in.readObject()) if (((BusPass) o).passId.equals(target)) { found = true; break; }
            }
            if (found) break;
        }
        double cross = ms(t0);
        System.out.printf("search a pass of unknown campus (worst case, campus 12): %8.0f ms over up to 12 files   vs  MySQL primary-key lookup: about 0.2-0.5 ms%n", cross);
        for (Path p : campusFiles) Files.deleteIfExists(p);

        Files.deleteIfExists(txt);
        Files.deleteIfExists(ser);
        try (Connection c = PassData.open(false)) { PassData.truncate(c); }
    }
}
