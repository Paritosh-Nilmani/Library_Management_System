package servlet;

import dao.BookDAO;
import dao.AuditDAO;
import dao.IssuedBookDAO;
import model.Book;
import service.IsbnValidator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final BookDAO bookDAO = new BookDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final IssuedBookDAO issuedBookDAO = new IssuedBookDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        String action = value(request.getParameter("action"));
        try {
            if ("new".equals(action)) {
                Book book = new Book();
                book.setTotalCopies(1);
                book.setAvailableCopies(1);
                showForm(request, response, book, "create");
            } else if ("edit".equals(action) || "view".equals(action)) {
                int bookId = Integer.parseInt(request.getParameter("id"));
                Book book = bookDAO.getBookById(bookId);
                if (book == null) {
                    setFlash(request, "warning", "Book not found.");
                    response.sendRedirect(request.getContextPath() + "/books");
                    return;
                }
                if ("view".equals(action)) {
                    request.setAttribute("book", book);
                    request.setAttribute("loans", issuedBookDAO.getLoansForBook(bookId));
                    request.getRequestDispatcher("/WEB-INF/jsp/book-details.jsp").forward(request, response);
                } else {
                    showForm(request, response, book, "update");
                }
            } else {
                listBooks(request, response);
            }
        } catch (NumberFormatException exception) {
            setFlash(request, "danger", "Invalid book id.");
            response.sendRedirect(request.getContextPath() + "/books");
        } catch (SQLException exception) {
            throw new ServletException("Unable to process book request.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");
        String action = value(request.getParameter("action"));

        try {
            if ("create".equals(action)) {
                createBook(request, response);
            } else if ("update".equals(action)) {
                updateBook(request, response);
            } else if ("delete".equals(action)) {
                deleteBook(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/books");
            }
        } catch (SQLException exception) {
            throw new ServletException("Unable to save book data.", exception);
        }
    }

    private void listBooks(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        String keyword = value(request.getParameter("keyword"));
        String category = value(request.getParameter("category"));
        String availability = value(request.getParameter("availability"));
        if (!"available".equals(availability) && !"out-of-stock".equals(availability)) availability = "";
        String sort = value(request.getParameter("sort"));
        if (!java.util.Set.of("title", "author", "year", "available").contains(sort)) sort = "title";
        int pageSize = 20;
        int page = parsePage(request.getParameter("page"));
        int totalRows = bookDAO.countSearchBooks(keyword, category, availability);
        int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) pageSize));
        page = Math.min(page, totalPages);
        request.setAttribute("books", bookDAO.searchBooks(keyword, category, availability, sort, page, pageSize));
        request.setAttribute("categories", bookDAO.listCategories());
        request.setAttribute("keyword", keyword);
        request.setAttribute("category", category);
        request.setAttribute("availability", availability);
        request.setAttribute("sort", sort);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.getRequestDispatcher("/WEB-INF/jsp/books.jsp").forward(request, response);
    }

    private int parsePage(String raw) {
        try { return Math.max(1, Integer.parseInt(raw)); } catch (RuntimeException ignored) { return 1; }
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, Book book, String formAction)
            throws ServletException, IOException {
        request.setAttribute("book", book);
        request.setAttribute("formAction", formAction);
        request.getRequestDispatcher("/WEB-INF/jsp/book-form.jsp").forward(request, response);
    }

    private void createBook(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        List<String> errors = new ArrayList<>();
        Book book = readBook(request, false, errors);

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            showForm(request, response, book, "create");
            return;
        }

        try {
            bookDAO.addBook(book);
            auditDAO.record(currentAdmin(request).getAdminId(), "BOOK_CREATED", "book", book.getIsbn(), book.getTitle());
            setFlash(request, "success", "Book added successfully.");
            response.sendRedirect(request.getContextPath() + "/books");
        } catch (SQLException exception) {
            errors.add(databaseMessage(exception, "Unable to add book."));
            request.setAttribute("errors", errors);
            showForm(request, response, book, "create");
        }
    }

    private void updateBook(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        List<String> errors = new ArrayList<>();
        Book book = readBook(request, true, errors);

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            showForm(request, response, book, "update");
            return;
        }

        try {
            if (bookDAO.updateBook(book)) {
                auditDAO.record(currentAdmin(request).getAdminId(), "BOOK_UPDATED", "book", Integer.toString(book.getBookId()), book.getTitle());
                setFlash(request, "success", "Book updated successfully.");
            } else {
                setFlash(request, "warning", "Book not found.");
            }
            response.sendRedirect(request.getContextPath() + "/books");
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
            request.setAttribute("errors", errors);
            showForm(request, response, book, "update");
        } catch (SQLException exception) {
            errors.add(databaseMessage(exception, "Unable to update book."));
            request.setAttribute("errors", errors);
            showForm(request, response, book, "update");
        }
    }

    private void deleteBook(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        List<String> errors = new ArrayList<>();
        int bookId = parseInteger(request.getParameter("bookId"), "Book ID", errors);

        if (!errors.isEmpty()) {
            setFlash(request, "danger", errors.get(0));
            response.sendRedirect(request.getContextPath() + "/books");
            return;
        }

        try {
            if (bookDAO.deleteBook(bookId)) {
                auditDAO.record(currentAdmin(request).getAdminId(), "BOOK_DELETED", "book", Integer.toString(bookId), "Catalog record removed");
                setFlash(request, "success", "Book deleted successfully.");
            } else {
                setFlash(request, "warning", "Book not found.");
            }
        } catch (SQLException exception) {
            setFlash(request, "danger", databaseMessage(exception, "Unable to delete book."));
        }

        response.sendRedirect(request.getContextPath() + "/books");
    }

    private Book readBook(HttpServletRequest request, boolean requireId, List<String> errors) {
        Book book = new Book();
        if (requireId) {
            book.setBookId(parseInteger(request.getParameter("bookId"), "Book ID", errors));
        }

        book.setTitle(value(request.getParameter("title")));
        book.setAuthor(value(request.getParameter("author")));
        book.setPublisher(value(request.getParameter("publisher")));
        book.setCategory(value(request.getParameter("category")));
        book.setIsbn(value(request.getParameter("isbn")));
        if (!isBlank(request.getParameter("publicationYear"))) {
            try { book.setPublicationYear(Integer.parseInt(request.getParameter("publicationYear"))); }
            catch (NumberFormatException exception) { errors.add("Publication year must be a valid year."); }
        }
        book.setTotalCopies(parseInteger(request.getParameter("totalCopies"), "Total copies", errors));
        // Available stock is derived from total stock minus active loans in the DAO.
        book.setAvailableCopies(book.getTotalCopies());
        book.setShelfLocation(value(request.getParameter("shelfLocation")));

        validateBook(book, errors);
        return book;
    }

    private void validateBook(Book book, List<String> errors) {
        if (isBlank(book.getTitle())) {
            errors.add("Title is required.");
        }
        if (isBlank(book.getAuthor())) {
            errors.add("Author is required.");
        }
        if (book.getTotalCopies() < 1) {
            errors.add("Total copies must be at least 1.");
        }
        if (book.getPublicationYear() < 0 || book.getPublicationYear() > java.time.LocalDate.now().getYear()) {
            errors.add("Publication year must be between 1 and the current year.");
        }
        if (book.getAvailableCopies() < 0) {
            errors.add("Available copies cannot be negative.");
        }
        if (book.getAvailableCopies() > book.getTotalCopies()) {
            errors.add("Available copies cannot be greater than total copies.");
        }
        if (book.getIsbn().length() > 20) {
            errors.add("ISBN must be 20 characters or fewer.");
        } else if (!IsbnValidator.isValidOrBlank(book.getIsbn())) {
            errors.add("ISBN must be a valid ISBN-10 or ISBN-13, or left blank.");
        }
        if (book.getTitle().length() > 150 || book.getAuthor().length() > 100
                || book.getPublisher().length() > 100 || book.getCategory().length() > 80
                || book.getShelfLocation().length() > 50) {
            errors.add("One or more book fields exceed the permitted length.");
        }
    }

    private String databaseMessage(SQLException exception, String fallback) {
        if ("23000".equals(exception.getSQLState())) {
            return fallback + " Check duplicate ISBN values or existing issue history.";
        }
        return fallback + " Review the submitted values and existing records, then try again.";
    }
}
