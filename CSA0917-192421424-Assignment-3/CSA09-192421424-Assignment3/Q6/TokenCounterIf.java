/**
 * Q6 experiment: "Change while to if in both methods, run it several times, and explain any difference."
 * With if, a thread that is woken (by notifyAll or a spurious wake-up) does NOT re-check the condition, so a
 * second consumer can take from an EMPTY counter (stale/duplicate token, count < 0) and a producer can write
 * into a FULL counter (overwriting a token, count > 5).
 * To avoid an endless hang the main thread gives up after 5 seconds and reports it.
 */
public class TokenCounterIf {

    private final int[] buffer = new int[5];
    private int count = 0, in = 0, out = 0;
    private int minCount = 0, maxCount = 0;

    public synchronized void put(int token) throws InterruptedException {
        if (count == buffer.length) wait();          // if instead of while
        buffer[in] = token;
        in = (in + 1) % buffer.length;
        count++;
        maxCount = Math.max(maxCount, count);
        notifyAll();
    }

    public synchronized int take() throws InterruptedException {
        if (count == 0) wait();                      // if instead of while
        int token = buffer[out];
        out = (out + 1) % buffer.length;
        count--;
        minCount = Math.min(minCount, count);
        notifyAll();
        return token;
    }

    public static void main(String[] args) throws InterruptedException {
        TokenCounterIf counter = new TokenCounterIf();
        int[] collected = new int[4];
        Thread cook = new Thread(() -> {
            try { for (int t = 1; t <= 40; t++) counter.put(t); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread[] students = new Thread[4];                      // 4 consumers make the problem easy to see
        for (int s = 0; s < 4; s++) {
            final int id = s;
            students[s] = new Thread(() -> {
                try { for (int k = 0; k < 10; k++) collected[id] += counter.take(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            students[s].setDaemon(true);
        }
        cook.setDaemon(true);
        cook.start(); for (Thread t : students) t.start();
        long deadline = System.currentTimeMillis() + 5000;
        for (Thread t : students) t.join(Math.max(1, deadline - System.currentTimeMillis()));
        boolean hung = false;
        for (Thread t : students) hung |= t.isAlive();
        int sum = 0; for (int c : collected) sum += c;
        System.out.println("IF-variant: sum = " + sum + " (expected 820), min count seen = " + counter.minCount
                + ", max count seen = " + counter.maxCount + (hung ? "  ** HUNG (threads still waiting after 5 s) **" : ""));
    }
}
