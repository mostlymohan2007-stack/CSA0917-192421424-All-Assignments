import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Task B2 - damaged bus-pass lines fed to (1) the ORIGINAL naive parser (split + direct conversion) and
 * (2) the strict parser of the redesigned store. Shows what each adversarial input reveals about the original design.
 */
public class DamagedDataDemo {

    /** The "original design": trusts every line. */
    static BusPass naiveParse(String line) {
        String[] f = line.split("\\|");
        return new BusPass(f[0], f[1], f[2], f[3], f[4], LocalDate.parse(f[5]), new BigDecimal(f[6]));
    }

    static String tryNaive(String line) {
        try { return "ACCEPTED " + naiveParse(line); }
        catch (Exception e) { return "CRASH " + e.getClass().getSimpleName() + ": " + e.getMessage(); }
    }

    static String tryStrict(String line) {
        try { return "ACCEPTED " + BusPass.parse(line); }
        catch (DamagedRecordException e) { return "skipped, reason: " + e.getMessage(); }
    }

    public static void main(String[] args) {
        String[][] cases = {
            {"D0 valid line (control)",            "P2001|192421401|Ravi Kumar|R12|Main Gate|2026-12-31|1500.00",
                    "nothing: both parsers accept it"},
            {"D1 wrong field count (5 fields)",    "P2002|192421402|Meena Iyer|R12|Library",
                    "the original design assumes every line has 7 fields; one short line throws and would stop the whole program"},
            {"D2 too many fields (9 fields)",      "P2003|192421403|Anita Rao|R15|North Stop|2026-12-31|1800.00|EXTRA|MORE",
                    "the extra fields are silently ignored: wrong data is accepted WITHOUT any error (worse than a crash)"},
            {"D3 non-numeric amount",              "P2004|192421404|Kiran Das|R12|Bus Stand|2026-12-31|abc",
                    "no validation before conversion: NumberFormatException; a bad amount can also hide as a very large/negative number"},
            {"D4 empty line",                      "",
                    "split gives one empty field, so f[1] fails; blank lines are common (editors add them) and must be ignored safely"},
            {"D5 negative amount",                 "P2005|192421405|Sun Lee|R12|Gate|2026-12-31|-500.00",
                    "the original design accepts it: syntactically valid but semantically wrong (business rule missing)"},
            {"D6 impossible date",                 "P2006|192421406|Tej Rao|R12|Gate|2026-13-45|1500.00",
                    "date errors are only found when the date is parsed; the original design has no per-line recovery"},
            {"D7 delimiter inside a name",         "P2007|192421407|Rao|Anita|R12|Gate|2026-12-31|1500.00",
                    "the delimiter appears in data: the line looks like 8 fields; the original format has no escaping rule"},
            {"D8 truncated last line (power failure)", "P2008|192421408|Sun",
                    "an incomplete write looks like a short line; the store must treat it as damaged, not as a pass"},
        };
        System.out.println("=== Damaged data: ORIGINAL naive parser versus the strict parser of the redesigned store ===");
        for (String[] c : cases) {
            System.out.println(c[0] + "   input: \"" + c[1] + "\"");
            System.out.println("   original (naive) : " + tryNaive(c[1]));
            System.out.println("   redesigned       : " + tryStrict(c[1]));
            System.out.println("   what it reveals  : " + c[2]);
            System.out.println();
        }
    }
}
