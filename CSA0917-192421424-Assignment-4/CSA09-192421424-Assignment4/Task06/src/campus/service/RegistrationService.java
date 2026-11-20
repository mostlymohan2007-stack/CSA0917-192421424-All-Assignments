package campus.service;

import campus.dao.RegistrationDao;
import campus.model.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Business rules and use cases. No Swing/AWT and no JDBC here:
 *  - validation of every field,
 *  - register (duplicate / full rules are enforced by the DAO transaction and reported as domain exceptions),
 *  - paging, filtering and CSV export.
 */
public class RegistrationService {
    public static final List<String> DEPARTMENTS = List.of("CSE", "ECE", "MECH", "CIVIL", "EEE", "IT");
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9]{6,10}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .'-]{1,59}");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]{1,40}@[A-Za-z0-9.-]{1,30}\\.[A-Za-z]{2,}");

    private final RegistrationDao dao;

    public RegistrationService(RegistrationDao dao) { this.dao = dao; }

    /** Pseudocode "Register": validate -> (DAO transaction: lock event, upsert student, capacity, insert) -> map failures. */
    public void register(String studentId, String name, String email, String department, EventInfo event)
            throws RegistrationException, SQLException {
        String id = studentId == null ? "" : studentId.trim();
        String nm = name == null ? "" : name.trim();
        String em = email == null ? "" : email.trim();
        if (!ID.matcher(id).matches()) throw new ValidationException("Student ID must be 6 to 10 letters or digits.");
        if (!NAME.matcher(nm).matches()) throw new ValidationException("Name must be 2 to 60 characters: letters, space, . ' -");
        if (!EMAIL.matcher(em).matches()) throw new ValidationException("Enter a valid e-mail address, for example name@college.edu.");
        if (department == null || !DEPARTMENTS.contains(department)) throw new ValidationException("Select a department.");
        if (event == null) throw new ValidationException("Select an event.");
        dao.register(new Student(id.toUpperCase(), nm, em, department), event.getEventId());
    }

    public List<EventInfo> events() throws SQLException { return dao.findEvents(); }

    /** Result of one page request. */
    public static final class Page {
        public final List<RegistrationRow> rows;
        public final long total;
        public final int page, pageSize;
        Page(List<RegistrationRow> rows, long total, int page, int pageSize) { this.rows = rows; this.total = total; this.page = page; this.pageSize = pageSize; }
        public int pages() { return (int) Math.max(1, (total + pageSize - 1) / pageSize); }
    }

    /** Pseudocode "Filter": count rows for the filter, then fetch ONE page with LIMIT/OFFSET. */
    public Page loadPage(Integer eventId, int page, int pageSize) throws SQLException {
        long total = dao.count(eventId);
        int pages = (int) Math.max(1, (total + pageSize - 1) / pageSize);
        int p = Math.min(Math.max(page, 0), pages - 1);
        return new Page(dao.findPage(eventId, pageSize, p * pageSize), total, p, pageSize);
    }

    /** Writes every matching registration to a CSV file, streaming (memory stays small). Returns the number of rows. */
    public int exportCsv(Integer eventId, Path file) throws SQLException, IOException {
        int[] n = {0};
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("reg_id,student_id,student_name,event,registered_at\n");
            try {
                dao.forEach(eventId, r -> {
                    try {
                        w.write(r.regId + "," + csv(r.studentId) + "," + csv(r.studentName) + "," + csv(r.eventTitle) + "," + csv(r.registeredAt) + "\n");
                        n[0]++;
                    } catch (IOException e) { throw new UncheckedIOException(e); }
                });
            } catch (UncheckedIOException e) { throw e.getCause(); }
        }
        return n[0];
    }

    private static String csv(String s) {
        if (s == null) return "";
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}
