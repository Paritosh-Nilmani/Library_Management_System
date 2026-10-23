package dao;

import model.Book;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class BookDAO {
    private static final String BASE_SELECT = "SELECT book_id, title, author, publisher, category, isbn, publication_year, "
            + "total_copies, available_copies, shelf_location FROM books";

    public boolean addBook(Book book) throws SQLException {
        String sql = "INSERT INTO books (title, author, publisher, category, isbn, publication_year, total_copies, available_copies, shelf_location) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillBookStatement(statement, book);
            statement.setInt(8, book.getTotalCopies());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean updateBook(Book book) throws SQLException {
        String sql = "UPDATE books SET title = ?, author = ?, publisher = ?, category = ?, isbn = ?, publication_year = ?, "
                + "total_copies = ?, available_copies = ?, shelf_location = ? WHERE book_id = ?";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int issuedCopies;
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT total_copies, available_copies FROM books WHERE book_id = ? FOR UPDATE")) {
                    lock.setInt(1, book.getBookId());
                    try (ResultSet row = lock.executeQuery()) {
                        if (!row.next()) {
                            connection.rollback();
                            return false;
                        }
                        issuedCopies = row.getInt("total_copies") - row.getInt("available_copies");
                    }
                }
                if (book.getTotalCopies() < issuedCopies) {
                    throw new IllegalArgumentException("Total copies cannot be lower than the number currently issued (" + issuedCopies + ").");
                }
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, book.getTitle());
                    statement.setString(2, book.getAuthor());
                    statement.setString(3, nullIfBlank(book.getPublisher()));
                    statement.setString(4, nullIfBlank(book.getCategory()));
                    statement.setString(5, nullIfBlank(book.getIsbn()));
                    if (book.getPublicationYear() <= 0) statement.setNull(6, java.sql.Types.SMALLINT);
                    else statement.setInt(6, book.getPublicationYear());
                    statement.setInt(7, book.getTotalCopies());
                    statement.setInt(8, book.getTotalCopies() - issuedCopies);
                    statement.setString(9, nullIfBlank(book.getShelfLocation()));
                    statement.setInt(10, book.getBookId());
                    boolean updated = statement.executeUpdate() > 0;
                    connection.commit();
                    return updated;
                }
            } catch (SQLException | RuntimeException exception) {
                try { connection.rollback(); } catch (SQLException ignored) { }
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean deleteBook(int bookId) throws SQLException {
        String sql = "DELETE FROM books WHERE book_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            return statement.executeUpdate() > 0;
        }
    }

    public Book getBookById(int bookId) throws SQLException {
        String sql = BASE_SELECT + " WHERE book_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapBook(resultSet);
                }
            }
        }

        return null;
    }

    public List<Book> getAllBooks() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY title";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapBooks(resultSet);
        }
    }

    public List<Book> getAvailableBooks() throws SQLException {
        String sql = BASE_SELECT + " WHERE available_copies > 0 ORDER BY title";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapBooks(resultSet);
        }
    }

    public List<Book> searchBooks(String keyword) throws SQLException {
        return searchBooks(keyword, 1, Integer.MAX_VALUE);
    }

    public List<Book> searchBooks(String keyword, int page, int pageSize) throws SQLException {
        return searchBooks(keyword, null, null, "title", page, pageSize);
    }

    public List<Book> searchBooks(String keyword, String category, String availability, String sort, int page, int pageSize) throws SQLException {
        List<String> values = new ArrayList<>();
        String where = buildWhere(keyword, category, availability, values);
        String sql = BASE_SELECT + where + " ORDER BY " + orderBy(sort) + " LIMIT ? OFFSET ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bind(statement, values);
            statement.setInt(index++, pageSize);
            statement.setLong(index, (long) (page - 1) * pageSize);
            try (ResultSet resultSet = statement.executeQuery()) { return mapBooks(resultSet); }
        }
    }

    public int countSearchBooks(String keyword) throws SQLException {
        return countSearchBooks(keyword, null, null);
    }

    public int countSearchBooks(String keyword, String category, String availability) throws SQLException {
        List<String> values = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM books" + buildWhere(keyword, category, availability, values);
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet row = statement.executeQuery()) { return row.next() ? row.getInt(1) : 0; }
        }
    }

    public List<String> listCategories() throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT DISTINCT category FROM books WHERE category IS NOT NULL AND category <> '' ORDER BY category");
             ResultSet rows = statement.executeQuery()) {
            List<String> categories = new ArrayList<>();
            while (rows.next()) categories.add(rows.getString(1));
            return categories;
        }
    }

    private String buildWhere(String keyword, String category, String availability, List<String> values) {
        List<String> predicates = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            predicates.add("(title LIKE ? OR author LIKE ? OR publisher LIKE ? OR category LIKE ? OR isbn LIKE ? OR CAST(publication_year AS CHAR) LIKE ?)");
            String term = "%" + keyword.trim() + "%";
            for (int i = 0; i < 6; i++) values.add(term);
        }
        if (category != null && !category.isBlank()) { predicates.add("category = ?"); values.add(category); }
        if ("available".equals(availability)) predicates.add("available_copies > 0");
        if ("out-of-stock".equals(availability)) predicates.add("available_copies = 0");
        return predicates.isEmpty() ? "" : " WHERE " + String.join(" AND ", predicates);
    }

    private int bind(PreparedStatement statement, List<String> values) throws SQLException {
        int index = 1;
        for (String value : values) statement.setString(index++, value);
        return index;
    }

    private String orderBy(String sort) {
        switch (sort == null ? "title" : sort) {
            case "author": return "author, title";
            case "year": return "publication_year DESC, title";
            case "available": return "available_copies DESC, title";
            default: return "title";
        }
    }

    public int countBooks() throws SQLException {
        return count("SELECT COUNT(*) FROM books");
    }

    public int countAvailableCopies() throws SQLException {
        return count("SELECT COALESCE(SUM(available_copies), 0) FROM books");
    }

    public int countTotalCopies() throws SQLException {
        return count("SELECT COALESCE(SUM(total_copies), 0) FROM books");
    }

    public List<Book> getLowStockBooks(int limit) throws SQLException {
        String sql = BASE_SELECT + " WHERE available_copies <= 2 ORDER BY available_copies, title LIMIT ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, Math.min(limit, 100)));
            try (ResultSet resultSet = statement.executeQuery()) { return mapBooks(resultSet); }
        }
    }

    private void fillBookStatement(PreparedStatement statement, Book book) throws SQLException {
        statement.setString(1, book.getTitle());
        statement.setString(2, book.getAuthor());
        statement.setString(3, nullIfBlank(book.getPublisher()));
        statement.setString(4, nullIfBlank(book.getCategory()));
        statement.setString(5, nullIfBlank(book.getIsbn()));
        if (book.getPublicationYear() <= 0) statement.setNull(6, java.sql.Types.SMALLINT);
        else statement.setInt(6, book.getPublicationYear());
        statement.setInt(7, book.getTotalCopies());
        statement.setInt(8, book.getAvailableCopies());
        statement.setString(9, nullIfBlank(book.getShelfLocation()));
    }

    private List<Book> mapBooks(ResultSet resultSet) throws SQLException {
        List<Book> books = new ArrayList<>();
        while (resultSet.next()) {
            books.add(mapBook(resultSet));
        }
        return books;
    }

    private Book mapBook(ResultSet resultSet) throws SQLException {
        Book book = new Book();
        book.setBookId(resultSet.getInt("book_id"));
        book.setTitle(resultSet.getString("title"));
        book.setAuthor(resultSet.getString("author"));
        book.setPublisher(resultSet.getString("publisher"));
        book.setCategory(resultSet.getString("category"));
        book.setIsbn(resultSet.getString("isbn"));
        book.setPublicationYear(resultSet.getInt("publication_year"));
        book.setTotalCopies(resultSet.getInt("total_copies"));
        book.setAvailableCopies(resultSet.getInt("available_copies"));
        book.setShelfLocation(resultSet.getString("shelf_location"));
        return book;
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }
        return 0;
    }

    private String nullIfBlank(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
