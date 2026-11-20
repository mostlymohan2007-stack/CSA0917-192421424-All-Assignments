import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * Task B3 - transaction and failure. A pass with an INVALID amount (negative, rejected by CHECK (amount > 0)) is placed
 * halfway through the input list. The same input is loaded (a) with auto-commit and (b) inside one transaction.
 * Expected: (a) the rows before the bad row stay in the table (half-loaded data); (b) rollback leaves ZERO rows.
 */
public class FailureDemo {

    static BusPass withAmount(BusPass p, String amount) {
        return new BusPass(p.passId, p.regNo, p.name, p.route, p.stop, p.validity, new BigDecimal(amount));
    }

    /** Loads with a plain loop of PreparedStatement inserts, auto-commit ON. */
    static String autoCommitRun(Connection c, List<BusPass> list) throws SQLException {
        PassData.truncate(c);
        c.setAutoCommit(true);
        try { Insert10Demo.insertWithPrepared(c, list); return "completed"; }
        catch (SQLException e) { return "FAILED: " + e.getMessage(); }
    }

    /** Loads with PassLoader (batches of 1,000) inside ONE transaction; rolls back on any failure. */
    static String transactionRun(Connection c, List<BusPass> list) throws SQLException {
        PassData.truncate(c);
        c.setAutoCommit(false);
        try {
            PassLoader.load(c, list);
            c.commit();
            return "committed";
        } catch (SQLException e) {
            c.rollback();
            return "FAILED, rolled back: " + e.getMessage().split("\n")[0];
        } finally {
            c.setAutoCommit(true);
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection c = PassData.open(false)) {
            // ---- the 10-record case from the assignment: invalid amount at position 6 of 10 ----
            List<BusPass> ten = PassData.generate(10);
            ten.set(5, withAmount(ten.get(5), "-500.00"));
            System.out.println("=== 10 records, record #6 (P6) has amount -500.00 ===");
            String r1 = autoCommitRun(c, ten);
            System.out.println("auto-commit (row by row)  : " + r1);
            System.out.println("   rows left in the table : " + PassData.count(c) + "   <-- half-loaded data: P1..P5 stay, P7..P10 were never inserted");
            String r2 = transactionRun(c, ten);
            System.out.println("single transaction        : " + r2);
            System.out.println("   rows left in the table : " + PassData.count(c) + "   <-- all or nothing");

            // ---- bigger case with batches of 1,000: failure inside the second batch ----
            List<BusPass> big = PassData.generate(2000);
            big.set(1500, withAmount(big.get(1500), "-1.00"));
            System.out.println();
            System.out.println("=== 2,000 records (2 batches of 1,000), record #1501 has amount -1.00 ===");
            PassData.truncate(c);
            c.setAutoCommit(true);
            try { PassLoader.load(c, big); System.out.println("batch, auto-commit        : completed"); }
            catch (SQLException e) { System.out.println("batch, auto-commit        : FAILED: " + e.getMessage().split("\n")[0]); }
            System.out.println("   rows left in the table : " + PassData.count(c) + "   <-- partial data remains");
            String r3 = transactionRun(c, big);
            System.out.println("batch, single transaction : " + r3);
            System.out.println("   rows left in the table : " + PassData.count(c));

            // ---- control: the same list without the bad row loads completely ----
            big.set(1500, withAmount(big.get(1500), "1501.00"));
            System.out.println("control (all amounts valid) : " + transactionRun(c, big) + ", rows in table = " + PassData.count(c));
            PassData.truncate(c);
        }
    }
}
