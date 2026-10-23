package servlet;

import dao.AdminDAO;
import model.Admin;
import util.LoginAttemptLimiter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class LoginServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("admin") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = value(request.getParameter("username"));
        String password = request.getParameter("password"); // Passwords are not trimmed or stored in request/session state.
        String attemptKey = username.toLowerCase(Locale.ROOT);

        if (isBlank(username) || password == null || password.isEmpty() || username.length() > 50
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            showError(request, response, username);
            return;
        }
        if (LoginAttemptLimiter.isLocked(attemptKey)) {
            showError(request, response, username);
            return;
        }

        try {
            Admin admin = adminDAO.validateAdmin(username, password);
            if (admin == null) {
                LoginAttemptLimiter.recordFailure(attemptKey);
                showError(request, response, username);
                return;
            }

            LoginAttemptLimiter.recordSuccess(attemptKey);
            HttpSession session = request.getSession(false);
            if (session == null) session = request.getSession(true);
            request.changeSessionId();
            session.removeAttribute("csrfToken"); // Rotate CSRF token after authentication.
            session.setAttribute("admin", admin);
            session.setMaxInactiveInterval(30 * 60);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException exception) {
            throw new ServletException("Unable to validate admin login.", exception);
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String username)
            throws ServletException, IOException {
        request.setAttribute("error", "Invalid username or password, or sign-in is temporarily unavailable.");
        request.setAttribute("username", username);
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }
}
