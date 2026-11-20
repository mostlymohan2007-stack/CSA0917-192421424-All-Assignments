import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Immutable result of a successful payment. */
public final class Receipt {
    private final int number;
    private final String studentId;
    private final BigDecimal amount;
    private final String method;
    private final BigDecimal balanceAfter;
    private final LocalDateTime time;

    public Receipt(int number, String studentId, BigDecimal amount, String method, BigDecimal balanceAfter, LocalDateTime time) {
        this.number = number; this.studentId = studentId; this.amount = amount;
        this.method = method; this.balanceAfter = balanceAfter; this.time = time;
    }
    public int getNumber() { return number; }
    public String getStudentId() { return studentId; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getTime() { return time; }

    public String toText() {
        return "FEE PAYMENT RECEIPT\n-------------------\n"
                + "Receipt no : " + number + "\n"
                + "Student ID : " + studentId + "\n"
                + "Amount     : " + amount.toPlainString() + "\n"
                + "Method     : " + method + "\n"
                + "Balance    : " + balanceAfter.toPlainString() + "\n"
                + "Date/Time  : " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n";
    }
}
