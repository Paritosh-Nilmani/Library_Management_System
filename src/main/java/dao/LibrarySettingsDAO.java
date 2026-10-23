package dao;

import model.LibrarySettings;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LibrarySettingsDAO {
    public LibrarySettings getSettings() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT max_active_loans, loan_period_days, daily_fine, max_fine FROM library_settings WHERE settings_id=1");
             ResultSet row = statement.executeQuery()) {
            if (!row.next()) throw new SQLException("Library settings are missing.");
            LibrarySettings settings = new LibrarySettings();
            settings.setMaxActiveLoans(row.getInt("max_active_loans"));
            settings.setLoanPeriodDays(row.getInt("loan_period_days"));
            settings.setDailyFine(row.getBigDecimal("daily_fine"));
            settings.setMaxFine(row.getBigDecimal("max_fine"));
            return settings;
        }
    }

    public void updateSettings(LibrarySettings settings, int actorId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE library_settings SET max_active_loans=?, loan_period_days=?, daily_fine=?, max_fine=? WHERE settings_id=1")) {
                update.setInt(1, settings.getMaxActiveLoans());
                update.setInt(2, settings.getLoanPeriodDays());
                update.setBigDecimal(3, settings.getDailyFine());
                update.setBigDecimal(4, settings.getMaxFine());
                if (update.executeUpdate() != 1) throw new SQLException("Library settings are missing.");
                try (PreparedStatement audit = connection.prepareStatement(
                        "INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details) VALUES (?, 'SETTINGS_UPDATED', 'library_settings', '1', ?)")) {
                    audit.setInt(1, actorId);
                    audit.setString(2, "max_loans=" + settings.getMaxActiveLoans() + "; loan_days=" + settings.getLoanPeriodDays()
                            + "; daily_fine=" + settings.getDailyFine().toPlainString() + "; max_fine=" + settings.getMaxFine().toPlainString());
                    audit.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
