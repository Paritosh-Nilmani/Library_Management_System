<%@ page import="model.LibrarySettings" %>
<%
    request.setAttribute("pageTitle", "Library Settings");
    LibrarySettings settings = (LibrarySettings) request.getAttribute("settings");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel form-panel">
    <div class="panel-header"><div><h2>Circulation policy</h2><p>These values apply to new loans and fine assessments.</p></div></div>
    <form action="<%= contextPath %>/settings" method="post" class="row g-3 needs-validation" novalidate>
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <div class="col-md-6"><label for="maxActiveLoans" class="form-label">Maximum active loans per student</label><input class="form-control" type="number" id="maxActiveLoans" name="maxActiveLoans" min="1" max="50" value="<%= settings == null ? 5 : settings.getMaxActiveLoans() %>" required></div>
        <div class="col-md-6"><label for="loanPeriodDays" class="form-label">Loan period (days)</label><input class="form-control" type="number" id="loanPeriodDays" name="loanPeriodDays" min="1" max="365" value="<%= settings == null ? 14 : settings.getLoanPeriodDays() %>" required></div>
        <div class="col-md-6"><label for="dailyFine" class="form-label">Fine per overdue day</label><input class="form-control" type="number" id="dailyFine" name="dailyFine" min="0" step="0.01" value="<%= settings == null ? "5.00" : settings.getDailyFine() %>" required></div>
        <div class="col-md-6"><label for="maxFine" class="form-label">Maximum fine per loan</label><input class="form-control" type="number" id="maxFine" name="maxFine" min="0" step="0.01" value="<%= settings == null ? "500.00" : settings.getMaxFine() %>" required></div>
        <div class="col-12 form-actions"><button class="btn btn-primary" type="submit"><i class="bi bi-save"></i> Save policy</button></div>
    </form>
</section>

<%@ include file="includes/footer.jsp" %>
