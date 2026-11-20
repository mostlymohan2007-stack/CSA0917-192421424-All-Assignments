/**
 * Q5 "Complete the Code": TODO 1 completed with a synchronized method.
 * Expected output:  Seats sold: 10
 */
public class SeatBooking {

    private final boolean[] booked;
    private int bookingsMade = 0;

    SeatBooking(int seats) { booked = new boolean[seats]; }

    // TODO 1 (completed): the check-then-book operation is atomic because the whole method is synchronized
    synchronized boolean book(int seat) {
        if (!booked[seat]) {          // check
            booked[seat] = true;      // act
            bookingsMade++;
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBooking screen = new SeatBooking(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10;               // 10 users compete for every seat
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}
