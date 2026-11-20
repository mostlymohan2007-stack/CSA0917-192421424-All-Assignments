/** A line of the pass file cannot be trusted (wrong field count, bad number, bad date, empty line ...). */
public class DamagedRecordException extends Exception {
    public DamagedRecordException(String message) { super(message); }
}
