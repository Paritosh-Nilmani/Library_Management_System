<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="model.ReportRow" %>
<%@ page import="util.HtmlUtil" %>
<%
    request.setAttribute("pageTitle", "Reports");
    LocalDate from = (LocalDate) request.getAttribute("from");
    LocalDate to = (LocalDate) request.getAttribute("to");
    List<ReportRow> mostBorrowed = (List<ReportRow>) request.getAttribute("mostBorrowed");
    List<ReportRow> activeBorrowers = (List<ReportRow>) request.getAttribute("activeBorrowers");
    List<ReportRow> overdueLoans = (List<ReportRow>) request.getAttribute("overdueLoans");
    List<ReportRow> inventory = (List<ReportRow>) request.getAttribute("inventory");
    List<ReportRow> monthlyTrends = (List<ReportRow>) request.getAttribute("monthlyTrends");
    BigDecimal fineCollection = (BigDecimal) request.getAttribute("fineCollection");
    BigDecimal outstandingFines = (BigDecimal) request.getAttribute("outstandingFines");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel mb-4">
    <div class="panel-header"><div><h2>Circulation and finance</h2><p>Database-backed summaries for the selected period.</p></div></div>
    <form action="<%= contextPath %>/reports" method="get" class="search-row">
        <div><label for="from" class="form-label">From</label><input class="form-control" type="date" id="from" name="from" value="<%= from %>" required></div>
        <div><label for="to" class="form-label">To</label><input class="form-control" type="date" id="to" name="to" value="<%= to %>" required></div>
        <button class="btn btn-primary align-self-end" type="submit">Apply range</button>
    </form>
    <div class="quick-actions mt-3">
        <a class="btn btn-outline-primary" href="<%= contextPath %>/reports?format=csv&amp;type=most-borrowed&amp;from=<%= from %>&amp;to=<%= to %>">Most borrowed CSV</a>
        <a class="btn btn-outline-primary" href="<%= contextPath %>/reports?format=csv&amp;type=borrowers&amp;from=<%= from %>&amp;to=<%= to %>">Borrowers CSV</a>
        <a class="btn btn-outline-primary" href="<%= contextPath %>/reports?format=csv&amp;type=overdue&amp;from=<%= from %>&amp;to=<%= to %>">Overdue CSV</a>
        <a class="btn btn-outline-primary" href="<%= contextPath %>/reports?format=csv&amp;type=inventory&amp;from=<%= from %>&amp;to=<%= to %>">Inventory CSV</a>
        <a class="btn btn-outline-primary" href="<%= contextPath %>/reports?format=csv&amp;type=monthly&amp;from=<%= from %>&amp;to=<%= to %>">Trends CSV</a>
    </div>
</section>

<div class="stats-grid mb-4">
    <article class="stat-card accent-green"><div><p>Fine payments collected in range</p><h2><%= fineCollection == null ? "0.00" : fineCollection.toPlainString() %></h2></div></article>
    <article class="stat-card accent-amber"><div><p>Outstanding assessed fines</p><h2><%= outstandingFines == null ? "0.00" : outstandingFines.toPlainString() %></h2></div></article>
</div>

<div class="row g-4">
    <div class="col-lg-6"><section class="panel h-100"><div class="panel-header"><div><h2>Most borrowed books</h2><p>Issues from <%= from %> through <%= to %>.</p></div></div><%@ include file="includes/report-table.jsp" %></section></div>
    <div class="col-lg-6"><section class="panel h-100"><div class="panel-header"><div><h2>Most active borrowers</h2><p>Issues from <%= from %> through <%= to %>.</p></div></div><%@ include file="includes/report-borrowers.jsp" %></section></div>
    <div class="col-lg-6"><section class="panel h-100"><div class="panel-header"><div><h2>Overdue loans</h2><p>Currently overdue active loans and days overdue.</p></div></div><%@ include file="includes/report-overdue.jsp" %></section></div>
    <div class="col-lg-6"><section class="panel h-100"><div class="panel-header"><div><h2>Inventory by category</h2><p>Title copies currently in the catalog.</p></div></div><%@ include file="includes/report-inventory.jsp" %></section></div>
    <div class="col-12"><section class="panel"><div class="panel-header"><div><h2>Monthly circulation</h2><p>Issues and returns from <%= from %> through <%= to %>.</p></div></div><%@ include file="includes/report-monthly.jsp" %></section></div>
</div>

<%@ include file="includes/footer.jsp" %>
