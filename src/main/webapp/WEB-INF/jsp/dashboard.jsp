<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Book" %>
<%@ page import="model.IssuedBook" %>
<%@ page import="util.HtmlUtil" %>
<%
    request.setAttribute("pageTitle", "Dashboard");
    Integer totalBooks = (Integer) request.getAttribute("totalBooks");
    Integer totalCopies = (Integer) request.getAttribute("totalCopies");
    Integer availableCopies = (Integer) request.getAttribute("availableCopies");
    Integer totalStudents = (Integer) request.getAttribute("totalStudents");
    Integer activeIssues = (Integer) request.getAttribute("activeIssues");
    Integer overdueLoans = (Integer) request.getAttribute("overdueLoans");
    BigDecimal outstandingFines = (BigDecimal) request.getAttribute("outstandingFines");
    List<Book> lowStockBooks = (List<Book>) request.getAttribute("lowStockBooks");
    List<IssuedBook> recentActivity = (List<IssuedBook>) request.getAttribute("recentActivity");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<div class="stats-grid">
    <article class="stat-card accent-teal"><div class="stat-icon"><i class="bi bi-journal-bookmark"></i></div><div><p>Book Titles</p><h2><%= totalBooks == null ? 0 : totalBooks %></h2></div></article>
    <article class="stat-card accent-green"><div class="stat-icon"><i class="bi bi-stack"></i></div><div><p>Physical Copies</p><h2><%= totalCopies == null ? 0 : totalCopies %></h2></div></article>
    <article class="stat-card accent-teal"><div class="stat-icon"><i class="bi bi-check2-circle"></i></div><div><p>Available Copies</p><h2><%= availableCopies == null ? 0 : availableCopies %></h2></div></article>
    <article class="stat-card accent-amber"><div class="stat-icon"><i class="bi bi-people"></i></div><div><p>Active Students</p><h2><%= totalStudents == null ? 0 : totalStudents %></h2></div></article>
    <article class="stat-card accent-rose"><div class="stat-icon"><i class="bi bi-clipboard-check"></i></div><div><p>Active Loans</p><h2><%= activeIssues == null ? 0 : activeIssues %></h2></div></article>
    <article class="stat-card accent-rose"><div class="stat-icon"><i class="bi bi-clock-history"></i></div><div><p>Overdue Loans</p><h2><%= overdueLoans == null ? 0 : overdueLoans %></h2></div></article>
    <article class="stat-card accent-amber"><div class="stat-icon"><i class="bi bi-cash-coin"></i></div><div><p>Outstanding Fines</p><h2><%= outstandingFines == null ? "0.00" : outstandingFines.toPlainString() %></h2></div></article>
</div>

<section class="panel mt-4">
    <div class="panel-header"><div><h2>Quick actions</h2><p>Common library tasks are one click away.</p></div></div>
    <div class="quick-actions">
        <a class="btn btn-primary" href="<%= contextPath %>/books?action=new"><i class="bi bi-plus-circle"></i> Add Book</a>
        <a class="btn btn-outline-primary" href="<%= contextPath %>/students?action=new"><i class="bi bi-person-plus"></i> Add Student</a>
        <a class="btn btn-success" href="<%= contextPath %>/issue-book"><i class="bi bi-box-arrow-up-right"></i> Issue Book</a>
        <a class="btn btn-outline-secondary" href="<%= contextPath %>/issued-books"><i class="bi bi-list-check"></i> Circulation</a>
        <a class="btn btn-outline-secondary" href="<%= contextPath %>/reports"><i class="bi bi-bar-chart"></i> Reports</a>
    </div>
</section>

<div class="row g-4 mt-1">
    <div class="col-xl-5">
        <section class="panel h-100">
            <div class="panel-header"><div><h2>Low stock</h2><p>Titles with two or fewer available copies.</p></div></div>
            <div class="table-responsive"><table class="table align-middle data-table">
                <thead><tr><th>Title</th><th>Author</th><th>Available</th></tr></thead><tbody>
                <% if (lowStockBooks == null || lowStockBooks.isEmpty()) { %>
                    <tr><td colspan="3"><div class="empty-state compact"><p>No low-stock titles.</p></div></td></tr>
                <% } else { for (Book book : lowStockBooks) { %>
                    <tr><td><%= HtmlUtil.escape(book.getTitle()) %></td><td><%= HtmlUtil.escape(book.getAuthor()) %></td><td><span class="badge <%= book.getAvailableCopies() == 0 ? "text-bg-danger" : "text-bg-warning" %>"><%= book.getAvailableCopies() %></span></td></tr>
                <% } } %>
                </tbody>
            </table></div>
        </section>
    </div>
    <div class="col-xl-7">
        <section class="panel h-100">
            <div class="panel-header"><div><h2>Recent circulation</h2><p>Latest issue and return records.</p></div><a href="<%= contextPath %>/issued-books">View all</a></div>
            <div class="table-responsive"><table class="table align-middle data-table">
                <thead><tr><th>Book</th><th>Student</th><th>Activity date</th><th>Status</th></tr></thead><tbody>
                <% if (recentActivity == null || recentActivity.isEmpty()) { %>
                    <tr><td colspan="4"><div class="empty-state compact"><p>No circulation activity yet.</p></div></td></tr>
                <% } else { for (IssuedBook loan : recentActivity) { %>
                    <tr><td><%= HtmlUtil.escape(loan.getBookTitle()) %></td><td><%= HtmlUtil.escape(loan.getStudentName()) %></td><td><%= loan.getReturnDate() == null ? loan.getIssueDate() : loan.getReturnDate() %></td><td><span class="badge <%= "ISSUED".equals(loan.getStatus()) ? "text-bg-warning" : "text-bg-success" %>"><%= HtmlUtil.escape(loan.getStatus()) %></span></td></tr>
                <% } } %>
                </tbody>
            </table></div>
        </section>
    </div>
</div>

<%@ include file="includes/footer.jsp" %>
