import java.math.BigDecimal;
import java.util.Optional;

/** Data-access contract. FeeService depends on this interface, never on JDBC or Swing. */
public interface PaymentRepository {
    /** Outstanding fee of a student, or empty if the student does not exist. */
    Optional<BigDecimal> findOutstanding(String studentId) throws Exception;

    /** Stores the payment and reduces the outstanding fee; returns the new receipt number. */
    int savePayment(String studentId, BigDecimal amount, String method) throws Exception;
}
