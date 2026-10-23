package servlet;

import dao.FinePaymentDAO;
import model.FinePayment;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class FinePaymentServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private final FinePaymentDAO paymentDAO = new FinePaymentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if ("csv".equalsIgnoreCase(request.getParameter("format"))) {
                writeCsv(response, paymentDAO.listAllPayments());
                return;
            }
            int pageSize = 25;
            int page = parsePage(request.getParameter("page"));
            int totalRows = paymentDAO.countPayments();
            int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) pageSize));
            page = Math.min(page, totalPages);
            List<FinePayment> payments = paymentDAO.listPayments(page, pageSize);
            request.setAttribute("payments", payments);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.getRequestDispatcher("/WEB-INF/jsp/payments.jsp").forward(request, response);
        } catch (SQLException exception) {
            throw new ServletException("Unable to load fine payment history.", exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            long issueId = Long.parseLong(request.getParameter("issueId"));
            BigDecimal amount = new BigDecimal(request.getParameter("amount"));
            String method = request.getParameter("method");
            String note = request.getParameter("note");
            if (note != null && note.length() > 255) throw new IllegalArgumentException("Payment note must be 255 characters or fewer.");
            paymentDAO.recordPayment(issueId, amount, method, note, currentAdmin(request).getAdminId());
            setFlash(request, "success", "Fine payment recorded.");
        } catch (NumberFormatException | NullPointerException exception) {
            setFlash(request, "danger", "Enter a valid loan ID and payment amount.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            setFlash(request, "danger", exception.getMessage());
        } catch (SQLException exception) {
            throw new ServletException("Unable to record fine payment.", exception);
        }
        response.sendRedirect(request.getContextPath() + "/issued-books");
    }

    private int parsePage(String raw) {
        try { return Math.max(1, Integer.parseInt(raw)); } catch (RuntimeException ignored) { return 1; }
    }

    private void writeCsv(HttpServletResponse response, List<FinePayment> payments) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=library-fine-payments.csv");
        var writer = response.getWriter();
        writer.write("Payment ID,Loan ID,Student,Book,Amount,Method,Paid At,Recorded By,Note\r\n");
        for (FinePayment payment : payments) {
            writer.write(csv(payment.getPaymentId()) + "," + csv(payment.getIssueId()) + ","
                    + csv(payment.getStudentName()) + "," + csv(payment.getBookTitle()) + ","
                    + csv(payment.getAmount().toPlainString()) + "," + csv(payment.getMethod()) + ","
                    + csv(payment.getPaidAt()) + "," + csv(payment.getRecordedBy()) + "," + csv(payment.getNote()) + "\r\n");
        }
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.stripLeading().matches("^[=+@-].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
