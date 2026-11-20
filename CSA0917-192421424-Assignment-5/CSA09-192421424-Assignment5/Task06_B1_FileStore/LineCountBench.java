import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

/**
 * Task A2 / B1 - counts the lines of an attendance log with six techniques and measures time, number of read
 * calls that reach the FileInputStream (each one is a call into the operating system) and memory.
 *   java LineCountBench 1 10 50        (file sizes in MB; default 1 10 50)
 * Unbuffered reading of 50 MB makes 52 million read calls and can take minutes.
 */
public class LineCountBench {

    /** FileInputStream that counts how many read calls reach it (each one is a system call). */
    static final class CountingFileInputStream extends FileInputStream {
        long calls = 0;
        CountingFileInputStream(File f) throws FileNotFoundException { super(f); }
        @Override public int read() throws IOException { calls++; return super.read(); }
        @Override public int read(byte[] b) throws IOException { calls++; return super.read(b); }
        @Override public int read(byte[] b, int off, int len) throws IOException { calls++; return super.read(b, off, len); }
    }

    static long usedKb() {
        for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(40); } catch (InterruptedException e) { } }
        Runtime r = Runtime.getRuntime();
        return (r.totalMemory() - r.freeMemory()) / 1024;
    }

    static Path makeLog(long targetBytes) throws IOException {
        Path p = Path.of("attendance_" + (targetBytes / (1024 * 1024)) + "MB.log");
        if (Files.exists(p) && Files.size(p) >= targetBytes) return p;
        String[] status = {"PRESENT", "ABSENT", "LATE"};
        try (BufferedWriter w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            long written = 0; int i = 0;
            while (written < targetBytes) {
                String line = "2026-11-" + String.format("%02d", 1 + i % 28) + "," + (192421000 + i % 9000) + "," + status[i % 3] + ",CSE" + (100 + i % 50) + "\n";
                w.write(line); written += line.length(); i++;
            }
        }
        return p;
    }

    interface Counter { long[] run(Path p) throws IOException; }          // returns {lines, osReadCalls}

    static long[] unbuffered(Path p) throws IOException {
        long lines = 0;
        try (CountingFileInputStream in = new CountingFileInputStream(p.toFile())) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
            return new long[]{lines, in.calls};
        }
    }

    static long[] bufferedStreamPerByte(Path p) throws IOException {
        long lines = 0;
        CountingFileInputStream raw = new CountingFileInputStream(p.toFile());
        try (BufferedInputStream in = new BufferedInputStream(raw, 8192)) {
            int b;
            while ((b = in.read()) != -1) if (b == '\n') lines++;
        }
        return new long[]{lines, raw.calls};
    }

    static long[] byteArray(Path p, int size) throws IOException {
        long lines = 0;
        try (CountingFileInputStream in = new CountingFileInputStream(p.toFile())) {
            byte[] buf = new byte[size];
            int n;
            while ((n = in.read(buf)) != -1) for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
            return new long[]{lines, in.calls};
        }
    }

    static long[] readLine(Path p) throws IOException {
        long lines = 0;
        CountingFileInputStream raw = new CountingFileInputStream(p.toFile());
        try (BufferedReader r = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
            while (r.readLine() != null) lines++;
        }
        return new long[]{lines, raw.calls};
    }

    static double median(double[] a) { double[] c = a.clone(); java.util.Arrays.sort(c); return c[c.length / 2]; }

    static void measure(String label, Path p, Counter c, int runs) throws IOException {
        double[] t = new double[runs];
        long[] res = null;
        for (int i = 0; i < runs; i++) {
            long t0 = System.nanoTime();
            res = c.run(p);
            t[i] = (System.nanoTime() - t0) / 1e6;
        }
        System.out.printf("   %-44s lines=%-9d OS read calls=%-11d time=%10.1f ms%s%n", label, res[0], res[1], median(t), runs > 1 ? "  (median of " + runs + ")" : "  (single run)");
    }

    public static void main(String[] args) throws Exception {
        int[] sizes = args.length > 0 ? java.util.Arrays.stream(args).mapToInt(Integer::parseInt).toArray() : new int[]{1, 10, 50};
        System.out.println("Java " + System.getProperty("java.version") + "   (each OS read call = one call into the operating system)");
        for (int mb : sizes) {
            Path p = makeLog((long) mb * 1024 * 1024);
            long size = Files.size(p);
            System.out.println();
            System.out.printf("=== %d MB attendance log: %,d bytes ===%n", mb, size);
            System.out.printf("   Prediction: unbuffered = %,d read calls; 8 KB buffer = %,d; 64 KB buffer = %,d%n", size, (size + 8191) / 8192, (size + 65535) / 65536);
            int slowRuns = mb >= 10 ? 1 : 3;
            measure("1. FileInputStream.read() one byte at a time", p, LineCountBench::unbuffered, slowRuns);
            measure("2. BufferedInputStream(8 KB).read() per byte", p, LineCountBench::bufferedStreamPerByte, 3);
            measure("3. read(byte[8192]) manual buffer", p, q -> byteArray(q, 8192), 3);
            measure("4. read(byte[65536]) manual buffer", p, q -> byteArray(q, 65536), 3);
            measure("5. BufferedReader.readLine()", p, LineCountBench::readLine, 3);

            long before = usedKb();
            long t0 = System.nanoTime();
            List<String> all = Files.readAllLines(p, StandardCharsets.UTF_8);
            double ms = (System.nanoTime() - t0) / 1e6;
            long heapKb = usedKb() - before;
            System.out.printf("   %-44s lines=%-9d heap held=%,d KB (%.1f x file size)   time=%10.1f ms%n", "6. Files.readAllLines() (whole file in memory)", all.size(), heapKb, heapKb * 1024.0 / size, ms);
            all = null;
            long before2 = usedKb();
            long[] r = readLine(p);
            long after2 = usedKb() - before2;
            System.out.printf("   %-44s heap held after counting with readLine: about %,d KB (buffers only)%n", "   (for comparison)", Math.max(after2, 0));
        }
    }
}
