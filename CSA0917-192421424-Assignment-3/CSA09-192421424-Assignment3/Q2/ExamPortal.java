import java.io.IOException;
import java.util.Set;

// ---------- Exception hierarchy ----------
// Throwable -> Exception -> ExamException (checked, base of every portal rule violation)
//                              |- ExamWindowClosedException   (checked)
//                              |- NotRegisteredException      (checked)
//                              |- SubmissionTooLargeException (checked)
//                              |- SubmissionStorageException  (checked, carries the IOException as cause)
// Throwable -> Exception -> RuntimeException -> InvalidSubmissionException (unchecked: programming/usage bug)

class ExamException extends Exception {
    ExamException(String message) { super(message); }
    ExamException(String message, Throwable cause) { super(message, cause); }
}

class ExamWindowClosedException extends ExamException {
    ExamWindowClosedException(String message) { super(message); }
}

class NotRegisteredException extends ExamException {
    NotRegisteredException(String message) { super(message); }
}

class SubmissionTooLargeException extends ExamException {
    SubmissionTooLargeException(String message) { super(message); }
}

class SubmissionStorageException extends ExamException {
    SubmissionStorageException(String message, Throwable cause) { super(message, cause); }
}

class InvalidSubmissionException extends RuntimeException {
    InvalidSubmissionException(String message) { super(message); }
}

// ---------- Storage layer ----------
class SubmissionStore {
    private long remainingQuotaKb;
    SubmissionStore(long quotaKb) { this.remainingQuotaKb = quotaKb; }

    /** Low-level I/O: throws IOException (checked, technical). */
    private void write(String fileName, long sizeKb) throws IOException {
        if (sizeKb > remainingQuotaKb) throw new IOException("Disk quota exceeded");
        remainingQuotaKb -= sizeKb;
    }

    /** Storage layer converts the technical IOException into a meaningful exception AND keeps the cause. */
    void save(String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            write(fileName, sizeKb);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName, e);   // chaining
        }
    }

    /** BAD version used only for the experiment: re-throws WITHOUT the cause (information is lost). */
    void saveLossy(String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            write(fileName, sizeKb);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName, null);  // cause dropped
        }
    }
}

// ---------- Service layer ----------
class SubmissionService {
    static final int MAX_SIZE_KB = 200;
    private final Set<String> registered = Set.of("S101:CSA0917", "S102:CSA0917", "S103:CSA0917");
    private final SubmissionStore store;
    private final boolean lossy;

    SubmissionService(SubmissionStore store, boolean lossy) { this.store = store; this.lossy = lossy; }

    void submit(String studentId, String course, String fileName, long sizeKb, int minutesAfterStart)
            throws ExamException {
        if (fileName == null || fileName.isBlank())
            throw new InvalidSubmissionException("File name is empty");                      // unchecked
        if (minutesAfterStart > 90)
            throw new ExamWindowClosedException("Exam window closed " + (minutesAfterStart - 90) + " min ago");
        if (!registered.contains(studentId + ":" + course))
            throw new NotRegisteredException(studentId + " is not registered for " + course);
        if (sizeKb > MAX_SIZE_KB)
            throw new SubmissionTooLargeException(fileName + " is " + sizeKb + " KB, limit " + MAX_SIZE_KB + " KB");
        if (lossy) store.saveLossy(fileName, sizeKb); else store.save(fileName, sizeKb);   // propagates upward
    }
}

// ---------- Controller layer: the ONLY place where exceptions are finally handled ----------
class SubmissionController {
    private final SubmissionService service;
    SubmissionController(SubmissionService service) { this.service = service; }

    String handle(String studentId, String course, String fileName, long sizeKb, int minutes, boolean showTrace) {
        try {
            service.submit(studentId, course, fileName, sizeKb, minutes);
            return "OK: " + fileName + " submitted";
        } catch (ExamWindowClosedException | NotRegisteredException | SubmissionTooLargeException e) {
            return "REJECTED: " + e.getMessage();                       // business-rule problem: tell the student why
        } catch (SubmissionStorageException e) {
            if (showTrace) {
                System.out.println("---- full chained stack trace ----");
                e.printStackTrace(System.out);
                System.out.println("---- end of stack trace ----");
            }
            System.out.println("   [log for admin] cause = " + e.getCause());
            return "SORRY: your file could not be saved, please try again later";   // student never sees the IOException
        } catch (ExamException e) {
            return "ERROR: " + e.getMessage();
        } catch (InvalidSubmissionException e) {
            return "BUG: " + e.getMessage();
        }
    }
}

public class ExamPortal {
    public static void main(String[] args) {
        SubmissionController c = new SubmissionController(new SubmissionService(new SubmissionStore(300), false));

        System.out.println("=== Q2 scenario table (one scenario per exception) ===");
        System.out.println("S1 normal      : " + c.handle("S101", "CSA0917", "answer1.pdf", 120, 60, false));
        System.out.println("S2 window shut : " + c.handle("S101", "CSA0917", "answer2.pdf", 100, 120, false));
        System.out.println("S3 unregistered: " + c.handle("S999", "CSA0917", "answer3.pdf", 100, 30, false));
        System.out.println("S4 too large   : " + c.handle("S102", "CSA0917", "answer4.pdf", 450, 30, false));
        System.out.println("S5 empty name  : " + c.handle("S102", "CSA0917", " ", 50, 30, false));
        System.out.println();
        System.out.println("S6 storage fail (quota left 180 KB, file 190 KB), WITH cause:");
        System.out.println("   result: " + c.handle("S103", "CSA0917", "answer6.pdf", 190, 30, true));

        System.out.println();
        System.out.println("S7 same failure, but re-thrown WITHOUT the cause:");
        SubmissionController lossy = new SubmissionController(new SubmissionService(new SubmissionStore(100), true));
        System.out.println("   result: " + lossy.handle("S103", "CSA0917", "answer7.pdf", 190, 30, true));

        System.out.println();
        try {
            new SubmissionStore(10).save("x.pdf", 50);
        } catch (SubmissionStorageException e) {
            System.out.println("getCause() with chaining   : " + e.getCause());
        }
        try {
            new SubmissionStore(10).saveLossy("x.pdf", 50);
        } catch (SubmissionStorageException e) {
            System.out.println("getCause() without chaining: " + e.getCause());
        }
    }
}
