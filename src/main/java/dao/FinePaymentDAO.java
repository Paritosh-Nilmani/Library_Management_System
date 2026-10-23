package dao;

import model.FinePayment;
import util.DBConnection;
import service.FinePolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FinePaymentDAO {
    private static final String PAYMENT_SELECT = "SELECT fp.payment_id, fp.issue_id, fp.amount, fp.payment_method, fp.paid_at, fp.note, "
            + "b.title AS book_title, s.name AS student_name, a.full_name AS recorded_by "
            + "FROM fine_payments fp JOIN issued_books ib ON ib.issue_id = fp.issue_id "
            + "JOIN books b ON b.book_id = ib.book_id JOIN students s ON s.student_id = ib.student_id "
            + "LEFT JOIN admin a ON a.admin_id = fp.recorded_by";

    public void recordPayment(long issueId, BigDecimal amount, String method, String note, int actorId) throws SQLException {
        if (amount == null || !amount.abs().equals(amount) || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Payment amount may have at most two decimal places.");
        }
        if (!List.of("CASH", "CARD", "BANK_TRANSFER", "OTHER").contains(method)) {
            throw new IllegalArgumentException("Select a valid payment method.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BigDecimal assessed;
                String status;
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT fine, status FROM issued_books WHERE issue_id = ? FOR UPDATE")) {
                    lock.setLong(1, issueId);
                    try (ResultSet row = lock.executeQuery()) {
                        if (!row.next()) throw new IllegalArgumentException("Loan record was not found.");
                        assessed = row.getBigDecimal("fine");
                        status = row.getString("status");
                    }
                }
                if (!"RETURNED".equals(status)) throw new IllegalStateException("A fine payment can be recorded only after a book is returned.");
                BigDecimal paid = BigDecimal.ZERO;
                try (PreparedStatement sum = connection.prepareStatement(
                        "SELECT COALESCE(SUM(amount), 0) FROM fine_payments WHERE issue_id = ?")) {
                    sum.setLong(1, issueId);
                    try (ResultSet row = sum.executeQuery()) { row.next(); paid = row.getBigDecimal(1); }
                }
                BigDecimal balance = assessed.subtract(paid).max(BigDecimal.ZERO);
                try {
                    FinePolicy.balanceAfterPayment(assessed, paid, amount);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Payment exceeds the outstanding balance of "
                            + balance.setScale(2, RoundingMode.HALF_UP).toPlainString() + ".");
                }

                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO fine_payments (issue_id, amount, payment_method, recorded_by, note) VALUES (?, ?, ?, ?, ?)")) {
                    insert.setLong(1, issueId);
                    insert.setBigDecimal(2, amount);
                    insert.setString(3, method);
                    insert.setInt(4, actorId);
                    insert.setString(5, note == null || note.isBlank() ? null : note.trim());
                    insert.executeUpdate();
                }
                try (PreparedStatement audit = connection.prepareStatement(
                        "INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details) VALUES (?, 'FINE_PAYMENT_RECORDED', 'issued_book', ?, ?)")) {
                    audit.setInt(1, actorId);
                    audit.setString(2, Long.toString(issueId));
                    audit.setString(3, "amount=" + amount.toPlainString() + "; method=" + method);
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

    public List<FinePayment> listPayments(int page, int pageSize) throws SQLException {
        String sql = PAYMENT_SELECT + " ORDER BY fp.paid_at DESC, fp.payment_id DESC LIMIT ? OFFSET ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pageSize);
            statement.setLong(2, (long) (page - 1) * pageSize);
            try (ResultSet rows = statement.executeQuery()) {
                List<FinePayment> payments = new ArrayList<>();
                while (rows.next()) payments.add(map(rows));
                return payments;
            }
        }
    }

    public List<FinePayment> listAllPayments() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(PAYMENT_SELECT + " ORDER BY fp.paid_at DESC");
             ResultSet rows = statement.executeQuery()) {
            List<FinePayment> payments = new ArrayList<>();
            while (rows.next()) payments.add(map(rows));
            return payments;
        }
    }

    public int countPayments() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM fine_payments");
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getInt(1) : 0;
        }
    }

    private FinePayment map(ResultSet row) throws SQLException {
        FinePayment payment = new FinePayment();
        payment.setPaymentId(row.getLong("payment_id"));
        payment.setIssueId(row.getLong("issue_id"));
        payment.setAmount(row.getBigDecimal("amount"));
        payment.setMethod(row.getString("payment_method"));
        payment.setPaidAt(row.getTimestamp("paid_at").toLocalDateTime());
        payment.setNote(row.getString("note"));
        payment.setBookTitle(row.getString("book_title"));
        payment.setStudentName(row.getString("student_name"));
        payment.setRecordedBy(row.getString("recorded_by"));
        return payment;
    }
}
