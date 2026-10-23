<%@ page import="java.util.List" %>
<%@ page import="model.Book" %>
<%@ page import="model.IssuedBook" %>
<%@ page import="util.HtmlUtil" %>
<%
    request.setAttribute("pageTitle", "Book Details");
    Book book = (Book) request.getAttribute("book");
    List<IssuedBook> loans = (List<IssuedBook>) request.getAttribute("loans");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel mb-4">
    <div class="panel-header"><div><h2><%= HtmlUtil.escape(book.getTitle()) %></h2><p>Book ID #<%= book.getBookId() %></p></div><a class="btn btn-outline-primary" href="<%= contextPath %>/books?action=edit&amp;id=<%= book.getBookId() %>">Edit book</a></div>
    <div class="row g-3">
        <div class="col-md-4"><strong>Author</strong><div><%= HtmlUtil.escape(book.getAuthor()) %></div></div>
        <div class="col-md-4"><strong>Publisher</strong><div><%= HtmlUtil.escape(book.getPublisher()) %></div></div>
        <div class="col-md-4"><strong>Category</strong><div><%= HtmlUtil.escape(book.getCategory()) %></div></div>
        <div class="col-md-4"><strong>ISBN</strong><div><%= HtmlUtil.escape(book.getIsbn()) %></div></div>
        <div class="col-md-4"><strong>Publication year</strong><div><%= book.getPublicationYear() == 0 ? "-" : book.getPublicationYear() %></div></div>
        <div class="col-md-4"><strong>Shelf</strong><div><%= HtmlUtil.escape(book.getShelfLocation()) %></div></div>
        <div class="col-md-4"><strong>Total copies</strong><div><%= book.getTotalCopies() %></div></div>
        <div class="col-md-4"><strong>Available copies</strong><div><%= book.getAvailableCopies() %></div></div>
        <div class="col-md-4"><strong>Issued copies</strong><div><%= book.getTotalCopies() - book.getAvailableCopies() %></div></div>
    </div>
</section>

<section class="panel">
    <div class="panel-header"><div><h2>Circulation history</h2><p>Most recent 100 loans for this title.</p></div></div>
    <div class="table-responsive"><table class="table align-middle data-table">
        <thead><tr><th>Loan</th><th>Student</th><th>Issue</th><th>Due</th><th>Return</th><th>Status</th><th>Fine</th></tr></thead>
        <tbody>
        <% if (loans == null || loans.isEmpty()) { %><tr><td colspan="7"><div class="empty-state compact"><p>No circulation history.</p></div></td></tr>
        <% } else { for (IssuedBook loan : loans) { %>
            <tr><td>#<%= loan.getIssueId() %></td><td><%= HtmlUtil.escape(loan.getStudentName()) %> · <%= HtmlUtil.escape(loan.getStudentRollNo()) %></td><td><%= loan.getIssueDate() %></td><td><%= loan.getDueDate() %></td><td><%= loan.getReturnDate() == null ? "-" : loan.getReturnDate() %></td><td><%= HtmlUtil.escape(loan.getStatus()) %></td><td><%= loan.getFine() == null ? "0.00" : loan.getFine().toPlainString() %></td></tr>
        <% } } %>
        </tbody>
    </table></div>
</section>

<%@ include file="includes/footer.jsp" %>
