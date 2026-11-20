package campus.dao;

import campus.model.*;

import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/** Data-access contract: the service layer depends on this interface, not on JDBC. */
public interface RegistrationDao {

    List<EventInfo> findEvents() throws SQLException;

    /** Saves (or updates) the student and registers him/her for the event in ONE transaction. */
    void register(Student student, int eventId) throws SQLException, DuplicateRegistrationException, EventFullException;

    /** Number of registrations; eventId == null means all events. */
    long count(Integer eventId) throws SQLException;

    /** One page of rows ordered by reg_id (SQL LIMIT / OFFSET). */
    List<RegistrationRow> findPage(Integer eventId, int limit, int offset) throws SQLException;

    /** Keyset page: rows with reg_id greater than lastRegId (alternative to OFFSET). */
    List<RegistrationRow> findPageAfter(Integer eventId, long lastRegId, int limit) throws SQLException;

    /** Loads EVERYTHING into memory (used only to demonstrate why this does not scale). */
    List<RegistrationRow> findAll(Integer eventId) throws SQLException;

    /** Streams every matching row to the consumer without holding all rows in memory (used by the CSV export). */
    void forEach(Integer eventId, Consumer<RegistrationRow> consumer) throws SQLException;
}
