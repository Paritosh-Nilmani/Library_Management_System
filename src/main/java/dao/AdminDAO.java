package dao;

import at.favre.lib.crypto.bcrypt.BCrypt;
import model.Admin;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDAO {
    // A dummy hash keeps unknown-user attempts on the same expensive verification path.
    private static final String DUMMY_HASH = BCrypt.withDefaults().hashToString(12, "not-a-real-password".toCharArray());

    public Admin validateAdmin(String username, String password) throws SQLException {
        String sql = "SELECT admin_id, username, password_hash, full_name FROM admin WHERE username = ? AND active = 1";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String hash = resultSet.getString("password_hash");
                    boolean verified = false;
                    try {
                        verified = password != null && hash != null
                                && BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
                    } catch (IllegalArgumentException ignored) {
                        // Invalid stored hash is treated as an authentication failure.
                    }
                    if (verified) {
                        return new Admin(resultSet.getInt("admin_id"), resultSet.getString("username"),
                                resultSet.getString("full_name"));
                    }
                    return null;
                }
            }
        }
        BCrypt.verifyer().verify((password == null ? "" : password).toCharArray(), DUMMY_HASH);
        return null;
    }

    public boolean updatePassword(int adminId, String currentPassword, String newPassword) throws SQLException {
        String selectSql = "SELECT password_hash FROM admin WHERE admin_id = ? AND active = 1";
        String updateSql = "UPDATE admin SET password_hash = ?, password_changed_at = CURRENT_TIMESTAMP WHERE admin_id = ?";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
            String currentHash;
            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setInt(1, adminId);
                try (ResultSet row = select.executeQuery()) {
                    if (!row.next()) { connection.rollback(); return false; }
                    currentHash = row.getString("password_hash");
                }
            }
            if (currentPassword == null || !BCrypt.verifyer().verify(currentPassword.toCharArray(), currentHash).verified) {
                connection.rollback();
                return false;
            }
            String newHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, newHash);
                update.setInt(2, adminId);
                if (update.executeUpdate() != 1) { connection.rollback(); return false; }
            }
            try (PreparedStatement audit = connection.prepareStatement(
                    "INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details) VALUES (?, 'PASSWORD_CHANGED', 'admin', ?, 'Password changed by account owner')")) {
                audit.setInt(1, adminId);
                audit.setString(2, Integer.toString(adminId));
                audit.executeUpdate();
            }
            connection.commit();
            return true;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean createFirstAdmin(String username, String fullName, String password) throws SQLException {
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement count = connection.prepareStatement("SELECT COUNT(*) FROM admin WHERE active = 1");
                     ResultSet row = count.executeQuery()) {
                    row.next();
                    if (row.getInt(1) != 0) { connection.rollback(); return false; }
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO admin (username, password_hash, full_name, role, active) VALUES (?, ?, ?, 'ADMIN', 1) "
                                + "ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), full_name = VALUES(full_name), role = 'ADMIN', active = 1")) {
                    insert.setString(1, username);
                    insert.setString(2, hash);
                    insert.setString(3, fullName);
                    insert.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
