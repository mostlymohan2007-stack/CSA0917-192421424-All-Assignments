/**
 * Q5 - the same program WITHOUT synchronized (the version the assignment asks you to run 10 times first).
 * Two things make the bug show up reliably: a CountDownLatch releases all 100 threads at the same instant (real
 * users click at the same time), and a 1 ms pause between the check and the act models a database round-trip (it widens the race window).
 * The race exists even without them - they only make it visible.
 */
public class SeatBookingUnsafe {

    private final boolean[] booked;
    private int bookingsMade = 0;

    SeatBookingUnsafe(int seats) { booked = new boolean[seats]; }

    /** Models the ~1 ms a real app spends between 'is the seat free?' and 'save the booking' (database round-trip). */
    static void pause() {
        try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    boolean book(int seat) {           // NO synchronized
        if (!booked[seat]) {           // 1. check
            pause();                   //    <- another thread may run here and ALSO see "free"
            booked[seat] = true;       // 2. act
            bookingsMade++;            //    (also a non-atomic read-modify-write)
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBookingUnsafe screen = new SeatBookingUnsafe(10);
        Thread[] users = new Thread[100];
        java.util.concurrent.CountDownLatch gate = new java.util.concurrent.CountDownLatch(1);   // all users press "book" together
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;
            users[i] = new Thread(() -> { try { gate.await(); } catch (InterruptedException e) { return; } screen.book(seat); });
            users[i].start();
        }
        gate.countDown();
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade + "   (expected 10)");
    }
}
