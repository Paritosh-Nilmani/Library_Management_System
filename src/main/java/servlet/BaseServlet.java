package servlet;

import model.Admin;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public abstract class BaseServlet extends HttpServlet {
    protected boolean ensureLoggedIn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Admin admin = session == null ? null : (Admin) session.getAttribute("admin");

        if (admin == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        return true;
    }

    protected Admin currentAdmin(HttpServletRequest request) {
        return (Admin) request.getSession(false).getAttribute("admin");
    }

    protected void setFlash(HttpServletRequest request, String type, String message) {
        HttpSession session = request.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

    protected String value(String text) {
        return text == null ? "" : text.trim();
    }

    protected boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    protected int parseInteger(String rawValue, String fieldName, List<String> errors) {
        if (isBlank(rawValue)) {
            errors.add(fieldName + " is required.");
            return -1;
        }

        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException exception) {
            errors.add(fieldName + " must be a valid number.");
            return -1;
        }
    }
}
