package dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditDAO {
    private static final Logger LOG = LoggerFactory.getLogger(AuditDAO.class);

    /** Best-effort audit for legacy CRUD DAOs; circulation and finance write audit rows transactionally. */
    public void record(int actorId, String action, String entity, String entityId, String details) {
        String sql = "INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, actorId);
            statement.setString(2, action);
            statement.setString(3, entity);
            statement.setString(4, entityId);
            statement.setString(5, details);
            statement.executeUpdate();
        } catch (SQLException exception) {
            LOG.error("Unable to write audit entry for action {} on {}", action, entity, exception);
        }
    }
}
