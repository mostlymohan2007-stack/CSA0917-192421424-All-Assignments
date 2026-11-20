import java.sql.*;
import java.util.*;

/**
 * Task 2 - compares "query the database every time the department changes" with "load all doctors once into a Map".
 *   java -cp mysql-connector-j-8.4.0.jar;. DoctorLoadBench URL USER PASSWORD
 * Timing depends on the machine and on MySQL; re-run on your computer and quote your own numbers.
 */
public class DoctorLoadBench {
    static final String[] DEPTS = {"General Medicine", "Cardiology", "Orthopedics", "Pediatrics"};

    static List<String> queryDoctors(Connection c, String dept) throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT name FROM doctor WHERE department = ?")) {
            ps.setString(1, dept);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getString(1)); }
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        String url = args[0], user = args[1], pass = args[2];
        int changes = 2000;

        // Option A: new connection + query for every department change (what a careless handler does)
        long t0 = System.nanoTime();
        int rowsA = 0;
        for (int i = 0; i < changes / 10; i++) {                       // only 200 because it is slow
            try (Connection c = DriverManager.getConnection(url, user, pass)) { rowsA += queryDoctors(c, DEPTS[i % 4]).size(); }
        }
        double perA = (System.nanoTime() - t0) / 1e6 / (changes / 10);

        // Option B: one shared connection, query every time
        double perB;
        try (Connection c = DriverManager.getConnection(url, user, pass)) {
            queryDoctors(c, DEPTS[0]);                                  // warm-up
            t0 = System.nanoTime();
            int rowsB = 0;
            for (int i = 0; i < changes; i++) rowsB += queryDoctors(c, DEPTS[i % 4]).size();
            perB = (System.nanoTime() - t0) / 1e6 / changes;
        }

        // Option C: load once into a Map, then look up
        Map<String, List<String>> map = new HashMap<>();
        t0 = System.nanoTime();
        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT department, name FROM doctor")) {
            while (rs.next()) map.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
        }
        double loadOnceMs = (System.nanoTime() - t0) / 1e6;
        for (int i = 0; i < 10000; i++) map.get(DEPTS[i % 4]);        // warm-up
        t0 = System.nanoTime();
        int rowsC = 0;
        int lookups = 1_000_000;
        for (int i = 0; i < lookups; i++) rowsC += map.get(DEPTS[i & 3]).size();
        double perCns = (double) (System.nanoTime() - t0) / lookups;

        System.out.println("Department changes simulated: " + changes + " (Option A used " + changes / 10 + ")");
        System.out.printf("A. new connection + query each time : %9.3f ms per change%n", perA);
        System.out.printf("B. shared connection, query each time: %9.3f ms per change%n", perB);
        System.out.printf("C. load once (%.3f ms one-time) then Map lookup: %9.1f ns per change%n", loadOnceMs, perCns);
        System.out.printf("Map lookup is about %,.0f times faster than a query on a shared connection (rows seen: %d)%n", perB * 1e6 / perCns, rowsC);
    }
}
