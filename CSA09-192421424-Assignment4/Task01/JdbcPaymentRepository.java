import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

/**
 * JDBC implementation: PreparedStatement (no SQL injection), try-with-resources (everything is closed),
 * one transaction for "insert payment + reduce due", credentials supplied from outside.
 */
public class JdbcPaymentRepository implements PaymentRepository {
    private final String url, user, password;

    public JdbcPaymentRepository(String url, String user, String password) {
        this.url = url; this.user = user; this.password = password;
    }

    @Override public Optional<BigDecimal> findOutstanding(String studentId) throws SQLException {
        String sql = "SELECT outstanding FROM fee_due WHERE student_id = ?";
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty();
            }
        }
    }

    @Override public int savePayment(String studentId, BigDecimal amount, String method) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            c.setAutoCommit(false);
            try (PreparedStatement ins = c.prepareStatement(
                         "INSERT INTO fee_payment (student_id, amount, method) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement upd = c.prepareStatement(
                         "UPDATE fee_due SET outstanding = outstanding - ? WHERE student_id = ?")) {
                ins.setString(1, studentId); ins.setBigDecimal(2, amount); ins.setString(3, method);
                ins.executeUpdate();
                upd.setBigDecimal(1, amount); upd.setString(2, studentId);
                upd.executeUpdate();
                int receipt;
                try (ResultSet keys = ins.getGeneratedKeys()) { keys.next(); receipt = keys.getInt(1); }
                c.commit();
                return receipt;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
