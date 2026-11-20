import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.List;

/**
 * Task B3 - PreparedStatement batches compared with multi-row INSERT and MySQL LOAD DATA (LOCAL INFILE).
 * All variants load inside ONE transaction (LOAD DATA is a single statement). 10,000 and 100,000 rows, median of 3 runs.
 * LOAD DATA LOCAL needs: server local_infile=ON (SET GLOBAL local_infile=1) and the driver option allowLoadLocalInfile=true.
 */
public class BulkLoadBench {
    static final int ROWS_PER_STATEMENT = 500;

    static void multiRowInsert(Connection c, List<BusPass> list) throws SQLException {
        String one = "(?,?,?,?,?,?,?)";
        for (int from = 0; from < list.size(); from += ROWS_PER_STATEMENT) {
            int to = Math.min(from + ROWS_PER_STATEMENT, list.size());
            StringBuilder sql = new StringBuilder("INSERT INTO bus_pass (pass_id, reg_no, name, route, stop, validity, amount) VALUES ");
            for (int i = from; i < to; i++) sql.append(i > from ? "," : "").append(one);
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                int k = 1;
                for (int i = from; i < to; i++) {
                    BusPass p = list.get(i);
                    ps.setString(k++, p.passId); ps.setString(k++, p.regNo); ps.setString(k++, p.name); ps.setString(k++, p.route);
                    ps.setString(k++, p.stop); ps.setDate(k++, java.sql.Date.valueOf(p.validity)); ps.setBigDecimal(k++, p.amount);
                }
                ps.executeUpdate();
            }
        }
    }

    static void loadData(Connection c, List<BusPass> list, Path tsv) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(tsv, StandardCharsets.UTF_8)) {       // file creation time is part of the cost
            for (BusPass p : list) w.write(p.passId + "\t" + p.regNo + "\t" + p.name + "\t" + p.route + "\t" + p.stop + "\t" + p.validity + "\t" + p.amount.toPlainString() + "\n");
        }
        String sql = "LOAD DATA LOCAL INFILE '" + tsv.toAbsolutePath().toString().replace("\\", "/") + "' INTO TABLE bus_pass "
                + "FIELDS TERMINATED BY '\\t' LINES TERMINATED BY '\\n' (pass_id, reg_no, name, route, stop, validity, amount)";
        try (Statement st = c.createStatement()) { st.execute(sql); }
    }

    interface Run { void go(Connection c, List<BusPass> l) throws Exception; }

    static double time(Connection c, int n, Run r) throws Exception {
        double[] t = new double[3];
        for (int i = 0; i < 3; i++) {
            List<BusPass> list = PassData.generate(n);
            PassData.truncate(c);
            c.setAutoCommit(false);
            long t0 = System.nanoTime();
            r.go(c, list);
            c.commit();
            t[i] = (System.nanoTime() - t0) / 1e6;
            c.setAutoCommit(true);
            if (PassData.count(c) != n) throw new IllegalStateException("row count mismatch: " + PassData.count(c));
        }
        java.util.Arrays.sort(t);
        return t[1];
    }

    public static void main(String[] args) throws Exception {
        try (Connection plain = PassData.open(false); Connection rw = PassData.open(true)) {
            Path tsv = Path.of("passes.tsv");
            try (Statement st = plain.createStatement()) { st.execute("SET GLOBAL local_infile = 1"); }
            System.out.println("Median of 3 runs, one transaction, local MySQL. LOAD DATA time includes writing the temporary file.");
            System.out.printf("%-58s %14s %14s%n", "Method", "10,000 rows", "100,000 rows");
            String[] names = {"PreparedStatement batch of 1,000 (rewrite OFF)", "PreparedStatement batch of 1,000 (rewrite ON)", "Multi-row INSERT, " + ROWS_PER_STATEMENT + " rows per statement", "LOAD DATA LOCAL INFILE"};
            for (int m = 0; m < names.length; m++) {
                StringBuilder line = new StringBuilder(String.format("%-58s", names[m]));
                for (int n : new int[]{10_000, 100_000}) {
                    double ms;
                    switch (m) {
                        case 0 -> ms = time(plain, n, (c, l) -> PassLoader.load(c, l));
                        case 1 -> ms = time(rw, n, (c, l) -> PassLoader.load(c, l));
                        case 2 -> ms = time(plain, n, BulkLoadBench::multiRowInsert);
                        default -> ms = time(plain, n, (c, l) -> loadData(c, l, tsv));
                    }
                    line.append(String.format(" %11.1f ms", ms));
                }
                System.out.println(line);
            }
            Files.deleteIfExists(tsv);
            PassData.truncate(plain);
        }
    }
}
