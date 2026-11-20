/**
 * Q2 follow-up question: "Explain what would be printed in the Cause line if the IOException were not
 * passed to the constructor." This program shows it: getCause() is null, so calling getMessage() on it
 * throws a NullPointerException (so the Cause line is never printed).
 */
public class StorageDemoNoCause {
    static class ExamException extends Exception {
        ExamException(String m, Throwable cause) { super(m, cause); }
    }
    public static void main(String[] args) {
        try {
            throw new ExamException("Could not store answer02.pdf", null);   // cause NOT passed
        } catch (ExamException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("getCause() = " + e.getCause());
            try {
                System.out.println("Cause: " + e.getCause().getMessage());
            } catch (NullPointerException npe) {
                System.out.println("Cause line crashes with NullPointerException: " + npe.getMessage());
            }
        }
    }
}
