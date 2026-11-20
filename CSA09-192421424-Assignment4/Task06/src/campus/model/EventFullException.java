package campus.model;

/** The event has no seats left. */
public class EventFullException extends RegistrationException {
    public EventFullException(String message) { super(message); }
}
