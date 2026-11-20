import java.sql.*;
import java.util.List;

/**
 * Task B3 - inserts 10 records with Statement, PreparedStatement and PreparedStatement batch (PassLoader),
 * verifies the table contents with SELECT and the column types with ResultSetMetaData.
 */
public class Insert10Demo {

    static void insertWithStatement(Connection c, List<BusPass> list) throws SQLException {
        try (Statement st = c.createStatement()) {
            for (BusPass p : list) {
                st.executeUpdate("INSERT INTO bus_pass (pass_id, reg_no, name, route, stop, validity, amount) VALUES ('"
                        + p.passId + "','" + p.regNo + "','" + p.name + "','" + p.route + "','" + p.stop + "','" + p.validity + "'," + p.amount.toPlainString() + ")");
            }
        }
    }

    static void insertWithPrepared(Connection c, List<BusPass> list) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(PassLoader.INSERT_SQL)) {
            for (BusPass p : list) {
                ps.setString(1, p.passId); ps.setString(2, p.regNo); ps.setString(3, p.name); ps.setString(4, p.route);
                ps.setString(5, p.stop); ps.setDate(6, java.sql.Date.valueOf(p.validity)); ps.setBigDecimal(7, p.amount);
                ps.executeUpdate();
            }
        }
    }

    static void print(Connection c, String title) throws SQLException {
        System.out.println("--- " + title + ": SELECT * FROM bus_pass ORDER BY pass_id ---");
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bus_pass ORDER BY LENGTH(pass_id), pass_id")) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder head = new StringBuilder();
            for (int i = 1; i <= cols; i++) head.append(String.format("%-12s", md.getColumnLabel(i)));
            System.out.println(head);
            int rows = 0;
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) row.append(String.format("%-12s", rs.getString(i)));
                System.out.println(row);
                rows++;
            }
            System.out.println("rows = " + rows);
        }
    }

    static void metadata(Connection c) throws SQLException {
        System.out.println("--- ResultSetMetaData of SELECT * FROM bus_pass ---");
        System.out.printf("%-4s %-10s %-12s %-14s %-10s %-8s %-10s%n", "#", "column", "SQL type", "Java class", "precision", "scale", "nullable");
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bus_pass")) {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                System.out.printf("%-4d %-10s %-12s %-14s %-10d %-8d %-10s%n", i, md.getColumnName(i), md.getColumnTypeName(i),
                        md.getColumnClassName(i).replace("java.lang.", "").replace("java.sql.", "").replace("java.math.", ""),
                        md.getPrecision(i), md.getScale(i), md.isNullable(i) == ResultSetMetaData.columnNoNulls ? "NOT NULL" : "NULL");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        List<BusPass> ten = PassData.generate(10);
        try (Connection c = PassData.open(false)) {
            PassData.truncate(c);
            insertWithStatement(c, ten);
            System.out.println("1. Statement            -> rows in table: " + PassData.count(c));
            print(c, "after Statement");
            PassData.truncate(c);
            insertWithPrepared(c, ten);
            System.out.println("2. PreparedStatement    -> rows in table: " + PassData.count(c));
            PassData.truncate(c);
            int n = PassLoader.load(c, ten);
            System.out.println("3. PreparedStatement batch (PassLoader) -> rows reported: " + n + ", rows in table: " + PassData.count(c));
            print(c, "after batch (the three methods gave identical contents)");
            metadata(c);
        }
    }
}
