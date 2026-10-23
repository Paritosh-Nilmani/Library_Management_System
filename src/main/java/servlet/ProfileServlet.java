package servlet;

import dao.AdminDAO;
import model.Admin;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProfileServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("admin", currentAdmin(request));
        request.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String current = request.getParameter("currentPassword");
        String next = request.getParameter("newPassword");
        String confirm = request.getParameter("confirmPassword");
        List<String> errors = new ArrayList<>();
        int passwordBytes = next == null ? 0 : next.getBytes(StandardCharsets.UTF_8).length;
        if (current == null || current.isEmpty()) errors.add("Current password is required.");
        if (passwordBytes < 12 || passwordBytes > 72) errors.add("New password must be 12 to 72 UTF-8 bytes.");
        if (next == null || !next.equals(confirm)) errors.add("New password and confirmation must match.");

        try {
            Admin admin = currentAdmin(request);
            if (errors.isEmpty() && !adminDAO.updatePassword(admin.getAdminId(), current, next)) {
                errors.add("Current password was not accepted.");
            }
            if (!errors.isEmpty()) {
                request.setAttribute("errors", errors);
                request.setAttribute("admin", admin);
                request.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(request, response);
                return;
            }
            request.changeSessionId();
            request.getSession(false).removeAttribute("csrfToken");
            setFlash(request, "success", "Password updated successfully.");
            response.sendRedirect(request.getContextPath() + "/profile");
        } catch (SQLException exception) {
            throw new ServletException("Unable to update password.", exception);
        }
    }
}
