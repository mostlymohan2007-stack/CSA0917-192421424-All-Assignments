import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Task B1 - test run of the file store: normal case, non-existent id, damaged line, missing file,
 * incomplete write (truncated last line) and the supplied example (expected: P1003 record, then null for P1002).
 */
public class PassStoreDemo {
    static void show(String label, Object value) { System.out.println(String.format("%-48s -> %s", label, value)); }

    public static void main(String[] args) throws Exception {
        Path file = Path.of("bus_passes_example.txt");
        // The supplied example file: P1002 is DAMAGED (only 5 fields), P1003 is valid.
        Files.writeString(file,
                "P1001|192421401|Ravi Kumar|R12|Main Gate|2026-12-31|1500.00\n"
              + "P1002|192421402|Meena Iyer|R12|Library\n"
              + "P1003|192421403|Anita Rao|R15|North Stop|2026-12-31|1800.00\n", StandardCharsets.UTF_8);
        PassStore store = new PassStore(file);

        System.out.println("=== Supplied example (expected: the P1003 record, then null for the damaged P1002 line) ===");
        System.out.println(store.find("P1003"));
        System.out.println(store.find("P1002"));
        System.out.println("   damaged lines seen by that scan: " + store.lastDamaged());

        System.out.println();
        System.out.println("=== B1 test table ===");
        show("T1 normal: find P1001", store.find("P1001"));
        show("T2 id that does not exist: find P9999", store.find("P9999"));
        show("T3 damaged line: find P1002", store.find("P1002") + "   " + store.lastDamaged());

        store.add(BusPass.create("P1004", "192421404", "Kiran Das", "R12", "Bus Stand", LocalDate.of(2026, 12, 31), "1500.00"), true);
        show("T4 add P1004 then find it", store.find("P1004"));
        List<BusPass> r12 = store.listByRoute("R12");
        show("T5 list route R12 (expect P1001, P1004)", r12.size() + " records: " + r12.stream().map(p -> p.passId).toList());

        try { BusPass.create("P1005", "192421405", "Bad|Name", "R12", "Gate", LocalDate.of(2026, 12, 31), "1500"); }
        catch (IllegalArgumentException e) { show("T6 add with the delimiter inside a field", "rejected: " + e.getMessage()); }

        // Incomplete write after a power failure: the last line is cut in the middle and has no newline.
        Files.writeString(file, Files.readString(file) + "P1006|192421406|Sun", StandardCharsets.UTF_8);
        show("T7 truncated last line: find P1006", store.find("P1006") + "   " + store.lastDamaged());
        show("T8 earlier records still readable: find P1003", store.find("P1003") != null ? "found" : "NOT FOUND");

        // Missing file
        PassStore missing = new PassStore(Path.of("no_such_file.txt"));
        try { missing.find("P1001"); }
        catch (NoSuchFileException e) { show("T9 missing file", "handled: store file not found (" + e.getMessage() + ") - exists()=" + missing.exists()); }

        Files.deleteIfExists(file);
    }
}
