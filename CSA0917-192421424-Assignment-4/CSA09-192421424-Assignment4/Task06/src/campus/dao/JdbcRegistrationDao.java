package campus.dao;

import campus.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * JDBC implementation. Rules of this class:
 *  - every statement is a PreparedStatement (no string concatenation of user data -> no SQL injection);
 *  - every Connection / Statement / ResultSet is closed by try-with-resources;
 *  - register() is one transaction: lock the event row, upsert the student, check capacity, insert the registration;
 *  - duplicates are finally guaranteed by the UNIQUE KEY uq_student_event, not only by a pre-check.
 */
public class JdbcRegistrationDao implements RegistrationDao {
    private static final String SELECT_ROWS =
            "SELECT r.reg_id, r.student_id, s.name, e.title, r.registered_at "
          + "FROM registration r JOIN student s ON s.student_id = r.student_id JOIN event e ON e.event_id = r.event_id ";

    private final Db db;

    public JdbcRegistrationDao(Db db) { this.db = db; }

    @Override
    public List<EventInfo> findEvents() throws SQLException {
        List<EventInfo> list = new ArrayList<>();
        try (Connection c = db.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT event_id, title, venue, event_date, capacity FROM event ORDER BY event_id")) {
            while (rs.next()) list.add(new EventInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5)));
        }
        return list;
    }

    @Override
    public void register(Student s, int eventId) throws SQLException, DuplicateRegistrationException, EventFullException {
        try (Connection c = db.get()) {
            c.setAutoCommit(false);
            try {
                int capacity;
                try (PreparedStatement ps = c.prepareStatement("SELECT capacity FROM event WHERE event_id = ? FOR UPDATE")) {
                    ps.setInt(1, eventId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Unknown event id " + eventId);
                        capacity = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO student (student_id, name, email, department) VALUES (?, ?, ?, ?) AS new_row "
                      + "ON DUPLICATE KEY UPDATE name = new_row.name, email = new_row.email, department = new_row.department")) {
                    ps.setString(1, s.getStudentId()); ps.setString(2, s.getName());
                    ps.setString(3, s.getEmail());     ps.setString(4, s.getDepartment());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM registration WHERE event_id = ?")) {
                    ps.setInt(1, eventId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getLong(1) >= capacity) throw new EventFullException("This event is full (" + capacity + " seats).");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO registration (student_id, event_id) VALUES (?, ?)")) {
                    ps.setString(1, s.getStudentId());
                    ps.setInt(2, eventId);
                    ps.executeUpdate();
                } catch (SQLIntegrityConstraintViolationException e) {
                    if (e.getMessage() != null && e.getMessage().contains("uq_student_event"))
                        throw new DuplicateRegistrationException("Student " + s.getStudentId() + " is already registered for this event.");
                    throw e;
                }
                c.commit();
            } catch (SQLException | DuplicateRegistrationException | EventFullException e) {
                c.rollback();
                throw e;
            }
        }
    }

    @Override
    public long count(Integer eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registration" + (eventId == null ? "" : " WHERE event_id = ?");
        try (Connection c = db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId != null) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    @Override
    public List<RegistrationRow> findPage(Integer eventId, int limit, int offset) throws SQLException {
        String sql = SELECT_ROWS + (eventId == null ? "" : "WHERE r.event_id = ? ") + "ORDER BY r.reg_id LIMIT ? OFFSET ?";
        try (Connection c = db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (eventId != null) ps.setInt(i++, eventId);
            ps.setInt(i++, limit);
            ps.setInt(i, offset);
            return readRows(ps);
        }
    }

    @Override
    public List<RegistrationRow> findPageAfter(Integer eventId, long lastRegId, int limit) throws SQLException {
        String sql = SELECT_ROWS + "WHERE r.reg_id > ? " + (eventId == null ? "" : "AND r.event_id = ? ") + "ORDER BY r.reg_id LIMIT ?";
        try (Connection c = db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, lastRegId);
            if (eventId != null) ps.setInt(i++, eventId);
            ps.setInt(i, limit);
            return readRows(ps);
        }
    }

    @Override
    public List<RegistrationRow> findAll(Integer eventId) throws SQLException {
        String sql = SELECT_ROWS + (eventId == null ? "" : "WHERE r.event_id = ? ") + "ORDER BY r.reg_id";
        try (Connection c = db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId != null) ps.setInt(1, eventId);
            return readRows(ps);
        }
    }

    @Override
    public void forEach(Integer eventId, Consumer<RegistrationRow> consumer) throws SQLException {
        String sql = SELECT_ROWS + (eventId == null ? "" : "WHERE r.event_id = ? ") + "ORDER BY r.reg_id";
        try (Connection c = db.get();
             PreparedStatement ps = c.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ps.setFetchSize(Integer.MIN_VALUE);                    // MySQL: stream row by row instead of buffering everything
            if (eventId != null) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) consumer.accept(toRow(rs));
            }
        }
    }

    private static List<RegistrationRow> readRows(PreparedStatement ps) throws SQLException {
        List<RegistrationRow> rows = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) rows.add(toRow(rs));
        }
        return rows;
    }

    private static RegistrationRow toRow(ResultSet rs) throws SQLException {
        return new RegistrationRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
    }
}
