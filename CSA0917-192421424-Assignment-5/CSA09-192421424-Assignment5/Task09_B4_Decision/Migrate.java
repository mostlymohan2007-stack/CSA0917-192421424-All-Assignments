import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * Task B4 - migration program: reads the existing pass file, validates every line, loads the valid records into MySQL
 * in batches of 1,000 inside ONE transaction, reports every rejected line, and prints DatabaseMetaData.
 *   java -cp mysql-connector-j-8.4.0.jar;. Migrate passes_100000.txt jdbc:mysql://127.0.0.1:3307/transport_test root "" rejects.txt
 * Policy: INVALID LINES ARE REJECTED AND REPORTED (they never reach the database); if the DATABASE itself fails,
 * the whole transaction is rolled back so the table is never half-loaded.
 */
public class Migrate {
    static final int BATCH = 1000;
    static final String INSERT_SQL = "INSERT INTO bus_pass (pass_id, reg_no, name, route, stop, validity, amount) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]);
        String url = args[1] + "?rewriteBatchedStatements=true&useSSL=false&allowPublicKeyRetrieval=true";
        Path rejectsFile = Path.of(args.length > 4 ? args[4] : "migration_rejects.txt");
        long t0 = System.nanoTime();

        Map<String, Integer> rejectByReason = new TreeMap<>();
        Set<String> seen = new HashSet<>();
        int lines = 0, valid = 0, rejected = 0, batches = 0;

        try (Connection con = DriverManager.getConnection(url, args[2], args[3]);
             BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
             BufferedWriter rejects = Files.newBufferedWriter(rejectsFile, StandardCharsets.UTF_8);
             PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {

            try (Statement st = con.createStatement()) { st.execute("TRUNCATE TABLE bus_pass"); }
            con.setAutoCommit(false);                                              // ONE transaction for the whole migration
            try {
                String line;
                int inBatch = 0;
                rejects.write("line_no | reason | text\n");
                while ((line = reader.readLine()) != null) {
                    lines++;
                    String reason = null;
                    BusPass p = null;
                    try {
                        p = BusPass.parse(line);
                        if (!seen.add(p.passId)) reason = "duplicate pass id " + p.passId;
                    } catch (DamagedRecordException e) {
                        reason = e.getMessage();
                    }
                    if (reason != null) {
                        rejected++;
                        rejectByReason.merge(reason.replaceAll("'[^']*'", "'..'").replaceAll("P\\d+", "P..").replaceAll("-?\\d+\\.\\d+", "N"), 1, Integer::sum);
                        rejects.write(lines + " | " + reason + " | " + (line.length() > 70 ? line.substring(0, 70) + "..." : line) + "\n");
                        continue;
                    }
                    ps.setString(1, p.passId); ps.setString(2, p.regNo); ps.setString(3, p.name); ps.setString(4, p.route);
                    ps.setString(5, p.stop); ps.setDate(6, java.sql.Date.valueOf(p.validity)); ps.setBigDecimal(7, p.amount);
                    ps.addBatch();
                    valid++;
                    if (++inBatch == BATCH) { ps.executeBatch(); batches++; inBatch = 0; }
                }
                if (inBatch > 0) { ps.executeBatch(); batches++; }
                con.commit();
                System.out.println("Transaction COMMITTED.");
            } catch (SQLException e) {
                con.rollback();
                System.out.println("DATABASE ERROR - transaction ROLLED BACK, table unchanged: " + e.getMessage());
                throw e;
            }

            double ms = (System.nanoTime() - t0) / 1e6;
            long inTable;
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) { rs.next(); inTable = rs.getLong(1); }

            System.out.println();
            System.out.println("=== Migration report ===");
            System.out.println("Input file            : " + in + " (" + Files.size(in) / 1024 + " KB)");
            System.out.println("Lines read            : " + lines);
            System.out.println("Valid records loaded  : " + valid + "   (" + batches + " batches of up to " + BATCH + ")");
            System.out.println("Rejected lines        : " + rejected + "   (details in " + rejectsFile + ")");
            for (Map.Entry<String, Integer> e : rejectByReason.entrySet()) System.out.println("      " + String.format("%5d", e.getValue()) + " x " + e.getKey());
            System.out.println("Rows in bus_pass      : " + inTable + (inTable == valid ? "   -> matches the number of valid records" : "   -> MISMATCH!"));
            System.out.printf("Total time            : %.0f ms (%.0f records/second)%n", ms, lines / (ms / 1000.0));

            DatabaseMetaData md = con.getMetaData();
            System.out.println();
            System.out.println("=== DatabaseMetaData ===");
            System.out.println("Database product      : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("Driver                : " + md.getDriverName() + " " + md.getDriverVersion());
            System.out.println("JDBC version          : " + md.getJDBCMajorVersion() + "." + md.getJDBCMinorVersion());
            System.out.println("URL / user            : " + md.getURL().replaceAll("\\?.*", "") + " / " + md.getUserName());
            System.out.println("Supports transactions : " + md.supportsTransactions() + ",  batch updates: " + md.supportsBatchUpdates());
            System.out.print("Available tables      : ");
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = md.getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
            }
            System.out.println(tables + "  (catalog " + con.getCatalog() + ")");
        }
    }
}
