import java.sql.*;
import java.util.List;

/**
 * Task B3 - "Complete the Code": PassLoader inserts passes with a PreparedStatement batch of 1,000 rows.
 * The assignment PDF does not print the supplied program, so this is a reconstruction with TODO 1 to TODO 3, all completed.
 * The caller controls the transaction (setAutoCommit / commit / rollback).
 */
public class PassLoader {
    static final int BATCH_SIZE = 1000;
    static final String INSERT_SQL =
            "INSERT INTO bus_pass (pass_id, reg_no, name, route, stop, validity, amount) VALUES (?, ?, ?, ?, ?, ?, ?)";

    /** Returns the number of rows inserted. */
    public static int load(Connection con, List<BusPass> passes) throws SQLException {
        int inserted = 0;
        try (PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            int count = 0;
            for (BusPass p : passes) {
                // TODO 1 (completed): set the seven parameters of the PreparedStatement from p
                ps.setString(1, p.passId);
                ps.setString(2, p.regNo);
                ps.setString(3, p.name);
                ps.setString(4, p.route);
                ps.setString(5, p.stop);
                ps.setDate(6, java.sql.Date.valueOf(p.validity));
                ps.setBigDecimal(7, p.amount);
                // TODO 2 (completed): add the row to the batch
                ps.addBatch();
                count++;
                if (count % BATCH_SIZE == 0) {
                    // TODO 3 (completed): execute the batch (1,000 rows) and add the number of rows inserted to 'inserted'
                    inserted += sum(ps.executeBatch());
                }
            }
            inserted += sum(ps.executeBatch());                 // the last, incomplete batch
        }
        return inserted;
    }

    /** With rewriteBatchedStatements the driver reports SUCCESS_NO_INFO (-2) for each row: count it as one row. */
    private static int sum(int[] results) {
        int n = 0;
        for (int r : results) n += (r == Statement.SUCCESS_NO_INFO) ? 1 : Math.max(r, 0);
        return n;
    }
}
