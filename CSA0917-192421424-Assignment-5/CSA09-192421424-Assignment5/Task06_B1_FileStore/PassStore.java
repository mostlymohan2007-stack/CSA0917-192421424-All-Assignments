import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Task B1 - bus-pass file store (delimited text file).
 * Operations: add (append), find by pass id (scan), list by route (scan), loadAll (for the HashMap alternative).
 * Damaged lines are NEVER fatal: they are skipped and counted/recorded.
 */
public class PassStore {
    private final Path file;
    /** Damaged lines seen by the most recent scan: "line N: reason". */
    private final List<String> damaged = new ArrayList<>();

    public PassStore(Path file) { this.file = file; }

    public boolean exists() { return Files.exists(file); }
    public List<String> lastDamaged() { return damaged; }

    /** Pseudocode "AddPass": validate, append ONE complete line (ending with a newline), flush, optionally force it to disk. */
    public void add(BusPass p, boolean forceToDisk) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file.toFile(), true)) {          // append mode: never rewrites old data
            out.write((p.toLine() + "\n").getBytes(StandardCharsets.UTF_8));             // one write call for the whole line
            out.flush();
            if (forceToDisk) out.getFD().sync();                                          // survive a power failure
        }
    }

    /**
     * Pseudocode "FindPass": scan the file line by line; skip damaged lines; return the matching record or null.
     * Complete the Code: TODO 1 is completed below.
     */
    public BusPass find(String passId) throws IOException {
        damaged.clear();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                // TODO 1 (completed): read line by line, skip damaged lines and return the matching record
                try {
                    BusPass p = BusPass.parse(line);
                    if (p.passId.equals(passId)) return p;
                } catch (DamagedRecordException e) {
                    damaged.add("line " + lineNo + ": " + e.getMessage());                // skip it, remember it, keep searching
                }
            }
        }
        return null;
    }

    /** Pseudocode "ListByRoute": scan the whole file and collect the records of one route. */
    public List<BusPass> listByRoute(String route) throws IOException {
        damaged.clear();
        List<BusPass> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                try {
                    BusPass p = BusPass.parse(line);
                    if (p.route.equals(route)) result.add(p);
                } catch (DamagedRecordException e) {
                    damaged.add("line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return result;
    }

    /** Loads every valid record into a HashMap keyed by pass id (alternative design: O(n) once, then O(1) per find). */
    public Map<String, BusPass> loadAll() throws IOException {
        damaged.clear();
        Map<String, BusPass> map = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                try {
                    BusPass p = BusPass.parse(line);
                    map.put(p.passId, p);
                } catch (DamagedRecordException e) {
                    damaged.add("line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return map;
    }
}
