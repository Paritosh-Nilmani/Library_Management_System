<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="model.Student" %>
<%@ page import="model.IssuedBook" %>
<%@ page import="util.HtmlUtil" %>
<%
    request.setAttribute("pageTitle", "Student Profile");
    Student student = (Student) request.getAttribute("student");
    List<IssuedBook> loans = (List<IssuedBook>) request.getAttribute("loans");
    int activeCount = 0;
    int overdueCount = 0;
    BigDecimal assessed = BigDecimal.ZERO;
    BigDecimal paid = BigDecimal.ZERO;
    if (loans != null) for (IssuedBook loan : loans) {
        if ("ISSUED".equals(loan.getStatus())) {
            activeCount++;
            if (loan.getDueDate().isBefore(LocalDate.now())) overdueCount++;
        }
        assessed = assessed.add(loan.getFine() == null ? BigDecimal.ZERO : loan.getFine());
        paid = paid.add(loan.getPaidFine());
    }
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel mb-4">
    <div class="panel-header"><div><h2><%= HtmlUtil.escape(student.getName()) %></h2><p>Registration <strong><%= HtmlUtil.escape(student.getRollNo()) %></strong> · <%= HtmlUtil.escape(student.getStatus()) %></p></div><a class="btn btn-outline-primary" href="<%= contextPath %>/students?action=edit&amp;id=<%= student.getStudentId() %>">Edit student</a></div>
    <div class="row g-3">
        <div class="col-md-4"><strong>Email</strong><div><%= HtmlUtil.escape(student.getEmail()) %></div></div>
        <div class="col-md-4"><strong>Phone</strong><div><%= HtmlUtil.escape(student.getPhone()) %></div></div>
        <div class="col-md-4"><strong>Department / Course</strong><div><%= HtmlUtil.escape(student.getDepartment()) %> / <%= HtmlUtil.escape(student.getCourse()) %></div></div>
        <div class="col-md-4"><strong>Enrollment Date</strong><div><%= student.getEnrollmentDate() == null ? "-" : student.getEnrollmentDate() %></div></div>
        <div class="col-md-4"><strong>Active / Overdue Loans</strong><div><%= activeCount %> / <%= overdueCount %></div></div>
        <div class="col-md-4"><strong>Assessed / Paid / Balance</strong><div><%= assessed.toPlainString() %> / <%= paid.toPlainString() %> / <%= assessed.subtract(paid).max(BigDecimal.ZERO).toPlainString() %></div></div>
    </div>
</section>

<section class="panel">
    <div class="panel-header"><div><h2>Circulation history</h2><p>Most recent 200 loans.</p></div></div>
    <div class="table-responsive"><table class="table align-middle data-table">
        <thead><tr><th>Loan</th><th>Book</th><th>Issue</th><th>Due</th><th>Return</th><th>Status</th><th>Assessed</th><th>Paid</th></tr></thead>
        <tbody>
        <% if (loans == null || loans.isEmpty()) { %><tr><td colspan="8"><div class="empty-state compact"><p>No circulation history.</p></div></td></tr>
        <% } else { for (IssuedBook loan : loans) { %>
            <tr><td>#<%= loan.getIssueId() %></td><td><%= HtmlUtil.escape(loan.getBookTitle()) %></td><td><%= loan.getIssueDate() %></td><td><%= loan.getDueDate() %></td><td><%= loan.getReturnDate() == null ? "-" : loan.getReturnDate() %></td><td><%= HtmlUtil.escape(loan.getStatus()) %></td><td><%= loan.getFine() == null ? "0.00" : loan.getFine().toPlainString() %></td><td><%= loan.getPaidFine().toPlainString() %></td></tr>
        <% } } %>
        </tbody>
    </table></div>
</section>

<%@ include file="includes/footer.jsp" %>
