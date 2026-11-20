import java.sql.*;
import java.util.*;

/**
 * Task B3 - insertion time for 1,000, 10,000 and 100,000 records with Statement, PreparedStatement and
 * PreparedStatement batch, with auto-commit and with ONE transaction (and the batch with rewriteBatchedStatements on/off).
 *   java -cp mysql-connector-j-8.4.0.jar;. InsertBench [capRowsForAutoCommit]
 * Auto-commit variants commit (and flush the log to disk) after EVERY row, so they are very slow; above the cap they are
 * measured on 'cap' rows and scaled linearly (marked "extrapolated").
 */
public class InsertBench {
    interface Method { void run(Connection c, List<BusPass> list) throws SQLException; }

    static void viaStatement(Connection c, List<BusPass> list) throws SQLException {
        try (Statement st = c.createStatement()) {
            for (BusPass p : list)
                st.executeUpdate("INSERT INTO bus_pass (pass_id, reg_no, name, route, stop, validity, amount) VALUES ('"
                        + p.passId + "','" + p.regNo + "','" + p.name + "','" + p.route + "','" + p.stop + "','" + p.validity + "'," + p.amount.toPlainString() + ")");
        }
    }

    static void viaPrepared(Connection c, List<BusPass> list) throws SQLException {
        Insert10Demo.insertWithPrepared(c, list);
    }

    static void viaBatch(Connection c, List<BusPass> list) throws SQLException { PassLoader.load(c, list); }

    static final class Variant {
        final String name; final Method m; final boolean autoCommit; final boolean rewrite;
        Variant(String name, Method m, boolean autoCommit, boolean rewrite) { this.name = name; this.m = m; this.autoCommit = autoCommit; this.rewrite = rewrite; }
    }

    public static void main(String[] args) throws Exception {
        int cap = args.length > 0 ? Integer.parseInt(args[0]) : 20_000;
        Variant[] variants = {
            new Variant("A. Statement, auto-commit (1 commit per row)", InsertBench::viaStatement, true, false),
            new Variant("B. Statement, ONE transaction", InsertBench::viaStatement, false, false),
            new Variant("C. PreparedStatement, auto-commit", InsertBench::viaPrepared, true, false),
            new Variant("D. PreparedStatement, ONE transaction", InsertBench::viaPrepared, false, false),
            new Variant("E. PS batch of 1,000, auto-commit", InsertBench::viaBatch, true, false),
            new Variant("F. PS batch of 1,000, ONE transaction", InsertBench::viaBatch, false, false),
            new Variant("G. PS batch, ONE transaction, rewriteBatchedStatements=true", InsertBench::viaBatch, false, true),
        };
        int[] sizes = {1_000, 10_000, 100_000};
        System.out.println("MySQL local, Java " + System.getProperty("java.version") + ", auto-commit variants capped at " + cap + " rows (scaled above that)");
        System.out.printf("%-62s %12s %12s %14s%n", "Method", "1,000 rows", "10,000 rows", "100,000 rows");
        Connection rw = PassData.open(true), plain = PassData.open(false);
        // warm-up
        for (int w = 0; w < 2; w++) { PassData.truncate(plain); plain.setAutoCommit(false); viaBatch(plain, PassData.generate(1000)); plain.commit(); plain.setAutoCommit(true); }
        StringBuilder csv = new StringBuilder("method,rows,ms,extrapolated\n");
        for (Variant v : variants) {
            StringBuilder line = new StringBuilder(String.format("%-62s", v.name));
            for (int n : sizes) {
                Connection c = v.rewrite ? rw : plain;
                int run = (v.autoCommit && n > cap) ? cap : n;
                List<BusPass> list = PassData.generate(run);
                PassData.truncate(c);
                c.setAutoCommit(v.autoCommit);
                long t0 = System.nanoTime();
                v.m.run(c, list);
                if (!v.autoCommit) c.commit();
                double ms = (System.nanoTime() - t0) / 1e6;
                long rows = PassData.count(c);
                c.setAutoCommit(true);
                if (rows != run) throw new IllegalStateException(v.name + ": expected " + run + " rows but table has " + rows);
                boolean extra = run != n;
                double shown = extra ? ms * n / run : ms;
                line.append(String.format(" %11s%s", String.format("%,.0f ms", shown), extra ? "*" : " "));
                csv.append(v.name.replace(",", ";")).append(',').append(n).append(',').append(String.format("%.1f", shown)).append(',').append(extra).append('\n');
            }
            System.out.println(line);
        }
        System.out.println("(* = measured on " + cap + " rows and scaled linearly to the full size; every row is committed separately, so cost per row is constant)");
        System.out.println();
        System.out.println("CSV data:");
        System.out.print(csv);
        rw.close(); plain.close();
    }
}
