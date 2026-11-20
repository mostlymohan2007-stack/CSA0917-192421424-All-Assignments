import java.util.ArrayList;
import java.util.List;

/**
 * Q6 Student A: records the counter size after EVERY put/take to prove it never exceeds 5 or drops below 0.
 * Uses the correct while-loop design with 1 producer and 3 consumers.
 */
public class TokenCounterTrace {

    private final int[] buffer = new int[5];
    private int count = 0, in = 0, out = 0;
    private final List<Integer> sizes = new ArrayList<>();      // guarded by the same lock

    public synchronized void put(int token) throws InterruptedException {
        while (count == buffer.length) wait();
        buffer[in] = token; in = (in + 1) % buffer.length; count++;
        sizes.add(count);
        notifyAll();
    }

    public synchronized int take() throws InterruptedException {
        while (count == 0) wait();
        int token = buffer[out]; out = (out + 1) % buffer.length; count--;
        sizes.add(count);
        notifyAll();
        return token;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounterTrace c = new TokenCounterTrace();
        long[] sum = new long[3];
        Thread cook = new Thread(() -> {
            try { for (int t = 1; t <= 30; t++) { c.put(t); if (t % 7 == 0) Thread.sleep(3); } } catch (InterruptedException e) { }
        });
        Thread[] cons = new Thread[3];
        for (int s = 0; s < 3; s++) {
            final int id = s;
            cons[s] = new Thread(() -> { try { for (int k = 0; k < 10; k++) sum[id] += c.take(); } catch (InterruptedException e) { } });
        }
        cook.start(); for (Thread t : cons) t.start();
        cook.join(); for (Thread t : cons) t.join();
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int v : c.sizes) { min = Math.min(min, v); max = Math.max(max, v); }
        System.out.println("operations recorded = " + c.sizes.size());
        System.out.println("counter size after each operation: " + c.sizes);
        System.out.println("minimum size seen = " + min + ", maximum size seen = " + max + "  (must stay within 0..5)");
        System.out.println("sum collected = " + (sum[0] + sum[1] + sum[2]) + " (expected 465)");
    }
}
