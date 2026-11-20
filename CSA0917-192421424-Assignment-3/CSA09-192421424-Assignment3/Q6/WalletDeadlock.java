import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Q6 Part A - hostel wallet transfers. Modes:
 *   deadlock : transfer(A->B) and transfer(B->A) lock the two accounts in opposite orders -> deadlock
 *   ordered  : both transfers lock the account with the SMALLER id first -> no deadlock
 *   trylock  : ReentrantLock.tryLock(timeout) with back-off and retry (alternative concept)
 */
public class WalletDeadlock {

    static class Account {
        final int id;
        int balance;
        final ReentrantLock lock = new ReentrantLock();
        Account(int id, int balance) { this.id = id; this.balance = balance; }
    }

    // ---- deadlock-prone: locks "from" then "to" ----
    static void transferUnsafe(Account from, Account to, int amount) throws InterruptedException {
        synchronized (from) {
            Thread.sleep(100);                       // makes the interleaving reliable
            synchronized (to) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    // ---- deadlock-free: consistent global lock ordering by account id ----
    static void transferOrdered(Account from, Account to, int amount) throws InterruptedException {
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        synchronized (first) {
            Thread.sleep(100);
            synchronized (second) {
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }

    // ---- alternative: tryLock with timeout and retry ----
    static int retries = 0;
    static void transferTryLock(Account from, Account to, int amount) throws InterruptedException {
        while (true) {
            if (from.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                try {
                    Thread.sleep(100);
                    if (to.lock.tryLock(50, TimeUnit.MILLISECONDS)) {
                        try {
                            from.balance -= amount;
                            to.balance += amount;
                            return;
                        } finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }
            }
            synchronized (WalletDeadlock.class) { retries++; }
            Thread.sleep((long) (Math.random() * 40));          // random back-off so the two threads do not collide forever
        }
    }

    interface Transfer { void run(Account f, Account t, int amt) throws InterruptedException; }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "deadlock";
        Transfer tr = switch (mode) {
            case "ordered" -> WalletDeadlock::transferOrdered;
            case "trylock" -> WalletDeadlock::transferTryLock;
            default -> WalletDeadlock::transferUnsafe;
        };
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> { try { tr.run(a, b, 100); } catch (InterruptedException e) { } }, "Transfer-A-to-B");
        Thread t2 = new Thread(() -> { try { tr.run(b, a, 50); } catch (InterruptedException e) { } }, "Transfer-B-to-A");
        t1.setDaemon(true); t2.setDaemon(true);
        t1.start(); t2.start();
        t1.join(2000); t2.join(2000);

        if (t1.isAlive() || t2.isAlive()) {
            System.out.println("mode=" + mode + ": still running after 2 s -> DEADLOCK suspected");
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            long[] ids = mx.findDeadlockedThreads();
            if (ids != null) {
                for (ThreadInfo ti : mx.getThreadInfo(ids)) {
                    System.out.println("  " + ti.getThreadName() + " is " + ti.getThreadState() + " waiting for lock " + ti.getLockName()
                            + " which is held by " + ti.getLockOwnerName());
                }
            }
            System.out.println("Balances unchanged: A=" + a.balance + ", B=" + b.balance);
        } else {
            System.out.println("mode=" + mode + ": both transfers finished. A=" + a.balance + ", B=" + b.balance
                    + " (expected A=950, B=1050, total=2000)" + (mode.equals("trylock") ? ", retries=" + retries : ""));
        }
    }
}
