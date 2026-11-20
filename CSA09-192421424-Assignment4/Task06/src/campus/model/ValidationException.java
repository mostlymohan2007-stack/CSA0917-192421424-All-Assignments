package campus.model;

/** Input is invalid (empty name, bad email ...). */
public class ValidationException extends RegistrationException {
    public ValidationException(String message) { super(message); }
}
