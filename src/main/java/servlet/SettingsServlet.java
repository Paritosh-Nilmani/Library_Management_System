package servlet;

import dao.LibrarySettingsDAO;
import model.LibrarySettings;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SettingsServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final LibrarySettingsDAO settingsDAO = new LibrarySettingsDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            request.setAttribute("settings", settingsDAO.getSettings());
            request.getRequestDispatcher("/WEB-INF/jsp/settings.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load library settings.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<String> errors = new ArrayList<>();
        LibrarySettings settings = new LibrarySettings();
        settings.setMaxActiveLoans(parseInt(request.getParameter("maxActiveLoans"), "Borrowing limit", errors));
        settings.setLoanPeriodDays(parseInt(request.getParameter("loanPeriodDays"), "Loan period", errors));
        settings.setDailyFine(parseMoney(request.getParameter("dailyFine"), "Daily fine", errors));
        settings.setMaxFine(parseMoney(request.getParameter("maxFine"), "Maximum fine", errors));
        if (settings.getMaxActiveLoans() < 1 || settings.getMaxActiveLoans() > 50) errors.add("Borrowing limit must be between 1 and 50.");
        if (settings.getLoanPeriodDays() < 1 || settings.getLoanPeriodDays() > 365) errors.add("Loan period must be between 1 and 365 days.");
        BigDecimal decimalMaximum = new BigDecimal("999999.99");
        if (settings.getDailyFine() != null && (settings.getDailyFine().signum() < 0 || settings.getDailyFine().compareTo(decimalMaximum) > 0)) errors.add("Daily fine must be between 0 and 999999.99.");
        if (settings.getMaxFine() != null && (settings.getMaxFine().signum() < 0 || settings.getMaxFine().compareTo(decimalMaximum) > 0)) errors.add("Maximum fine must be between 0 and 999999.99.");

        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("settings", settings);
            request.getRequestDispatcher("/WEB-INF/jsp/settings.jsp").forward(request, response);
            return;
        }
        try {
            settingsDAO.updateSettings(settings, currentAdmin(request).getAdminId());
            setFlash(request, "success", "Circulation and fine settings updated.");
            response.sendRedirect(request.getContextPath() + "/settings");
        } catch (SQLException exception) {
            throw new ServletException("Unable to save library settings.", exception);
        }
    }

    private int parseInt(String raw, String label, List<String> errors) {
        try { return Integer.parseInt(raw); }
        catch (RuntimeException exception) { errors.add(label + " must be a whole number."); return 0; }
    }

    private BigDecimal parseMoney(String raw, String label, List<String> errors) {
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.scale() > 2) throw new NumberFormatException();
            return value;
        } catch (RuntimeException exception) { errors.add(label + " must be a valid amount with up to two decimals."); return null; }
    }
}
