package servlet;

import dao.IssuedBookDAO;
import model.IssuedBook;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IssuedBookServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final IssuedBookDAO issuedBookDAO = new IssuedBookDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        try {
            String keyword = value(request.getParameter("keyword"));
            String status = value(request.getParameter("status"));
            if (!java.util.Set.of("ISSUED", "RETURNED", "OVERDUE", "LOST", "DAMAGED").contains(status)) status = "";
            int pageSize = 20;
            int page = parsePage(request.getParameter("page"));
            int totalRows = issuedBookDAO.countSearchIssuedBooks(keyword, status);
            int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) pageSize));
            page = Math.min(page, totalPages);
            List<IssuedBook> issuedBooks = issuedBookDAO.searchIssuedBooks(keyword, status, page, pageSize);
            request.setAttribute("keyword", keyword);
            request.setAttribute("statusFilter", status);
            request.setAttribute("issuedBooks", issuedBooks);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.getRequestDispatcher("/WEB-INF/jsp/issued-books.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load issued books.", exception);
        }
    }

    private int parsePage(String raw) {
        try { return Math.max(1, Integer.parseInt(raw)); } catch (RuntimeException ignored) { return 1; }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureLoggedIn(request, response)) {
            return;
        }

        request.setCharacterEncoding("UTF-8");
        String action = value(request.getParameter("action"));

        if (!"return".equals(action)) {
            response.sendRedirect(request.getContextPath() + "/issued-books");
            return;
        }

        List<String> errors = new ArrayList<>();
        long issueId;
        try { issueId = Long.parseLong(request.getParameter("issueId")); if (issueId <= 0) throw new NumberFormatException(); }
        catch (RuntimeException exception) { errors.add("Issue ID must be a valid number."); issueId = -1; }
        if (!errors.isEmpty()) {
            setFlash(request, "danger", errors.get(0));
            response.sendRedirect(request.getContextPath() + "/issued-books");
            return;
        }

        try {
            BigDecimal fine = issuedBookDAO.returnBook(issueId, currentAdmin(request).getAdminId());
            setFlash(request, "success", "Book returned successfully. Fine: " + fine.toPlainString());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            setFlash(request, "danger", exception.getMessage());
        } catch (SQLException exception) {
            throw new ServletException("Unable to return book.", exception);
        }

        response.sendRedirect(request.getContextPath() + "/issued-books");
    }
}
