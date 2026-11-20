import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reader that counts how many instances are currently open (to measure leaks). */
class TrackedReader extends BufferedReader {
    static final AtomicInteger open = new AtomicInteger();
    private boolean closed = false;
    TrackedReader(String path) throws IOException { super(new FileReader(path)); open.incrementAndGet(); }
    @Override public void close() throws IOException {
        if (!closed) { closed = true; open.decrementAndGet(); }
        super.close();
    }
}

/** Writer that counts how many instances are currently open. */
class TrackedWriter extends BufferedWriter {
    static final AtomicInteger open = new AtomicInteger();
    private boolean closed = false;
    TrackedWriter(String path) throws IOException { super(new FileWriter(path)); open.incrementAndGet(); }
    @Override public void close() throws IOException {
        if (!closed) { closed = true; open.decrementAndGet(); }
        super.close();
    }
}

/** A resource whose close() itself fails - used to demonstrate suppressed exceptions. */
class FlakyResource implements AutoCloseable {
    private final String name;
    FlakyResource(String name) { this.name = name; System.out.println("   open  " + name); }
    @Override public void close() throws Exception {
        System.out.println("   close " + name);
        throw new IOException("close() failed for " + name);
    }
}

/** Q3 - Daily sales report: manual close, manual close in finally, try-with-resources. */
public class SalesReport {

    static final Logger LOG = Logger.getLogger("SalesReport");

    /** Parses "item,qty,price"; logs WHICH file and line caused a failure. */
    static long parseLine(String storeFile, int lineNo, String line) {
        try {
            String[] p = line.split(",");
            return Long.parseLong(p[1].trim()) * Long.parseLong(p[2].trim());
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "FAILURE in file=" + storeFile + " line=" + lineNo + " text='" + line + "' -> " + e);
            throw new IllegalStateException("Bad data in " + storeFile + " line " + lineNo, e);
        }
    }

    static long totalOf(TrackedReader r, String storeFile) throws IOException {
        long total = 0;
        String line;
        int n = 0;
        while ((line = r.readLine()) != null) total += parseLine(storeFile, ++n, line);
        return total;
    }

    // ---------- Version 1: manual close() - NOT in finally (the buggy, leaking version) ----------
    static void leaky(List<String> stores, String out) throws IOException {
        TrackedWriter w = new TrackedWriter(out);
        long grand = 0;
        for (String s : stores) {
            TrackedReader r = new TrackedReader(s);
            long t = totalOf(r, s);              // if this throws, r and w are never closed
            w.write(s + " = " + t + "\n");
            grand += t;
            r.close();
        }
        w.write("GRAND TOTAL = " + grand + "\n");
        w.close();
    }

    // ---------- Version 2: try / catch / finally with manual close() (correct but verbose) ----------
    static void manualFinally(List<String> stores, String out) throws IOException {
        TrackedWriter w = null;
        try {
            w = new TrackedWriter(out);
            long grand = 0;
            for (String s : stores) {
                TrackedReader r = null;
                try {
                    r = new TrackedReader(s);
                    long t = totalOf(r, s);
                    w.write(s + " = " + t + "\n");
                    grand += t;
                } finally {
                    if (r != null) {
                        try { r.close(); } catch (IOException ignore) { /* must not hide the original exception */ }
                    }
                }
            }
            w.write("GRAND TOTAL = " + grand + "\n");
        } finally {
            if (w != null) {
                try { w.close(); } catch (IOException ignore) { }
            }
        }
    }

    // ---------- Version 3: try-with-resources ----------
    static void twr(List<String> stores, String out) throws IOException {
        try (TrackedWriter w = new TrackedWriter(out)) {
            long grand = 0;
            for (String s : stores) {
                try (TrackedReader r = new TrackedReader(s)) {
                    long t = totalOf(r, s);
                    w.write(s + " = " + t + "\n");
                    grand += t;
                }
            }
            w.write("GRAND TOTAL = " + grand + "\n");
        }
    }

    interface Version { void run(List<String> stores, String out) throws IOException; }

    static void writeInputs() throws IOException {
        Files.createDirectories(Path.of("input"));
        Files.writeString(Path.of("input/store1.csv"), "pen,10,5\nbook,3,120\n");
        Files.writeString(Path.of("input/store2.csv"), "bag,2,800\nbottle,5,60\n");
        Files.writeString(Path.of("input/store3.csv"), "ruler,4,10\nbad row,xx,yy\nglue,1,25\n");   // line 2 is corrupt
        Files.writeString(Path.of("input/store4.csv"), "cap,6,150\n");
        Files.writeString(Path.of("input/store3_fixed.csv"), "ruler,4,10\nglue,1,25\n");
    }

    static void runCase(String label, Version v, List<String> stores, String out) {
        TrackedReader.open.set(0);
        TrackedWriter.open.set(0);
        String result = "completed normally";
        try {
            v.run(stores, out);
        } catch (Exception e) {
            result = "FAILED with " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        long size = 0;
        try { size = Files.size(Path.of(out)); } catch (IOException ignore) { }
        System.out.printf("%-26s -> %s%n", label, result);
        System.out.printf("%-26s    open readers=%d, open writers=%d, output file size=%d bytes%n",
                "", TrackedReader.open.get(), TrackedWriter.open.get(), size);
    }

    public static void main(String[] args) throws Exception {
        LOG.setUseParentHandlers(true);
        writeInputs();
        List<String> good = List.of("input/store1.csv", "input/store2.csv", "input/store3_fixed.csv", "input/store4.csv");
        List<String> bad = List.of("input/store1.csv", "input/store2.csv", "input/store3.csv", "input/store4.csv");

        System.out.println("=== 1. Same report from all three versions (good input) ===");
        String[] outs = {"out_leaky.txt", "out_manual.txt", "out_twr.txt"};
        leaky(good, outs[0]); manualFinally(good, outs[1]); twr(good, outs[2]);
        String a = Files.readString(Path.of(outs[0])), b = Files.readString(Path.of(outs[1])), c = Files.readString(Path.of(outs[2]));
        System.out.print(c);
        System.out.println("leaky == manual : " + a.equals(b) + "   manual == twr : " + b.equals(c));

        System.out.println();
        System.out.println("=== 2. Same FAILING input (store3.csv line 2 is corrupt) ===");
        runCase("Version 1 manual (leaky)", SalesReport::leaky, bad, "out_leaky_fail.txt");
        runCase("Version 2 manual finally", SalesReport::manualFinally, bad, "out_manual_fail.txt");
        runCase("Version 3 try-with-res.", SalesReport::twr, bad, "out_twr_fail.txt");

        System.out.println();
        System.out.println("=== 3. Order of closing and suppressed exceptions ===");
        try (FlakyResource first = new FlakyResource("R1-first-opened");
             FlakyResource second = new FlakyResource("R2-second-opened")) {
            System.out.println("   body runs and throws");
            throw new IllegalStateException("body failure");
        } catch (Exception e) {
            System.out.println("   primary exception : " + e);
            for (Throwable s : e.getSuppressed()) System.out.println("   suppressed        : " + s);
        }
    }
}
