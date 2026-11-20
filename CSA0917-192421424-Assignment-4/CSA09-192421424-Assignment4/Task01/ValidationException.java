/** Checked exception: input is wrong and the user can fix it. The message is safe to show on screen. */
public class ValidationException extends Exception {
    public ValidationException(String message) { super(message); }
}
