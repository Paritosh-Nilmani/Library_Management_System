package servlet;

import dao.ReportDAO;
import model.ReportRow;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;

public class ReportsServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final ReportDAO reportDAO = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LocalDate today = LocalDate.now();
        List<String> errors = new ArrayList<>();
        LocalDate from = parseDate(request.getParameter("from"), today.minusDays(30), "Start date", errors);
        LocalDate to = parseDate(request.getParameter("to"), today, "End date", errors);
        if (from.isAfter(to)) {
            errors.add("Start date must be on or before end date.");
            from = today.minusDays(30);
            to = today;
        }
        if (!errors.isEmpty()) request.setAttribute("errors", errors);
        try {
            if ("csv".equalsIgnoreCase(request.getParameter("format"))) {
                if (!errors.isEmpty()) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }
                String type = value(request.getParameter("type"));
                try { writeCsv(response, type, rows(type, from, to)); }
                catch (IllegalArgumentException exception) { response.sendError(HttpServletResponse.SC_BAD_REQUEST); }
                return;
            }
            request.setAttribute("from", from);
            request.setAttribute("to", to);
            request.setAttribute("mostBorrowed", reportDAO.mostBorrowed(from, to));
            request.setAttribute("activeBorrowers", reportDAO.activeBorrowers(from, to));
            request.setAttribute("overdueLoans", reportDAO.overdueLoans());
            request.setAttribute("inventory", reportDAO.inventory());
            request.setAttribute("monthlyTrends", reportDAO.monthlyTrends(from, to));
            request.setAttribute("fineCollection", reportDAO.fineCollection(from, to));
            request.setAttribute("outstandingFines", reportDAO.outstandingFines());
            request.getRequestDispatcher("/WEB-INF/jsp/reports.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to build reports.", exception);
        }
    }

    private LocalDate parseDate(String raw, LocalDate fallback, String field, List<String> errors) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return LocalDate.parse(raw); }
        catch (DateTimeParseException exception) { errors.add(field + " must be a valid date."); return fallback; }
    }

    private List<ReportRow> rows(String type, LocalDate from, LocalDate to) throws SQLException {
        switch (type) {
            case "most-borrowed": return reportDAO.mostBorrowed(from, to);
            case "borrowers": return reportDAO.activeBorrowers(from, to);
            case "overdue": return reportDAO.overdueLoans();
            case "inventory": return reportDAO.inventory();
            case "monthly": return reportDAO.monthlyTrends(from, to);
            default: throw new IllegalArgumentException("Select a supported report for CSV export.");
        }
    }

    private void writeCsv(HttpServletResponse response, String type, List<ReportRow> rows) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=library-" + type + "-report.csv");
        var writer = response.getWriter();
        writer.write("Name,Details,Count,Secondary Count,Amount\r\n");
        for (ReportRow row : rows) {
            writer.write(csv(row.getLabel()) + "," + csv(row.getDetail()) + "," + row.getQuantity() + ","
                    + row.getSecondaryQuantity() + "," + csv(row.getAmount().toPlainString()) + "\r\n");
        }
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.stripLeading().matches("^[=+@-].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
