package campus.model;

/** A student who registers for an event. */
public final class Student {
    private final String studentId, name, email, department;

    public Student(String studentId, String name, String email, String department) {
        this.studentId = studentId; this.name = name; this.email = email; this.department = department;
    }
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
}
