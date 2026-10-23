package dao;

import model.ReportRow;
import util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {
    public List<ReportRow> mostBorrowed(LocalDate from, LocalDate to) throws SQLException {
        return dateQuery("SELECT b.title AS label, b.author AS detail, COUNT(*) AS total FROM issued_books ib JOIN books b ON b.book_id=ib.book_id "
                + "WHERE ib.issue_date BETWEEN ? AND ? GROUP BY b.book_id,b.title,b.author ORDER BY total DESC,b.title LIMIT 20", from, to, 2);
    }

    public List<ReportRow> activeBorrowers(LocalDate from, LocalDate to) throws SQLException {
        return dateQuery("SELECT s.name AS label, s.roll_no AS detail, COUNT(*) AS total FROM issued_books ib JOIN students s ON s.student_id=ib.student_id "
                + "WHERE ib.issue_date BETWEEN ? AND ? GROUP BY s.student_id,s.name,s.roll_no ORDER BY total DESC,s.name LIMIT 20", from, to, 2);
    }

    public List<ReportRow> overdueLoans() throws SQLException {
        String sql = "SELECT s.name AS label, CONCAT(b.title, ' (loan #', ib.issue_id, ')') AS detail, "
                + "DATEDIFF(CURRENT_DATE, ib.due_date) AS days_overdue FROM issued_books ib "
                + "JOIN students s ON s.student_id=ib.student_id JOIN books b ON b.book_id=ib.book_id "
                + "WHERE ib.status='ISSUED' AND ib.due_date<CURRENT_DATE ORDER BY ib.due_date LIMIT 200";
        return query(sql, null, null, 0);
    }

    public List<ReportRow> inventory() throws SQLException {
        String sql = "SELECT COALESCE(category, 'Uncategorized') AS label, 'Total / available copies' AS detail, "
                + "COALESCE(SUM(total_copies),0) AS total, COALESCE(SUM(available_copies),0) AS available "
                + "FROM books GROUP BY category ORDER BY label";
        return query(sql, null, null, 1);
    }

    public List<ReportRow> monthlyTrends(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT DATE_FORMAT(activity_date,'%Y-%m') AS label, 'Issues / returns' AS detail, "
                + "SUM(issues) AS issues, SUM(returns) AS returns FROM ("
                + "SELECT issue_date AS activity_date,1 AS issues,0 AS returns FROM issued_books WHERE issue_date BETWEEN ? AND ? "
                + "UNION ALL SELECT return_date,0,1 FROM issued_books WHERE return_date BETWEEN ? AND ?) activity "
                + "GROUP BY DATE_FORMAT(activity_date,'%Y-%m') ORDER BY label";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from)); statement.setDate(2, Date.valueOf(to));
            statement.setDate(3, Date.valueOf(from)); statement.setDate(4, Date.valueOf(to));
            try (ResultSet rows = statement.executeQuery()) {
                List<ReportRow> result = new ArrayList<>();
                while (rows.next()) result.add(new ReportRow(rows.getString("label"), rows.getString("detail"),
                        rows.getLong("issues"), rows.getLong("returns"), BigDecimal.ZERO));
                return result;
            }
        }
    }

    public BigDecimal fineCollection(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM fine_payments WHERE DATE(paid_at) BETWEEN ? AND ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from)); statement.setDate(2, Date.valueOf(to));
            try (ResultSet row = statement.executeQuery()) { return row.next() ? row.getBigDecimal(1) : BigDecimal.ZERO; }
        }
    }

    public BigDecimal outstandingFines() throws SQLException {
        String sql = "SELECT COALESCE(SUM(GREATEST(ib.fine-COALESCE((SELECT SUM(fp.amount) FROM fine_payments fp WHERE fp.issue_id=ib.issue_id),0),0)),0) "
                + "FROM issued_books ib WHERE ib.status <> 'ISSUED'";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) { return row.next() ? row.getBigDecimal(1) : BigDecimal.ZERO; }
    }

    private List<ReportRow> dateQuery(String sql, LocalDate from, LocalDate to, int mode) throws SQLException {
        return query(sql, from, to, mode);
    }

    private List<ReportRow> query(String sql, LocalDate from, LocalDate to, int mode) throws SQLException {
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (from != null) { statement.setDate(1, Date.valueOf(from)); statement.setDate(2, Date.valueOf(to)); }
            try (ResultSet rows = statement.executeQuery()) {
                List<ReportRow> result = new ArrayList<>();
                while (rows.next()) {
                    long value = rows.getLong(mode == 0 ? "days_overdue" : mode == 1 ? "total" : "total");
                    long secondary = mode == 1 ? rows.getLong("available") : 0;
                    result.add(new ReportRow(rows.getString("label"), rows.getString("detail"), value, secondary, BigDecimal.ZERO));
                }
                return result;
            }
        }
    }
}
