package campus.model;

/** Base class of the expected business failures. The message is safe to show to the user. */
public class RegistrationException extends Exception {
    public RegistrationException(String message) { super(message); }
}
