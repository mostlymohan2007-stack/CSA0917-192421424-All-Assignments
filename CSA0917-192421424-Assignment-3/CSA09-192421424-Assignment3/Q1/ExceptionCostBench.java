/**
 * Q1/Q2 - Measures the time cost of creating + throwing + catching an exception versus returning an
 * error code, and how the cost grows with stack depth and with a cause chain.
 * Results depend on machine / JDK / JIT: re-run on your own computer and quote YOUR numbers.
 */
public class ExceptionCostBench {

    static class Plain extends Exception {
        Plain(String m) { super(m); }
    }
    static class NoTrace extends Exception {
        NoTrace(String m) { super(m, null, false, false); }   // writableStackTrace = false
    }

    static int sink = 0;

    static int errorCode(int i) { return (i % 2 == 0) ? -1 : 0; }

    static void throwAtDepth(int depth, int i) throws Plain {
        if (depth == 0) throw new Plain("fail " + i);
        throwAtDepth(depth - 1, i);
    }

    static void throwChained(int depth, int i) throws Plain {
        try {
            throwAtDepth(depth, i);
        } catch (Plain inner) {
            Plain outer = new Plain("wrapper " + i);
            outer.initCause(inner);
            throw outer;
        }
    }

    interface Job { void run(int i) throws Exception; }

    static double timeNsPerOp(Job job, int n) {
        for (int w = 0; w < 3; w++) {                           // warm-up rounds for the JIT
            for (int i = 0; i < n / 5; i++) { try { job.run(i); } catch (Exception e) { sink++; } }
        }
        long best = Long.MAX_VALUE;
        for (int r = 0; r < 5; r++) {                           // keep the best of 5 rounds
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) { try { job.run(i); } catch (Exception e) { sink++; } }
            best = Math.min(best, System.nanoTime() - t0);
        }
        return (double) best / n;
    }

    public static void main(String[] args) {
        int n = 200_000;
        System.out.println("Java " + System.getProperty("java.version") + ", " + n + " operations per round, best of 5 rounds");
        System.out.printf("Return an error code               : %8.1f ns/op%n", timeNsPerOp(i -> sink += errorCode(i), n));
        System.out.printf("Throw+catch, stack depth 1         : %8.1f ns/op%n", timeNsPerOp(i -> throwAtDepth(1, i), n));
        System.out.printf("Throw+catch, stack depth 10        : %8.1f ns/op%n", timeNsPerOp(i -> throwAtDepth(10, i), n));
        System.out.printf("Throw+catch, stack depth 50        : %8.1f ns/op%n", timeNsPerOp(i -> throwAtDepth(50, i), n));
        System.out.printf("Throw+catch, stack depth 200       : %8.1f ns/op%n", timeNsPerOp(i -> throwAtDepth(200, i), n));
        System.out.printf("Throw+catch, NO stack trace (depth 10): %5.1f ns/op%n", timeNsPerOp(i -> { throw new NoTrace("x"); }, n));
        System.out.printf("Chained (wrapper + cause), depth 10: %8.1f ns/op%n", timeNsPerOp(i -> throwChained(10, i), n));
        System.out.println("(sink=" + sink + ")");
    }
}
