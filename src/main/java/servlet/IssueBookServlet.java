package servlet;

import dao.BookDAO;
import dao.IssuedBookDAO;
import dao.StudentDAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class IssueBookServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final BookDAO bookDAO = new BookDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final IssuedBookDAO issuedBookDAO = new IssuedBookDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        try {
            showIssueForm(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load issue book form.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");
        List<String> errors = new ArrayList<>();

        int bookId = parseInteger(request.getParameter("bookId"), "Book", errors);
        int studentId = parseInteger(request.getParameter("studentId"), "Student", errors);
        LocalDate issueDate = parseDate(request.getParameter("issueDate"), "Issue date", errors);
        LocalDate dueDate = parseDate(request.getParameter("dueDate"), "Due date", errors);

        if (bookId <= 0) {
            errors.add("Please select a valid book.");
        }
        if (studentId <= 0) {
            errors.add("Please select a valid student.");
        }
        if (issueDate != null && dueDate != null && dueDate.isBefore(issueDate)) {
            errors.add("Due date cannot be before issue date.");
        }

        request.setAttribute("selectedBookId", bookId);
        request.setAttribute("selectedStudentId", studentId);
        request.setAttribute("issueDate", issueDate == null ? LocalDate.now() : issueDate);
        request.setAttribute("dueDate", dueDate == null ? LocalDate.now().plusDays(14) : dueDate);

        try {
            if (!errors.isEmpty()) {
                request.setAttribute("errors", errors);
                forwardIssueForm(request, response);
                return;
            }

            issuedBookDAO.issueBook(bookId, studentId, issueDate, dueDate, currentAdmin(request).getAdminId());
            setFlash(request, "success", "Book issued successfully.");
            response.sendRedirect(request.getContextPath() + "/issued-books");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            errors.add(exception.getMessage());
            request.setAttribute("errors", errors);
            try {
                forwardIssueForm(request, response);
            } catch (SQLException sqlException) {
                throw new ServletException("Unable to reload issue book form.", sqlException);
            }
        } catch (SQLException exception) {
            throw new ServletException("Unable to issue book.", exception);
        }
    }

    private LocalDate parseDate(String rawDate, String fieldName, List<String> errors) {
        if (isBlank(rawDate)) {
            errors.add(fieldName + " is required.");
            return null;
        }

        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException exception) {
            errors.add(fieldName + " must be a valid date.");
            return null;
        }
    }

    private void showIssueForm(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        request.setAttribute("issueDate", LocalDate.now());
        request.setAttribute("dueDate", LocalDate.now().plusDays(issuedBookDAO.getLoanPeriodDays()));
        forwardIssueForm(request, response);
    }

    private void forwardIssueForm(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        if (request.getAttribute("dueDate") == null) {
            request.setAttribute("dueDate", LocalDate.now().plusDays(issuedBookDAO.getLoanPeriodDays()));
        }
        if (request.getAttribute("issueDate") == null) request.setAttribute("issueDate", LocalDate.now());
        request.setAttribute("books", bookDAO.getAvailableBooks());
        request.setAttribute("students", studentDAO.getActiveStudents());
        request.getRequestDispatcher("/WEB-INF/jsp/issue-book.jsp").forward(request, response);
    }
}
