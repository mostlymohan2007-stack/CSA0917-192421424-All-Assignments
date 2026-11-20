import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Way 1: extend Thread. */
class StationThread extends Thread {
    final int scripts;
    volatile int scanned = 0;
    final List<String> finishOrder;
    StationThread(String name, int scripts, List<String> finishOrder) {
        super(name);
        this.scripts = scripts;
        this.finishOrder = finishOrder;
    }
    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(5 + ThreadLocalRandom.current().nextInt(6)); } catch (InterruptedException e) { return; }
            scanned++;
        }
        finishOrder.add(getName());
    }
}

/** Way 2: implement Runnable (the task is separate from the thread). */
class StationTask implements Runnable {
    final String name;
    final int scripts;
    volatile int scanned = 0;
    final List<String> finishOrder;
    StationTask(String name, int scripts, List<String> finishOrder) {
        this.name = name;
        this.scripts = scripts;
        this.finishOrder = finishOrder;
    }
    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(5 + ThreadLocalRandom.current().nextInt(6)); } catch (InterruptedException e) { return; }
            scanned++;
        }
        finishOrder.add(name);
    }
}

/**
 * Q4 - Parallel answer-script scanning. Modes (first argument):
 *   compare   : Thread version and Runnable version, start/join/isAlive, total printed after join (default)
 *   order     : one line - finishing order of the four stations (run it 10+ times)
 *   priority  : same as order but with station priorities 10,1,1,1 (sleep-based work)
 *   prioritycpu : CPU-bound stations with priorities 10,1,1,1
 *   lifecycle : prints the Thread.State values a station passes through
 *   runvsstart: calls run() directly and start(), printing the executing thread name
 *   daemon    : station D is a daemon thread; main ends without join
 *   executor  : ExecutorService with a fixed pool (alternative concept)
 */
public class ScannerStations {
    static final int[] BATCH = {8, 6, 10, 7};
    static final String[] NAMES = {"Station-A", "Station-B", "Station-C", "Station-D"};

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "compare";
        switch (mode) {
            case "compare" -> compare();
            case "order" -> order(false, false);
            case "priority" -> order(true, false);
            case "prioritycpu" -> order(true, true);
            case "lifecycle" -> lifecycle();
            case "runvsstart" -> runVsStart();
            case "daemon" -> daemon();
            case "executor" -> executor();
            default -> System.out.println("unknown mode " + mode);
        }
    }

    static void compare() throws Exception {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        StationThread[] ts = new StationThread[4];
        for (int i = 0; i < 4; i++) ts[i] = new StationThread(NAMES[i], BATCH[i], order);
        for (StationThread t : ts) t.start();
        System.out.println("Thread version : isAlive right after start = " + ts[0].isAlive());
        for (StationThread t : ts) t.join();
        int total = 0;
        for (StationThread t : ts) { total += t.scanned; System.out.println("  " + t.getName() + " scanned " + t.scanned + ", isAlive after join = " + t.isAlive()); }
        System.out.println("Thread version : finish order = " + order + "  TOTAL = " + total + " (expected 31)");

        order.clear();
        StationTask[] tasks = new StationTask[4];
        Thread[] threads = new Thread[4];
        for (int i = 0; i < 4; i++) { tasks[i] = new StationTask(NAMES[i], BATCH[i], order); threads[i] = new Thread(tasks[i]); }
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        total = 0;
        for (StationTask t : tasks) total += t.scanned;
        System.out.println("Runnable version: finish order = " + order + "  TOTAL = " + total + " (expected 31)");
    }

    static void order(boolean usePriority, boolean cpu) throws Exception {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        int[] prio = {10, 1, 1, 1};
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) {
            final String name = NAMES[i];
            final int batch = BATCH[i];
            Runnable work;
            if (cpu) {
                work = () -> { long x = 0; for (long k = 0; k < 300_000_000L; k++) x += k ^ (x >>> 3); if (x == 42) System.out.print(""); order.add(name); };
            } else {
                work = new StationTask(name, batch, order);
            }
            ts[i] = new Thread(work, name);
            if (usePriority) ts[i].setPriority(prio[i]);
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        System.out.println(order);
    }

    static void lifecycle() throws Exception {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        StationThread t = new StationThread("Station-A", 8, order);
        System.out.println("after new Thread()      : " + t.getState());
        t.start();
        System.out.println("right after start()     : " + t.getState());
        Thread.sleep(20);
        System.out.println("20 ms later (sleeping)  : " + t.getState());
        t.join();
        System.out.println("after join()            : " + t.getState() + ", isAlive=" + t.isAlive());
        try { t.start(); } catch (IllegalThreadStateException e) { System.out.println("start() again          : IllegalThreadStateException (a dead thread cannot be restarted)"); }
    }

    static void runVsStart() throws Exception {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        Runnable show = () -> System.out.println("   body executed by thread: " + Thread.currentThread().getName());
        Thread t1 = new Thread(show, "Station-A");
        System.out.println("t1.run()   :");
        t1.run();                       // ordinary method call on the main thread
        Thread t2 = new Thread(show, "Station-A");
        System.out.println("t2.start() :");
        t2.start();
        t2.join();
        StationThread viaRun = new StationThread("Station-RUN", 3, order);
        long t0 = System.nanoTime();
        viaRun.run();
        System.out.println("run() blocked main for " + (System.nanoTime() - t0) / 1_000_000 + " ms (no parallelism)");
    }

    static void daemon() throws Exception {
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        StationThread normal = new StationThread("Station-A", 4, order);
        StationThread daemonT = new StationThread("Station-D", 100, order);   // would need ~750 ms
        daemonT.setDaemon(true);
        normal.start();
        daemonT.start();
        normal.join();
        System.out.println("main ends now; daemon Station-D still running = " + daemonT.isAlive() + ", scanned so far = " + daemonT.scanned);
        System.out.println("finish order recorded = " + order + "  (Station-D never finishes because the JVM exits)");
    }

    static void executor() throws Exception {
        for (int pool : new int[]{4, 2}) {
            ExecutorService ex = Executors.newFixedThreadPool(pool);
            List<Future<Integer>> fs = new ArrayList<>();
            long t0 = System.nanoTime();
            for (int i = 0; i < 4; i++) {
                final int batch = BATCH[i];
                final String name = NAMES[i];
                Callable<Integer> c = () -> {
                    for (int k = 0; k < batch; k++) Thread.sleep(5 + ThreadLocalRandom.current().nextInt(6));
                    return batch;
                };
                fs.add(ex.submit(c));
            }
            int total = 0;
            for (Future<Integer> f : fs) total += f.get();
            ex.shutdown();
            System.out.println("Executor pool=" + pool + " : TOTAL = " + total + " (expected 31), elapsed = " + (System.nanoTime() - t0) / 1_000_000 + " ms");
        }
    }
}
