import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

// ---------- exception hierarchy for the processor ----------
class ScoreFileException extends Exception {
    final String file;
    ScoreFileException(String file, String message, Throwable cause) { super(file + ": " + message, cause); this.file = file; }
}
class MissingScoreFileException extends ScoreFileException {
    MissingScoreFileException(String file, Throwable cause) { super(file, "file is missing", cause); }
}
class CorruptScoreFileException extends ScoreFileException {
    CorruptScoreFileException(String file, String why, Throwable cause) { super(file, "corrupt (" + why + ")", cause); }
}

/**
 * Q7 - Concurrent, fault-tolerant processor for 10,000 score files.
 * Design: ExecutorService + Callable<Integer> + Future; shared results in ConcurrentHashMap; failures travel through
 * the Future (ExecutionException) so ONE bad file never stops the run; every submitted task is accounted for.
 *
 * File format (one line):  registerNumber,mark1,mark2,mark3,mark4,mark5   (each mark 0..100)
 *
 * Usage:  java ResultProcessor selftest            -> small hand-checked set
 *         java ResultProcessor bench <dataDir>     -> generate 10,000 files and measure pool sizes 1,2,4,8 (+16,32)
 *         java ResultProcessor alt <dataDir>       -> compare ExecutorService, parallelStream and one thread per file
 */
public class ResultProcessor {

    /** Shared state: registerNumber -> total marks (only successful files). */
    final Map<String, Integer> results = new ConcurrentHashMap<>();
    /** Shared state: log of failed files with reason. */
    final ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();
    final LongAdder ok = new LongAdder(), missing = new LongAdder(), corrupt = new LongAdder(), unexpected = new LongAdder();
    final long simulatedLatencyMicros;

    ResultProcessor(long simulatedLatencyMicros) { this.simulatedLatencyMicros = simulatedLatencyMicros; }

    /** One unit of work: read -> validate -> compute. Checked exceptions describe expected failures. */
    Callable<Integer> task(Path file) {
        return () -> process(file);
    }

    int process(Path file) throws ScoreFileException {
        String text;
        try {
            if (simulatedLatencyMicros > 0) {
                try { TimeUnit.MICROSECONDS.sleep(simulatedLatencyMicros); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            text = Files.readString(file);
        } catch (NoSuchFileException e) {
            throw new MissingScoreFileException(file.getFileName().toString(), e);
        } catch (IOException e) {
            throw new CorruptScoreFileException(file.getFileName().toString(), "unreadable", e);
        }
        String[] p = text.trim().split(",");
        if (p.length != 6 || p[0].isBlank()) throw new CorruptScoreFileException(file.getFileName().toString(), "expected 6 fields", null);
        int total = 0;
        try {
            for (int i = 1; i <= 5; i++) {
                int m = Integer.parseInt(p[i].trim());
                if (m < 0 || m > 100) throw new CorruptScoreFileException(file.getFileName().toString(), "mark out of range", null);
                total += m;
            }
        } catch (NumberFormatException e) {
            throw new CorruptScoreFileException(file.getFileName().toString(), "non-numeric mark", e);
        }
        results.put(p[0].trim(), total);          // thread-safe shared write
        return total;
    }

    /** Runs all files with the given pool size and returns elapsed milliseconds. Summary counters are filled in. */
    double run(List<Path> files, int poolSize) throws InterruptedException {
        long t0 = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        List<Future<Integer>> futures = new ArrayList<>(files.size());
        List<Path> submitted = new ArrayList<>(files);
        for (Path f : files) futures.add(pool.submit(task(f)));
        pool.shutdown();
        for (int i = 0; i < futures.size(); i++) {
            try {
                futures.get(i).get();
                ok.increment();
            } catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof MissingScoreFileException) missing.increment();
                else if (c instanceof CorruptScoreFileException) corrupt.increment();
                else unexpected.increment();                      // anything we did not predict is still counted
                failures.add(submitted.get(i).getFileName() + " -> " + c);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                unexpected.increment();
                failures.add(submitted.get(i).getFileName() + " -> interrupted");
            }
        }
        return (System.nanoTime() - t0) / 1e6;
    }

    long total() { return ok.sum() + missing.sum() + corrupt.sum() + unexpected.sum(); }

    String summary() {
        return "OK=" + ok.sum() + ", MISSING=" + missing.sum() + ", CORRUPT=" + corrupt.sum() + ", UNEXPECTED=" + unexpected.sum()
                + ", processed=" + total() + ", distinct results stored=" + results.size();
    }

    // ---------- data generation ----------
    /** Creates n files; every 50th (i%50==7) is corrupt (4 kinds), every 100th (i%100==13) is missing. Returns expected counts {ok, missing, corrupt}. */
    static long[] generate(Path dir, int n, List<Path> files) throws IOException {
        Files.createDirectories(dir);
        long ok = 0, missing = 0, corrupt = 0;
        for (int i = 0; i < n; i++) {
            Path f = dir.resolve("score" + i + ".txt");
            files.add(f);
            if (i % 100 == 13) { missing++; continue; }                         // listed but never created
            String content;
            if (i % 50 == 7) {
                corrupt++;
                content = switch ((i / 50) % 4) {
                    case 0 -> "R" + i + ",80,7x,90,60,70";                      // non-numeric
                    case 1 -> "R" + i + ",80,70";                               // too few fields
                    case 2 -> "R" + i + ",80,170,90,60,70";                     // out of range
                    default -> "";                                              // empty file
                };
            } else {
                ok++;
                content = "R" + i + "," + (50 + i % 50) + "," + (40 + i % 60) + "," + (60 + i % 40) + "," + (30 + i % 70) + "," + (70 + i % 30);
            }
            Files.writeString(f, content);
        }
        return new long[]{ok, missing, corrupt};
    }

    static double median(double[] a) { double[] c = a.clone(); Arrays.sort(c); return c[c.length / 2]; }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "selftest";
        System.out.println("Java " + System.getProperty("java.version") + ", cores = " + Runtime.getRuntime().availableProcessors());
        switch (mode) {
            case "selftest" -> selftest();
            case "bench" -> bench(Path.of(args.length > 1 ? args[1] : "data"));
            case "alt" -> alt(Path.of(args.length > 1 ? args[1] : "data"));
            default -> System.out.println("unknown mode");
        }
    }

    /** Hand-checked set of 12 files: 7 good, 2 missing, 3 corrupt. */
    static void selftest() throws Exception {
        Path dir = Files.createTempDirectory("q7selftest");
        String[][] spec = {
            {"a1.txt", "R1,10,20,30,40,50"},   // OK  total 150
            {"a2.txt", "R2,100,100,100,100,100"},// OK total 500
            {"a3.txt", "R3,0,0,0,0,0"},          // OK total 0
            {"a4.txt", "R4,55,65,75,85,95"},     // OK total 375
            {"a5.txt", "R5,1,2,3,4,5"},          // OK total 15
            {"a6.txt", "R6,90,80,70,60,50"},     // OK total 350
            {"a7.txt", "R7,12,34,56,78,90"},     // OK total 270
            {"c1.txt", "R8,10,abc,30,40,50"},    // CORRUPT non-numeric
            {"c2.txt", "R9,10,20"},              // CORRUPT too few fields
            {"c3.txt", "R10,10,20,300,40,50"},   // CORRUPT out of range
            {"m1.txt", null},                    // MISSING
            {"m2.txt", null},                    // MISSING
        };
        List<Path> files = new ArrayList<>();
        for (String[] s : spec) {
            Path f = dir.resolve(s[0]);
            files.add(f);
            if (s[1] != null) Files.writeString(f, s[1]);
        }
        ResultProcessor rp = new ResultProcessor(0);
        rp.run(files, 4);
        System.out.println("Hand-checked expectation : OK=7, MISSING=2, CORRUPT=3, UNEXPECTED=0, processed=12");
        System.out.println("Program summary          : " + rp.summary());
        boolean pass = rp.ok.sum() == 7 && rp.missing.sum() == 2 && rp.corrupt.sum() == 3 && rp.unexpected.sum() == 0 && rp.total() == 12
                && rp.results.get("R1") == 150 && rp.results.get("R2") == 500 && rp.results.get("R3") == 0 && rp.results.get("R4") == 375
                && rp.results.get("R5") == 15 && rp.results.get("R6") == 350 && rp.results.get("R7") == 270;
        System.out.println("Totals R1=" + rp.results.get("R1") + " R2=" + rp.results.get("R2") + " R3=" + rp.results.get("R3")
                + " R4=" + rp.results.get("R4") + " R5=" + rp.results.get("R5") + " R6=" + rp.results.get("R6") + " R7=" + rp.results.get("R7"));
        System.out.println("Failure log:");
        rp.failures.stream().sorted().forEach(s -> System.out.println("   " + s));
        System.out.println("SELF-TEST " + (pass ? "PASSED" : "FAILED"));
    }

    static void bench(Path dir) throws Exception {
        List<Path> files = new ArrayList<>();
        long[] exp = generate(dir, 10_000, files);
        System.out.println("Generated 10,000 files. Expected by the generator: OK=" + exp[0] + ", MISSING=" + exp[1] + ", CORRUPT=" + exp[2]);

        int[] pools = {1, 2, 4, 8, 16, 32};
        System.out.println("\n--- A. Real file reading (no artificial delay), median of 5 runs after 2 warm-up runs ---");
        for (int p : pools) {
            for (int w = 0; w < 2; w++) new ResultProcessor(0).run(files, p);
            double[] t = new double[5];
            ResultProcessor last = null;
            for (int r = 0; r < 5; r++) { last = new ResultProcessor(0); t[r] = last.run(files, p); }
            boolean correct = last.ok.sum() == exp[0] && last.missing.sum() == exp[1] && last.corrupt.sum() == exp[2] && last.total() == 10_000;
            System.out.printf("pool=%2d  median=%8.1f ms  min=%8.1f ms  summary correct=%s  [%s]%n", p, median(t), Arrays.stream(t).min().getAsDouble(), correct, last.summary());
        }

        System.out.println("\n--- B. SIMULATED 1 ms I/O latency per file (Thread.sleep) - models a slow disk / network share, median of 3 runs ---");
        for (int p : new int[]{1, 2, 4, 8, 16, 32}) {
            double[] t = new double[3];
            ResultProcessor last = null;
            for (int r = 0; r < 3; r++) { last = new ResultProcessor(1000); t[r] = last.run(files, p); }
            boolean correct = last.ok.sum() == exp[0] && last.missing.sum() == exp[1] && last.corrupt.sum() == exp[2] && last.total() == 10_000;
            System.out.printf("pool=%2d  median=%9.1f ms  min=%9.1f ms  summary correct=%s%n", p, median(t), Arrays.stream(t).min().getAsDouble(), correct);
        }
    }

    interface Approach { ResultProcessor run(List<Path> files, long latency) throws Exception; }

    static ResultProcessor viaExecutor(List<Path> files, long latency) throws Exception {
        ResultProcessor a = new ResultProcessor(latency);
        a.run(files, 8);
        return a;
    }

    static ResultProcessor viaParallelStream(List<Path> files, long latency) {
        // common ForkJoinPool (cores-1 workers + calling thread); a failure must be caught per element
        ResultProcessor b = new ResultProcessor(latency);
        files.parallelStream().forEach(f -> {
            try { b.process(f); b.ok.increment(); }
            catch (MissingScoreFileException e) { b.missing.increment(); }
            catch (CorruptScoreFileException e) { b.corrupt.increment(); }
            catch (Exception e) { b.unexpected.increment(); }
        });
        return b;
    }

    static ResultProcessor viaThreadPerFile(List<Path> files, long latency) throws Exception {
        ResultProcessor c = new ResultProcessor(latency);
        List<Thread> threads = new ArrayList<>();
        for (Path f : files) {
            Thread th = new Thread(() -> {
                try { c.process(f); c.ok.increment(); }
                catch (MissingScoreFileException e) { c.missing.increment(); }
                catch (CorruptScoreFileException e) { c.corrupt.increment(); }
                catch (Exception e) { c.unexpected.increment(); }
            });
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) th.join();
        return c;
    }

    static void alt(Path dir) throws Exception {
        List<Path> files = new ArrayList<>();
        long[] exp = generate(dir, 10_000, files);
        System.out.println("Expected: OK=" + exp[0] + ", MISSING=" + exp[1] + ", CORRUPT=" + exp[2]);
        String[] names = {"ExecutorService pool=8", "parallelStream", "one thread per file (10000)"};
        Approach[] approaches = {ResultProcessor::viaExecutor, ResultProcessor::viaParallelStream, ResultProcessor::viaThreadPerFile};

        for (Approach ap : approaches) { ap.run(files, 0); ap.run(files, 0); }     // warm-up: JIT + OS file cache

        for (long latency : new long[]{0, 1000}) {
            System.out.println("\n=== simulated latency per file = " + latency + " microseconds (median of 3 runs) ===");
            for (int k = 0; k < approaches.length; k++) {
                double[] t = new double[3];
                ResultProcessor last = null;
                for (int r = 0; r < 3; r++) {
                    long t0 = System.nanoTime();
                    last = approaches[k].run(files, latency);
                    t[r] = (System.nanoTime() - t0) / 1e6;
                }
                System.out.printf("%-28s: %9.1f ms  %s%n", names[k], median(t), last.summary());
            }
        }
    }
}
