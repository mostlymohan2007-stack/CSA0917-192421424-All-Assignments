import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;

/**
 * Task B4 - creates the existing pass file for the migration test, with KNOWN damage so the result can be checked:
 *   every 997th line is damaged (rotating: wrong field count, non-numeric amount, negative amount, bad date),
 *   every 5003rd record is followed by an empty line, and every 9973rd record repeats the pass id of the previous (valid) record.
 *   java MakeTestFile 100000 passes_100000.txt
 */
public class MakeTestFile {
    public static void main(String[] args) throws Exception {
        int n = Integer.parseInt(args[0]);
        Path file = Path.of(args[1]);
        int damaged = 0, blank = 0, duplicate = 0;
        java.util.Set<String> validIds = new java.util.HashSet<>();      // ids of records that are really valid and kept
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= n; i++) {
                String id = "P" + i;
                String ok = new BusPass(id, String.valueOf(192000000 + i), "Student " + i, "R" + (1 + i % 40), "Stop " + (1 + i % 25),
                        LocalDate.of(2026, 12, 31), new BigDecimal((1000 + i % 1000) + ".00")).toLine();
                String line;
                if (i % 997 == 0) {
                    damaged++;
                    switch ((i / 997) % 4) {
                        case 0 -> line = id + "|" + (192000000 + i) + "|Student " + i + "|R3|Stop 4";                         // 5 fields
                        case 1 -> line = ok.substring(0, ok.lastIndexOf('|') + 1) + "abc";                                  // amount not numeric
                        case 2 -> line = ok.substring(0, ok.lastIndexOf('|') + 1) + "-250.00";                              // negative amount
                        default -> line = ok.replace("2026-12-31", "2026-13-45");                                           // impossible date
                    }
                } else if (i % 9973 == 0) {
                    String dupId = "P" + (i - 1);                                   // repeats the pass id of the previous record
                    if (validIds.contains(dupId)) duplicate++;                      // only counted if that earlier record is itself valid
                    line = new BusPass(dupId, String.valueOf(192000000 + i), "Student " + i, "R5", "Stop 5", LocalDate.of(2026, 12, 31), new BigDecimal("1500.00")).toLine();
                } else {
                    validIds.add(id);
                    line = ok;
                }
                w.write(line); w.write('\n');
                if (i % 5003 == 0) { w.write('\n'); blank++; }
            }
        }
        System.out.println("Created " + file + ": " + n + " records + " + blank + " blank lines");
        System.out.println("Injected: damaged=" + damaged + ", duplicate pass ids=" + duplicate + ", blank lines=" + blank + "  -> expected valid records = " + validIds.size() + ", expected rejected lines = " + (damaged + duplicate + blank));
    }
}
