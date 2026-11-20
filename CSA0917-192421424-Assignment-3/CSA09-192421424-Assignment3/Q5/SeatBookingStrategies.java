import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Q5 - Compares booking strategies: unsafe, synchronized method, synchronized block, AtomicBoolean[]
 * with compareAndSet, ReentrantLock, ConcurrentHashMap.putIfAbsent.
 *
 * Part 1 (correctness): 100 threads, 10 seats, repeated ROUNDS times; counts rounds with a double booking
 *                      and the total number of double bookings (a seat sold more than once).
 * Part 2 (speed under contention): 8 threads each make 200,000 booking attempts on 1000 seats.
 */
public class SeatBookingStrategies {

    interface Booker { boolean book(int seat); }

    /** Part 1 models a 1 ms database round-trip between check and act (makes the race visible); Part 2 turns it off. */
    static volatile boolean slowGap = true;
    static void gap() {
        if (slowGap) { try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    }

    static class Unsafe implements Booker {
        final boolean[] b;
        Unsafe(int n) { b = new boolean[n]; }
        public boolean book(int s) { if (!b[s]) { gap(); b[s] = true; return true; } return false; }
    }
    static class SyncMethod implements Booker {
        final boolean[] b;
        SyncMethod(int n) { b = new boolean[n]; }
        public synchronized boolean book(int s) { if (!b[s]) { gap(); b[s] = true; return true; } return false; }
    }
    static class SyncBlock implements Booker {
        final boolean[] b;
        SyncBlock(int n) { b = new boolean[n]; }
        public boolean book(int s) {
            String ticket = "T-" + s;                  // non-critical work stays OUTSIDE the lock
            synchronized (this) {                      // only the check-then-act is locked
                if (!b[s]) { gap(); b[s] = true; return ticket != null; }
                return false;
            }
        }
    }
    static class Atomic implements Booker {
        final AtomicBoolean[] b;
        Atomic(int n) { b = new AtomicBoolean[n]; for (int i = 0; i < n; i++) b[i] = new AtomicBoolean(); }
        public boolean book(int s) { return b[s].compareAndSet(false, true); }
    }
    static class Locked implements Booker {
        final boolean[] b;
        final ReentrantLock lock = new ReentrantLock();
        Locked(int n) { b = new boolean[n]; }
        public boolean book(int s) {
            lock.lock();
            try { if (!b[s]) { gap(); b[s] = true; return true; } return false; }
            finally { lock.unlock(); }
        }
    }
    static class Chm implements Booker {
        final ConcurrentHashMap<Integer, Boolean> m = new ConcurrentHashMap<>();
        Chm(int n) { }
        public boolean book(int s) { return m.putIfAbsent(s, Boolean.TRUE) == null; }
    }

    interface Factory { Booker make(int seats); }

    static final String[] NAMES = {"Unsafe (no lock)", "synchronized method", "synchronized block", "AtomicBoolean[] CAS", "ReentrantLock", "ConcurrentHashMap.putIfAbsent"};
    static final Factory[] FACTORIES = {Unsafe::new, SyncMethod::new, SyncBlock::new, Atomic::new, Locked::new, Chm::new};

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version") + ", cores = " + Runtime.getRuntime().availableProcessors());
        System.out.println("\n=== Part 1: correctness (100 threads, 10 seats, 1000 rounds) ===");
        for (int k = 0; k < NAMES.length; k++) {
            int badRounds = 0;
            long doubleBookings = 0;
            for (int round = 0; round < 1000; round++) {
                final Booker booker = FACTORIES[k].make(10);
                final AtomicIntegerArray sold = new AtomicIntegerArray(10);
                Thread[] users = new Thread[100];
                final java.util.concurrent.CountDownLatch gate = new java.util.concurrent.CountDownLatch(1);
                for (int i = 0; i < users.length; i++) {
                    final int seat = i % 10;
                    users[i] = new Thread(() -> {
                        try { gate.await(); } catch (InterruptedException e) { return; }
                        if (booker.book(seat)) sold.incrementAndGet(seat);
                    });
                }
                for (Thread t : users) t.start();
                gate.countDown();                       // release all 100 users at the same instant
                for (Thread t : users) t.join();
                int extra = 0;
                for (int s = 0; s < 10; s++) extra += Math.max(0, sold.get(s) - 1);
                if (extra > 0) badRounds++;
                doubleBookings += extra;
            }
            System.out.printf("%-32s rounds with double booking = %4d / 1000, total double bookings = %d%n", NAMES[k], badRounds, doubleBookings);
        }

        slowGap = false;                               // Part 2 measures pure locking cost: no simulated database delay
        System.out.println("\n=== Part 2: speed under contention (8 threads x 200,000 attempts, 1000 seats; correct strategies only) ===");
        for (int k = 1; k < NAMES.length; k++) {
            long best = Long.MAX_VALUE;
            for (int rep = 0; rep < 5; rep++) {
                final Booker booker = FACTORIES[k].make(1000);
                Thread[] ts = new Thread[8];
                long t0 = System.nanoTime();
                for (int i = 0; i < ts.length; i++) {
                    final int id = i;
                    ts[i] = new Thread(() -> { for (int n = 0; n < 200_000; n++) booker.book((n * 7 + id) % 1000); });
                    ts[i].start();
                }
                for (Thread t : ts) t.join();
                best = Math.min(best, System.nanoTime() - t0);
            }
            System.out.printf("%-32s %8.1f ms total (best of 5), %6.1f ns per attempt%n", NAMES[k], best / 1e6, (double) best / (8 * 200_000));
        }
    }
}
