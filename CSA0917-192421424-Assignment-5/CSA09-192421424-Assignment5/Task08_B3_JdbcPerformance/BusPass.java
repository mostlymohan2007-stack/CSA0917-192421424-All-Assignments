import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * One bus-pass record. Text layout (delimited, UTF-8, one record per line, '|' as delimiter, 7 fields):
 *   passId|registerNumber|name|route|boardingStop|validityDate|amountPaid
 *   P1003|192421424|Anita Rao|R12|Main Gate|2026-12-31|1500.00
 */
public final class BusPass implements java.io.Serializable {
    private static final long serialVersionUID = 1L;                 // version number of the serialized form (see Part A, task A4)
    public static final int FIELD_COUNT = 7;
    private static final Pattern ID = Pattern.compile("P\\d{1,9}");
    private static final Pattern REG = Pattern.compile("\\d{6,12}");
    private static final Pattern ROUTE = Pattern.compile("R\\d{1,3}");

    public final String passId, regNo, name, route, stop;
    public final LocalDate validity;
    public final BigDecimal amount;

    public BusPass(String passId, String regNo, String name, String route, String stop, LocalDate validity, BigDecimal amount) {
        this.passId = passId; this.regNo = regNo; this.name = name; this.route = route;
        this.stop = stop; this.validity = validity; this.amount = amount;
    }

    /** One line of the text file (without the line terminator). */
    public String toLine() {
        return passId + "|" + regNo + "|" + name + "|" + route + "|" + stop + "|" + validity + "|" + amount.toPlainString();
    }

    /** Strict parser: every problem becomes a DamagedRecordException that says WHY the line is rejected. */
    public static BusPass parse(String line) throws DamagedRecordException {
        if (line == null || line.isBlank()) throw new DamagedRecordException("empty line");
        String[] f = line.split("\\|", -1);                         // -1 keeps trailing empty fields
        if (f.length != FIELD_COUNT) throw new DamagedRecordException("expected " + FIELD_COUNT + " fields but found " + f.length);
        for (int i = 0; i < f.length; i++) f[i] = f[i].trim();
        if (!ID.matcher(f[0]).matches()) throw new DamagedRecordException("bad pass id '" + f[0] + "'");
        if (!REG.matcher(f[1]).matches()) throw new DamagedRecordException("bad register number '" + f[1] + "'");
        if (f[2].isEmpty()) throw new DamagedRecordException("empty name");
        if (!ROUTE.matcher(f[3]).matches()) throw new DamagedRecordException("bad route '" + f[3] + "'");
        if (f[4].isEmpty()) throw new DamagedRecordException("empty boarding stop");
        LocalDate date;
        try { date = LocalDate.parse(f[5]); } catch (DateTimeParseException e) { throw new DamagedRecordException("bad validity date '" + f[5] + "'"); }
        BigDecimal amount;
        try { amount = new BigDecimal(f[6]); } catch (NumberFormatException e) { throw new DamagedRecordException("amount is not a number '" + f[6] + "'"); }
        if (amount.signum() <= 0) throw new DamagedRecordException("amount must be positive but is " + f[6]);
        return new BusPass(f[0], f[1], f[2], f[3], f[4], date, amount);
    }

    /** Validation for a new record created by the program (the delimiter must not appear inside a field). */
    public static BusPass create(String passId, String regNo, String name, String route, String stop, LocalDate validity, String amount) {
        BusPass p;
        try {
            p = parse(passId + "|" + regNo + "|" + name + "|" + route + "|" + stop + "|" + validity + "|" + amount);
        } catch (DamagedRecordException e) {
            throw new IllegalArgumentException("Invalid pass: " + e.getMessage());
        }
        return p;
    }

    @Override public String toString() {
        return "BusPass{" + passId + ", " + regNo + ", " + name + ", " + route + ", " + stop + ", " + validity + ", " + amount.toPlainString() + "}";
    }
}
