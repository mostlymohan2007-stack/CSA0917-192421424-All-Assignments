import java.sql.*;

/**
 * Task B2 - the UNSAFE login (user input joined into the SQL text) against the SAFE login (PreparedStatement),
 * run with the same inputs on the local test database transport_test.
 *   java -cp mysql-connector-j-8.4.0.jar;. LoginDemo jdbc:mysql://127.0.0.1:3307/transport_test root ""
 */
public class LoginDemo {

    /** UNSAFE: the supplied-style login query joins user input directly into an SQL string. */
    static String unsafeLogin(Connection c, String user, String pass) {
        String sql = "SELECT role FROM app_user WHERE username = '" + user + "' AND password_hash = SHA2('" + pass + "', 256)";
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? "ACCEPTED as " + rs.getString(1) : "rejected";
        } catch (SQLException e) {
            return "SQL error (" + e.getClass().getSimpleName() + "): " + firstLine(e.getMessage());
        }
    }

    /** SAFE: the SQL text is fixed; user input travels only as parameter VALUES. */
    static String safeLogin(Connection c, String user, String pass) {
        String sql = "SELECT role FROM app_user WHERE username = ? AND password_hash = SHA2(?, 256)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, pass);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? "ACCEPTED as " + rs.getString(1) : "rejected";
            }
        } catch (SQLException e) {
            return "SQL error (" + e.getClass().getSimpleName() + "): " + firstLine(e.getMessage());
        }
    }

    static String firstLine(String s) { return s == null ? "" : s.split("\n")[0]; }

    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection(args[0], args[1], args[2])) {
            String[][] tests = {
                // label, username, password, what a correct system must do
                {"L1 valid user and password",            "admin",                       "Admin@123", "accept"},
                {"L2 valid user, WRONG password",         "admin",                       "wrong",     "reject"},
                {"L3 unknown user",                       "hacker",                      "wrong",     "reject"},
                {"L4 INJECTION: comment out the password check", "admin' -- ",           "anything",  "reject"},
                {"L5 INJECTION: always-true condition",   "' OR 1=1 -- ",                "anything",  "reject"},
                {"L6 INJECTION: UNION supplies a fake row", "x' UNION SELECT 'ADMIN' -- ", "anything", "reject"},
                {"L7 INJECTION: stacked statement",       "x'; DROP TABLE app_user; -- ", "anything", "reject"},
                {"L8 name with an apostrophe (legitimate)", "o'brien",                   "pw",        "reject (no such user) without crashing"},
            };
            System.out.println("Unsafe query template : SELECT role FROM app_user WHERE username = '<user>' AND password_hash = SHA2('<pass>', 256)");
            System.out.println("Safe query template   : SELECT role FROM app_user WHERE username = ? AND password_hash = SHA2(?, 256)");
            System.out.println();
            System.out.printf("%-48s | %-42s | %-26s | %s%n", "Test", "UNSAFE version", "SAFE (PreparedStatement)", "correct behaviour");
            System.out.println("-".repeat(150));
            for (String[] t : tests) {
                System.out.printf("%-48s | %-42s | %-26s | %s%n", t[0], unsafeLogin(c, t[1], t[2]), safeLogin(c, t[1], t[2]), t[3]);
            }
            System.out.println();
            System.out.println("Text actually sent to MySQL by the UNSAFE version for L4:");
            System.out.println("   SELECT role FROM app_user WHERE username = 'admin' -- ' AND password_hash = SHA2('anything', 256)");
            System.out.println("   (everything after -- is a comment, so the password is never checked)");
            System.out.println("Text sent for L6:");
            System.out.println("   SELECT role FROM app_user WHERE username = 'x' UNION SELECT 'ADMIN' -- ' AND password_hash = ...");
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM app_user")) {
                rs.next();
                System.out.println();
                System.out.println("app_user still has " + rs.getInt(1) + " rows after all tests (the stacked DROP TABLE in L7 did not run: the driver rejects multiple statements by default; this protection is not a defence against L4-L6).");
            }
        }
    }
}
