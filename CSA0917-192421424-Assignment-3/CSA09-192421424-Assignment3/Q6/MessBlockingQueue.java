import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** Q6 alternative concept: the token counter with ArrayBlockingQueue (no explicit wait/notify). */
public class MessBlockingQueue {
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Integer> counter = new ArrayBlockingQueue<>(5);
        int[] collected = new int[2];
        Thread cook = new Thread(() -> {
            try { for (int t = 1; t <= 20; t++) counter.put(t); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[2];
        for (int s = 0; s < 2; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try { for (int k = 0; k < 10; k++) collected[id] += counter.take(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }
        long t0 = System.nanoTime();
        cook.start(); for (Thread t : students) t.start();
        cook.join(); for (Thread t : students) t.join();
        System.out.println("ArrayBlockingQueue: Sum of tokens collected: " + (collected[0] + collected[1]) + " (expected 210), time "
                + (System.nanoTime() - t0) / 1000 + " microseconds");
    }
}
