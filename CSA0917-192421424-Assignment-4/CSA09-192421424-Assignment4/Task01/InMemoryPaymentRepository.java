import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Test/demo repository that keeps data in memory; the optional delay imitates a slow database. */
public class InMemoryPaymentRepository implements PaymentRepository {
    private final Map<String, BigDecimal> due = new HashMap<>();
    private final long delayMillis;
    private int nextReceipt = 1001;

    public InMemoryPaymentRepository(long delayMillis) {
        this.delayMillis = delayMillis;
        due.put("192421424", new BigDecimal("50000.00"));
        due.put("192421425", new BigDecimal("12500.50"));
        due.put("192421426", new BigDecimal("0.00"));
    }

    @Override public synchronized Optional<BigDecimal> findOutstanding(String studentId) throws Exception {
        pause();
        return Optional.ofNullable(due.get(studentId));
    }

    @Override public synchronized int savePayment(String studentId, BigDecimal amount, String method) throws Exception {
        pause();
        due.merge(studentId, amount.negate(), BigDecimal::add);
        return nextReceipt++;
    }

    private void pause() throws InterruptedException { if (delayMillis > 0) Thread.sleep(delayMillis); }
}
