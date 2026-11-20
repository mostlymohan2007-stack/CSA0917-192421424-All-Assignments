import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business logic of the fee payment: validates input and processes the payment.
 * No Swing/AWT classes are used here, so it can be tested without a window.
 */
public class FeeService {
    public static final List<String> METHODS = List.of("Cash", "Card", "UPI");
    private static final Pattern ID = Pattern.compile("\\d{6,10}");
    private static final BigDecimal MAX_ONE_PAYMENT = new BigDecimal("500000");

    private final PaymentRepository repository;

    public FeeService(PaymentRepository repository) { this.repository = repository; }

    /** Pure validation (no database): returns the parsed amount or throws ValidationException with a clear message. */
    public BigDecimal validate(String studentId, String amountText, String method) throws ValidationException {
        if (studentId == null || !ID.matcher(studentId.trim()).matches())
            throw new ValidationException("Student ID must be 6 to 10 digits.");
        if (amountText == null || amountText.isBlank())
            throw new ValidationException("Enter the amount to pay.");
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Amount must be a number, for example 2500 or 2500.50.");
        }
        if (amount.signum() <= 0) throw new ValidationException("Amount must be greater than zero.");
        if (amount.scale() > 2) throw new ValidationException("Amount can have at most 2 decimal places.");
        if (amount.compareTo(MAX_ONE_PAYMENT) > 0) throw new ValidationException("A single payment cannot exceed 500000.");
        if (method == null || !METHODS.contains(method)) throw new ValidationException("Choose Cash, Card or UPI.");
        return amount;
    }

    /** Validates and processes one payment. Called on a worker thread by the window. */
    public Receipt pay(String studentId, String amountText, String method) throws Exception {
        BigDecimal amount = validate(studentId, amountText, method);
        String id = studentId.trim();
        Optional<BigDecimal> due = repository.findOutstanding(id);
        if (due.isEmpty()) throw new ValidationException("Student " + id + " was not found.");
        if (due.get().signum() == 0) throw new ValidationException("Student " + id + " has no outstanding fee.");
        if (amount.compareTo(due.get()) > 0)
            throw new ValidationException("Amount " + amount.toPlainString() + " is more than the outstanding fee " + due.get().toPlainString() + ".");
        int number = repository.savePayment(id, amount, method);
        return new Receipt(number, id, amount, method, due.get().subtract(amount), LocalDateTime.now());
    }
}
