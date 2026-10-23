<%@ page import="java.util.List" %>
<%@ page import="model.FinePayment" %>
<%@ page import="util.HtmlUtil" %>
<%
    request.setAttribute("pageTitle", "Fine Payments");
    List<FinePayment> payments = (List<FinePayment>) request.getAttribute("payments");
    int currentPage = (Integer) request.getAttribute("currentPage");
    int totalPages = (Integer) request.getAttribute("totalPages");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel">
    <div class="panel-header">
        <div><h2>Payment history</h2><p>Auditable fine payments recorded against returned loans.</p></div>
        <a class="btn btn-outline-primary" href="<%= contextPath %>/payments?format=csv"><i class="bi bi-download"></i> Export CSV</a>
    </div>
    <div class="table-responsive">
        <table class="table align-middle data-table">
            <thead><tr><th>Payment</th><th>Loan</th><th>Student</th><th>Book</th><th>Amount</th><th>Method</th><th>Paid At</th><th>Recorded By</th><th>Note</th></tr></thead>
            <tbody>
            <% if (payments == null || payments.isEmpty()) { %>
                <tr><td colspan="9"><div class="empty-state compact"><i class="bi bi-cash-coin"></i><p>No fine payments recorded.</p></div></td></tr>
            <% } else { for (FinePayment payment : payments) { %>
                <tr>
                    <td><%= payment.getPaymentId() %></td><td>#<%= payment.getIssueId() %></td>
                    <td><%= HtmlUtil.escape(payment.getStudentName()) %></td><td><%= HtmlUtil.escape(payment.getBookTitle()) %></td>
                    <td><%= payment.getAmount().toPlainString() %></td><td><%= HtmlUtil.escape(payment.getMethod()) %></td>
                    <td><%= payment.getPaidAt() %></td><td><%= HtmlUtil.escape(payment.getRecordedBy()) %></td>
                    <td><%= HtmlUtil.escape(payment.getNote()) %></td>
                </tr>
            <% } } %>
            </tbody>
        </table>
    </div>
    <% if (totalPages > 1) { %>
        <nav class="mt-3" aria-label="Payment pages"><ul class="pagination justify-content-end mb-0">
            <li class="page-item <%= currentPage <= 1 ? "disabled" : "" %>"><a class="page-link" href="?page=<%= currentPage - 1 %>">Previous</a></li>
            <li class="page-item disabled"><span class="page-link">Page <%= currentPage %> of <%= totalPages %></span></li>
            <li class="page-item <%= currentPage >= totalPages ? "disabled" : "" %>"><a class="page-link" href="?page=<%= currentPage + 1 %>">Next</a></li>
        </ul></nav>
    <% } %>
</section>

<%@ include file="includes/footer.jsp" %>
