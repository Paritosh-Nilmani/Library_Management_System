package servlet;

import dao.BookDAO;
import dao.IssuedBookDAO;
import dao.StudentDAO;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

public class DashboardServlet extends BaseServlet {
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
            request.setAttribute("totalBooks", bookDAO.countBooks());
            request.setAttribute("totalCopies", bookDAO.countTotalCopies());
            request.setAttribute("availableCopies", bookDAO.countAvailableCopies());
            request.setAttribute("totalStudents", studentDAO.countActiveStudents());
            request.setAttribute("activeIssues", issuedBookDAO.countActiveIssuedBooks());
            request.setAttribute("overdueLoans", issuedBookDAO.countOverdueLoans());
            request.setAttribute("outstandingFines", issuedBookDAO.sumOutstandingFines());
            request.setAttribute("lowStockBooks", bookDAO.getLowStockBooks(8));
            request.setAttribute("recentActivity", issuedBookDAO.getRecentActivity(8));
            request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load dashboard.", exception);
        }
    }
}
