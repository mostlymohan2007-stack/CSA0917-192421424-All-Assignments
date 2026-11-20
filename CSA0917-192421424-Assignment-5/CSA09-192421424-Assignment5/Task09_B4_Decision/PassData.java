import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Test-data generator and small JDBC helpers shared by the B3 programs. */
public class PassData {
    static final String BASE = "jdbc:mysql://127.0.0.1:3307/transport_test";

    public static List<BusPass> generate(int n) {
        List<BusPass> list = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            list.add(new BusPass("P" + i, String.valueOf(192000000 + i), "Student " + i, "R" + (1 + i % 40), "Stop " + (1 + i % 25),
                    LocalDate.of(2026, 12, 31), new BigDecimal((1000 + i % 1000) + ".00")));
        }
        return list;
    }

    public static Connection open(boolean rewrite) throws SQLException {
        String url = System.getProperty("db.url", BASE) + "?rewriteBatchedStatements=" + rewrite + "&allowLoadLocalInfile=true&useSSL=false&allowPublicKeyRetrieval=true";
        return DriverManager.getConnection(url, System.getProperty("db.user", "root"), System.getProperty("db.password", ""));
    }

    public static void truncate(Connection c) throws SQLException {
        boolean ac = c.getAutoCommit();
        c.setAutoCommit(true);
        try (Statement st = c.createStatement()) { st.execute("TRUNCATE TABLE bus_pass"); }
        c.setAutoCommit(ac);
    }

    public static long count(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) { rs.next(); return rs.getLong(1); }
    }
}
