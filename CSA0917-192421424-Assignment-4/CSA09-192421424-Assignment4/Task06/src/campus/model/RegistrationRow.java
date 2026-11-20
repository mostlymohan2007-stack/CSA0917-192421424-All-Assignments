package campus.model;

/** One line of the registrations table (read-only view of a registration joined with student and event). */
public final class RegistrationRow {
    public final long regId;
    public final String studentId, studentName, eventTitle, registeredAt;

    public RegistrationRow(long regId, String studentId, String studentName, String eventTitle, String registeredAt) {
        this.regId = regId; this.studentId = studentId; this.studentName = studentName;
        this.eventTitle = eventTitle; this.registeredAt = registeredAt;
    }
}
