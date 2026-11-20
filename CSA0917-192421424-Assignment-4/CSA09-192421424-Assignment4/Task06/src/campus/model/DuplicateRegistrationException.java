package campus.model;

/** The student is already registered for this event. */
public class DuplicateRegistrationException extends RegistrationException {
    public DuplicateRegistrationException(String message) { super(message); }
}
