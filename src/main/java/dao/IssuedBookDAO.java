package dao;

import model.IssuedBook;
import util.DBConnection;
import service.FinePolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IssuedBookDAO {
    private static final String BASE_SELECT = "SELECT ib.issue_id, ib.book_id, ib.student_id, ib.issue_date, ib.due_date, "
            + "ib.return_date, ib.status, ib.fine, b.title AS book_title, b.isbn AS book_isbn, "
            + "COALESCE((SELECT SUM(fp.amount) FROM fine_payments fp WHERE fp.issue_id = ib.issue_id), 0) AS paid_fine, "
            + "s.name AS student_name, s.roll_no AS student_roll_no "
            + "FROM issued_books ib INNER JOIN books b ON ib.book_id = b.book_id "
            + "INNER JOIN students s ON ib.student_id = s.student_id";

    /** Locks the borrower and selected title before checking limits and decrementing inventory. */
    public void issueBook(int bookId, int studentId, LocalDate issueDate, LocalDate dueDate, int actorId) throws SQLException {
        if (issueDate == null || dueDate == null || issueDate.isAfter(LocalDate.now()) || dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Issue and due dates are invalid.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int maximumLoans;
                int loanPeriod;
                try (PreparedStatement settings = connection.prepareStatement(
                        "SELECT max_active_loans, loan_period_days FROM library_settings WHERE settings_id = 1");
                     ResultSet row = settings.executeQuery()) {
                    if (!row.next()) throw new IllegalStateException("Library circulation settings are missing.");
                    maximumLoans = row.getInt("max_active_loans");
                    loanPeriod = row.getInt("loan_period_days");
                }
                if (!dueDate.equals(issueDate.plusDays(loanPeriod))) {
                    throw new IllegalArgumentException("Due date must be " + loanPeriod + " days after the issue date.");
                }

                try (PreparedStatement lockStudent = connection.prepareStatement(
                        "SELECT status FROM students WHERE student_id = ? FOR UPDATE")) {
                    lockStudent.setInt(1, studentId);
                    try (ResultSet student = lockStudent.executeQuery()) {
                        if (!student.next()) throw new IllegalArgumentException("Selected student was not found.");
                        if (!"ACTIVE".equals(student.getString("status"))) {
                            throw new IllegalStateException("Inactive students cannot borrow books.");
                        }
                    }
                }
                int activeLoans = 0;
                try (PreparedStatement active = connection.prepareStatement(
                        "SELECT issue_id FROM issued_books WHERE student_id = ? AND status = 'ISSUED' FOR UPDATE")) {
                    active.setInt(1, studentId);
                    try (ResultSet rows = active.executeQuery()) {
                        while (rows.next()) activeLoans++;
                    }
                }
                if (activeLoans >= maximumLoans) {
                    throw new IllegalStateException("This student has reached the active borrowing limit.");
                }
                try (PreparedStatement duplicate = connection.prepareStatement(
                        "SELECT issue_id FROM issued_books WHERE student_id = ? AND book_id = ? AND status = 'ISSUED' FOR UPDATE")) {
                    duplicate.setInt(1, studentId);
                    duplicate.setInt(2, bookId);
                    try (ResultSet row = duplicate.executeQuery()) {
                        if (row.next()) throw new IllegalStateException("This student already has an active loan for the selected book.");
                    }
                }

                try (PreparedStatement lockBook = connection.prepareStatement(
                        "SELECT available_copies FROM books WHERE book_id = ? FOR UPDATE")) {
                    lockBook.setInt(1, bookId);
                    try (ResultSet book = lockBook.executeQuery()) {
                        if (!book.next()) throw new IllegalArgumentException("Selected book was not found.");
                        if (book.getInt("available_copies") <= 0) {
                            throw new IllegalStateException("No copies are currently available for this book.");
                        }
                    }
                }

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0")) {
                    update.setInt(1, bookId);
                    if (update.executeUpdate() != 1) throw new IllegalStateException("No copies are currently available for this book.");
                }
                long issueId;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO issued_books (book_id, student_id, issue_date, due_date, status, issued_by) "
                                + "VALUES (?, ?, ?, ?, 'ISSUED', ?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insert.setInt(1, bookId);
                    insert.setInt(2, studentId);
                    insert.setDate(3, Date.valueOf(issueDate));
                    insert.setDate(4, Date.valueOf(dueDate));
                    insert.setInt(5, actorId);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not retrieve new loan ID.");
                        issueId = keys.getLong(1);
                    }
                }
                recordAudit(connection, actorId, "BOOK_ISSUED", "issued_book", Long.toString(issueId), "book=" + bookId + "; student=" + studentId);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** Assesses the overdue fine and restores stock exactly once in a single transaction. */
    public BigDecimal returnBook(long issueId, int actorId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int bookId;
                LocalDate dueDate;
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT book_id, due_date, status FROM issued_books WHERE issue_id = ? FOR UPDATE")) {
                    lock.setLong(1, issueId);
                    try (ResultSet row = lock.executeQuery()) {
                        if (!row.next()) throw new IllegalArgumentException("Loan record was not found.");
                        if (!"ISSUED".equals(row.getString("status"))) {
                            throw new IllegalStateException("This loan is no longer active.");
                        }
                        bookId = row.getInt("book_id");
                        dueDate = row.getDate("due_date").toLocalDate();
                    }
                }
                BigDecimal dailyFine;
                BigDecimal maximumFine;
                try (PreparedStatement settings = connection.prepareStatement(
                        "SELECT daily_fine, max_fine FROM library_settings WHERE settings_id = 1");
                     ResultSet row = settings.executeQuery()) {
                    if (!row.next()) throw new IllegalStateException("Library fine settings are missing.");
                    dailyFine = row.getBigDecimal("daily_fine");
                    maximumFine = row.getBigDecimal("max_fine");
                }
                LocalDate returnedOn = LocalDate.now();
                BigDecimal fine = FinePolicy.assess(dueDate, returnedOn, dailyFine, maximumFine);

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE issued_books SET return_date = ?, status = 'RETURNED', fine = ?, returned_by = ? "
                                + "WHERE issue_id = ? AND status = 'ISSUED'")) {
                    update.setDate(1, Date.valueOf(returnedOn));
                    update.setBigDecimal(2, fine);
                    update.setInt(3, actorId);
                    update.setLong(4, issueId);
                    if (update.executeUpdate() != 1) throw new IllegalStateException("This loan is no longer active.");
                }
                try (PreparedStatement updateBook = connection.prepareStatement(
                        "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ? AND available_copies < total_copies")) {
                    updateBook.setInt(1, bookId);
                    if (updateBook.executeUpdate() != 1) throw new SQLException("Inventory was inconsistent while returning this loan.");
                }
                recordAudit(connection, actorId, "BOOK_RETURNED", "issued_book", Long.toString(issueId), "fine=" + fine.toPlainString());
                connection.commit();
                return fine;
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<IssuedBook> getIssuedBooks() throws SQLException {
        return query(BASE_SELECT + " ORDER BY ib.created_at DESC", null);
    }

    public List<IssuedBook> searchIssuedBooks(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) return getIssuedBooks();
        String sql = BASE_SELECT + " WHERE b.title LIKE ? OR b.isbn LIKE ? OR s.name LIKE ? "
                + "OR s.roll_no LIKE ? OR ib.status LIKE ? OR CAST(ib.issue_id AS CHAR) LIKE ? ORDER BY ib.created_at DESC";
        return query(sql, "%" + keyword.trim() + "%");
    }

    public List<IssuedBook> searchIssuedBooks(String keyword, int page, int pageSize) throws SQLException {
        return searchIssuedBooks(keyword, "", page, pageSize);
    }

    public List<IssuedBook> searchIssuedBooks(String keyword, String status, int page, int pageSize) throws SQLException {
        List<String> values = new ArrayList<>();
        String where = buildLoanWhere(keyword, status, values);
        String sql = BASE_SELECT + where + " ORDER BY ib.created_at DESC LIMIT ? OFFSET ?";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (String value : values) statement.setString(index++, value);
            statement.setInt(index++, pageSize);
            statement.setLong(index, (long) (page - 1) * pageSize);
            try (ResultSet rows = statement.executeQuery()) {
                List<IssuedBook> records = new ArrayList<>();
                while (rows.next()) records.add(mapIssuedBook(rows));
                return records;
            }
        }
    }

    public int countSearchIssuedBooks(String keyword) throws SQLException {
        return countSearchIssuedBooks(keyword, "");
    }

    public int countSearchIssuedBooks(String keyword, String status) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM issued_books ib JOIN books b ON b.book_id=ib.book_id "
                + "JOIN students s ON s.student_id=ib.student_id" + buildLoanWhere(keyword, status, values);
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (String value : values) statement.setString(index++, value);
            try (ResultSet row = statement.executeQuery()) { return row.next() ? row.getInt(1) : 0; }
        }
    }

    private String buildLoanWhere(String keyword, String status, List<String> values) {
        List<String> predicates = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            predicates.add("(b.title LIKE ? OR b.isbn LIKE ? OR s.name LIKE ? OR s.roll_no LIKE ? OR ib.status LIKE ? OR CAST(ib.issue_id AS CHAR) LIKE ?)");
            String term = "%" + keyword.trim() + "%";
            for (int i = 0; i < 6; i++) values.add(term);
        }
        if ("ISSUED".equals(status) || "RETURNED".equals(status) || "LOST".equals(status) || "DAMAGED".equals(status)) {
            predicates.add("ib.status = ?"); values.add(status);
        } else if ("OVERDUE".equals(status)) {
            predicates.add("ib.status = 'ISSUED' AND ib.due_date < CURRENT_DATE");
        }
        return predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);
    }

    public int countActiveIssuedBooks() throws SQLException {
        return count("SELECT COUNT(*) FROM issued_books WHERE status = 'ISSUED'");
    }

    public int countOverdueLoans() throws SQLException {
        return count("SELECT COUNT(*) FROM issued_books WHERE status = 'ISSUED' AND due_date < CURRENT_DATE");
    }

    public int getLoanPeriodDays() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT loan_period_days FROM library_settings WHERE settings_id = 1");
             ResultSet row = statement.executeQuery()) {
            if (!row.next()) throw new SQLException("Library circulation settings are missing.");
            return row.getInt(1);
        }
    }

    public BigDecimal sumOutstandingFines() throws SQLException {
        String sql = "SELECT COALESCE(SUM(GREATEST(ib.fine - COALESCE((SELECT SUM(fp.amount) FROM fine_payments fp "
                + "WHERE fp.issue_id = ib.issue_id), 0), 0)), 0) FROM issued_books ib WHERE ib.status <> 'ISSUED'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getBigDecimal(1) : BigDecimal.ZERO;
        }
    }

    public List<IssuedBook> getRecentActivity(int limit) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY COALESCE(ib.return_date, ib.issue_date) DESC, ib.issue_id DESC LIMIT ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, Math.min(limit, 50)));
            try (ResultSet rows = statement.executeQuery()) {
                List<IssuedBook> records = new ArrayList<>();
                while (rows.next()) records.add(mapIssuedBook(rows));
                return records;
            }
        }
    }

    public List<IssuedBook> getLoansForStudent(int studentId) throws SQLException {
        String sql = BASE_SELECT + " WHERE s.student_id = ? ORDER BY ib.issue_date DESC, ib.issue_id DESC LIMIT 200";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rows = statement.executeQuery()) {
                List<IssuedBook> records = new ArrayList<>();
                while (rows.next()) records.add(mapIssuedBook(rows));
                return records;
            }
        }
    }

    public List<IssuedBook> getLoansForBook(int bookId) throws SQLException {
        String sql = BASE_SELECT + " WHERE b.book_id = ? ORDER BY ib.issue_date DESC, ib.issue_id DESC LIMIT 100";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet rows = statement.executeQuery()) {
                List<IssuedBook> records = new ArrayList<>();
                while (rows.next()) records.add(mapIssuedBook(rows));
                return records;
            }
        }
    }

    private List<IssuedBook> query(String sql, String term) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (term != null) for (int i = 1; i <= 6; i++) statement.setString(i, term);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<IssuedBook> records = new ArrayList<>();
                while (resultSet.next()) records.add(mapIssuedBook(resultSet));
                return records;
            }
        }
    }

    private IssuedBook mapIssuedBook(ResultSet resultSet) throws SQLException {
        IssuedBook loan = new IssuedBook();
        loan.setIssueId(resultSet.getLong("issue_id"));
        loan.setBookId(resultSet.getInt("book_id"));
        loan.setStudentId(resultSet.getInt("student_id"));
        loan.setIssueDate(resultSet.getDate("issue_date").toLocalDate());
        loan.setDueDate(resultSet.getDate("due_date").toLocalDate());
        Date returned = resultSet.getDate("return_date");
        loan.setReturnDate(returned == null ? null : returned.toLocalDate());
        loan.setStatus(resultSet.getString("status"));
        loan.setFine(resultSet.getBigDecimal("fine"));
        loan.setPaidFine(resultSet.getBigDecimal("paid_fine"));
        loan.setBookTitle(resultSet.getString("book_title"));
        loan.setBookIsbn(resultSet.getString("book_isbn"));
        loan.setStudentName(resultSet.getString("student_name"));
        loan.setStudentRollNo(resultSet.getString("student_roll_no"));
        return loan;
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet row = statement.executeQuery()) {
            return row.next() ? row.getInt(1) : 0;
        }
    }

    private void recordAudit(Connection connection, int actorId, String action, String entity, String entityId, String details)
            throws SQLException {
        try (PreparedStatement audit = connection.prepareStatement(
                "INSERT INTO audit_logs (actor_id, action, entity_type, entity_id, details) VALUES (?, ?, ?, ?, ?)")) {
            audit.setInt(1, actorId);
            audit.setString(2, action);
            audit.setString(3, entity);
            audit.setString(4, entityId);
            audit.setString(5, details);
            audit.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { /* Preserve the original failure. */ }
    }
}
