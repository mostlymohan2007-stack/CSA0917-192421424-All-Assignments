package campus.test;

import campus.dao.Db;
import campus.dao.JdbcRegistrationDao;
import campus.model.*;
import campus.service.RegistrationService;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task 6 - requirement tests against the real MySQL database (run schema.sql first; run BEFORE seed_100000.sql).
 * Demonstrates: normal, invalid, duplicate registrations, event-full rule, SQL-injection safety, concurrent duplicates.
 */
public class ServiceTest {
    static int pass = 0, fail = 0;

    interface Call { void run() throws Exception; }

    static void check(String id, String requirement, Call call, Class<?> expected, String expectedText) {
        String got;
        Class<?> gotClass = null;
        try { call.run(); got = "OK (registered)"; }
        catch (Exception e) { gotClass = e.getClass(); got = e.getClass().getSimpleName() + ": " + e.getMessage(); }
        boolean ok = (expected == null) ? gotClass == null : (gotClass == expected && got.contains(expectedText));
        if (ok) pass++; else fail++;
        System.out.printf("%-4s %-4s %-44s -> %s%n", ok ? "PASS" : "FAIL", id, requirement, got);
    }

    public static void main(String[] args) throws Exception {
        Db db = new Db();
        JdbcRegistrationDao dao = new JdbcRegistrationDao(db);
        RegistrationService svc = new RegistrationService(dao);
        System.out.println("Database: " + db.describe());
        List<EventInfo> events = svc.events();
        EventInfo hack = events.get(1), tech = events.get(0), small = events.get(10);
        System.out.println("Events loaded: " + events.size() + " (first: " + tech + ", last: " + small + " capacity " + small.getCapacity() + ")");
        System.out.println();

        System.out.println("--- Normal registrations ---");
        check("N1", "valid registration", () -> svc.register("S100001", "Anita Rao", "anita@college.edu", "CSE", hack), null, null);
        check("N2", "same student, DIFFERENT event is allowed", () -> svc.register("S100001", "Anita Rao", "anita@college.edu", "CSE", tech), null, null);
        check("N3", "different student, same event", () -> svc.register("S100002", "Kiran Das", "kiran@college.edu", "ECE", hack), null, null);
        check("N4", "name with apostrophe (O'Brien)", () -> svc.register("S100003", "Sean O'Brien", "sean@college.edu", "IT", hack), null, null);

        System.out.println("--- Invalid input (rejected with a clear message) ---");
        check("I1", "empty name", () -> svc.register("S100004", "", "a@b.com", "CSE", hack), ValidationException.class, "Name must be");
        check("I2", "bad e-mail", () -> svc.register("S100004", "Ravi Kumar", "ravi-at-college", "CSE", hack), ValidationException.class, "valid e-mail");
        check("I3", "student ID too short", () -> svc.register("S1", "Ravi Kumar", "ravi@college.edu", "CSE", hack), ValidationException.class, "Student ID must be");
        check("I4", "no department selected", () -> svc.register("S100004", "Ravi Kumar", "ravi@college.edu", null, hack), ValidationException.class, "Select a department");
        check("I5", "no event selected", () -> svc.register("S100004", "Ravi Kumar", "ravi@college.edu", "CSE", null), ValidationException.class, "Select an event");
        check("I6", "SQL injection in name", () -> svc.register("S100004", "x'); DROP TABLE student;--", "ravi@college.edu", "CSE", hack), ValidationException.class, "Name must be");
        check("I7", "SQL injection in student id", () -> svc.register("S1' OR '1'='1", "Ravi Kumar", "ravi@college.edu", "CSE", hack), ValidationException.class, "Student ID must be");

        System.out.println("--- Duplicate registration ---");
        check("D1", "same student + same event again", () -> svc.register("S100001", "Anita Rao", "anita@college.edu", "CSE", hack), DuplicateRegistrationException.class, "already registered");
        check("D2", "duplicate with lower-case id (normalised)", () -> svc.register("s100001", "Anita Rao", "anita@college.edu", "CSE", hack), DuplicateRegistrationException.class, "already registered");
        System.out.println("Registrations for Hackathon 2026 after D1/D2 (expected 3: S100001, S100002, S100003): " + dao.count(hack.getEventId()));

        System.out.println("--- Capacity rule (event 11 has 3 seats) ---");
        for (int i = 1; i <= 3; i++) { final int k = i; check("C" + i, "seat " + i + " of 3", () -> svc.register("C20000" + k, "Capacity Test " + (char) ('A' + k), "c" + k + "@college.edu", "CSE", small), null, null); }
        check("C4", "4th student, event full", () -> svc.register("C200004", "Capacity Test D", "c4@college.edu", "CSE", small), EventFullException.class, "event is full");

        System.out.println("--- Concurrent duplicate attempts (10 threads, same student and event) ---");
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger okCount = new AtomicInteger(), dupCount = new AtomicInteger(), other = new AtomicInteger();
        List<Future<?>> fs = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            fs.add(pool.submit(() -> {
                try { start.await(); svc.register("S100009", "Race Tester", "race@college.edu", "CSE", tech); okCount.incrementAndGet(); }
                catch (DuplicateRegistrationException e) { dupCount.incrementAndGet(); }
                catch (Exception e) { other.incrementAndGet(); System.out.println("   unexpected: " + e); }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : fs) f.get();
        pool.shutdown();
        boolean raceOk = okCount.get() == 1 && dupCount.get() == 9 && other.get() == 0;
        if (raceOk) pass++; else fail++;
        System.out.printf("%-4s %-4s %-44s -> successes=%d, duplicates rejected=%d, other errors=%d%n", raceOk ? "PASS" : "FAIL", "R1", "exactly one of 10 simultaneous attempts wins", okCount.get(), dupCount.get(), other.get());

        System.out.println();
        System.out.println("Result: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
