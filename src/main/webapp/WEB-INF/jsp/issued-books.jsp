<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="model.IssuedBook" %>
<%@ page import="util.HtmlUtil" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%
    request.setAttribute("pageTitle", "Issued Books");
    List<IssuedBook> issuedBooks = (List<IssuedBook>) request.getAttribute("issuedBooks");
    String keyword = (String) request.getAttribute("keyword");
    String statusFilter = (String) request.getAttribute("statusFilter");
    int currentPage = (Integer) request.getAttribute("currentPage");
    int totalPages = (Integer) request.getAttribute("totalPages");
    LocalDate today = LocalDate.now();
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel">
    <div class="panel-header">
        <div>
            <h2>Issued Book Records</h2>
            <p>Track active loans, returned books, due dates, and fines.</p>
        </div>
        <a class="btn btn-success" href="<%= contextPath %>/issue-book">
            <i class="bi bi-box-arrow-up-right"></i> Issue Book
        </a>
    </div>

    <form action="<%= contextPath %>/issued-books" method="get" class="search-row">
        <div class="input-group">
            <span class="input-group-text"><i class="bi bi-search"></i></span>
            <input type="search" name="keyword" class="form-control"
                   placeholder="Search by book, ISBN, student, roll number, or status"
                   value="<%= HtmlUtil.escape(keyword) %>">
        </div>
        <select name="status" class="form-select" aria-label="Filter loan status">
            <option value="" <%= statusFilter.isEmpty() ? "selected" : "" %>>All statuses</option>
            <option value="ISSUED" <%= "ISSUED".equals(statusFilter) ? "selected" : "" %>>Active</option>
            <option value="OVERDUE" <%= "OVERDUE".equals(statusFilter) ? "selected" : "" %>>Overdue</option>
            <option value="RETURNED" <%= "RETURNED".equals(statusFilter) ? "selected" : "" %>>Returned</option>
            <option value="LOST" <%= "LOST".equals(statusFilter) ? "selected" : "" %>>Lost</option>
            <option value="DAMAGED" <%= "DAMAGED".equals(statusFilter) ? "selected" : "" %>>Damaged</option>
        </select>
        <button class="btn btn-outline-primary" type="submit">Search</button>
        <a class="btn btn-outline-secondary" href="<%= contextPath %>/issued-books">Reset</a>
    </form>

    <div class="table-responsive mt-3">
        <table class="table align-middle data-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Book</th>
                    <th>Student</th>
                    <th>Issue Date</th>
                    <th>Due Date</th>
                    <th>Return Date</th>
                    <th>Status</th>
                    <th>Fine / Balance</th>
                    <th class="text-end">Action</th>
                </tr>
            </thead>
            <tbody>
                <% if (issuedBooks == null || issuedBooks.isEmpty()) { %>
                    <tr>
                        <td colspan="9">
                            <div class="empty-state compact">
                                <i class="bi bi-clipboard-x"></i>
                                <p>No issued book records found.</p>
                            </div>
                        </td>
                    </tr>
                <% } else { %>
                    <% for (IssuedBook issuedBook : issuedBooks) {
                        boolean active = "ISSUED".equals(issuedBook.getStatus());
                        boolean overdue = active && issuedBook.getDueDate() != null && issuedBook.getDueDate().isBefore(today);
                    %>
                        <tr>
                            <td><%= issuedBook.getIssueId() %></td>
                            <td>
                                <strong><%= HtmlUtil.escape(issuedBook.getBookTitle()) %></strong>
                                <div class="table-muted">ISBN: <%= HtmlUtil.escape(issuedBook.getBookIsbn()) %></div>
                            </td>
                            <td>
                                <strong><%= HtmlUtil.escape(issuedBook.getStudentName()) %></strong>
                                <div class="table-muted"><%= HtmlUtil.escape(issuedBook.getStudentRollNo()) %></div>
                            </td>
                            <td><%= issuedBook.getIssueDate() %></td>
                            <td>
                                <%= issuedBook.getDueDate() %>
                                <% if (overdue) { %>
                                    <span class="badge text-bg-danger ms-1">Overdue</span>
                                <% } %>
                            </td>
                            <td><%= issuedBook.getReturnDate() == null ? "-" : issuedBook.getReturnDate() %></td>
                            <td>
                                <span class="badge <%= active ? "text-bg-warning" : "text-bg-success" %>">
                                    <%= HtmlUtil.escape(issuedBook.getStatus()) %>
                                </span>
                            </td>
                            <td>
                                <div>Assessed: <%= issuedBook.getFine() == null ? "0.00" : issuedBook.getFine().toPlainString() %></div>
                                <div class="table-muted">Paid: <%= issuedBook.getPaidFine().toPlainString() %> · Due: <%= issuedBook.getOutstandingFine().toPlainString() %></div>
                                <% if (!active && issuedBook.getOutstandingFine().signum() > 0) { %>
                                    <form action="<%= contextPath %>/payments" method="post" class="mt-2">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="issueId" value="<%= issuedBook.getIssueId() %>">
                                        <div class="input-group input-group-sm">
                                            <input class="form-control" type="number" name="amount" min="0.01" max="<%= issuedBook.getOutstandingFine().toPlainString() %>" step="0.01" placeholder="Amount" required aria-label="Payment amount">
                                            <select class="form-select" name="method" aria-label="Payment method">
                                                <option value="CASH">Cash</option><option value="CARD">Card</option><option value="BANK_TRANSFER">Bank transfer</option><option value="OTHER">Other</option>
                                            </select>
                                            <button class="btn btn-outline-success" type="submit">Pay</button>
                                        </div>
                                    </form>
                                <% } %>
                            </td>
                            <td class="text-end action-cell">
                                <% if (active) { %>
                                    <form action="<%= contextPath %>/issued-books" method="post" class="d-inline"
                                          onsubmit="return confirm('Mark this book as returned?');">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="action" value="return">
                                        <input type="hidden" name="issueId" value="<%= issuedBook.getIssueId() %>">
                                        <button type="submit" class="btn btn-sm btn-outline-success">
                                            <i class="bi bi-arrow-return-left"></i> Return
                                        </button>
                                    </form>
                                <% } else { %>
                                    <span class="table-muted">Returned</span>
                                <% } %>
                            </td>
                        </tr>
                    <% } %>
                <% } %>
            </tbody>
        </table>
    </div>
    <% if (totalPages > 1) { String encodedKeyword = URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8.name()); String encodedStatus = URLEncoder.encode(statusFilter == null ? "" : statusFilter, StandardCharsets.UTF_8.name()); %>
        <nav class="mt-3" aria-label="Circulation pages"><ul class="pagination justify-content-end mb-0">
            <li class="page-item <%= currentPage <= 1 ? "disabled" : "" %>"><a class="page-link" href="?keyword=<%= encodedKeyword %>&amp;status=<%= encodedStatus %>&amp;page=<%= currentPage - 1 %>">Previous</a></li>
            <li class="page-item disabled"><span class="page-link">Page <%= currentPage %> of <%= totalPages %></span></li>
            <li class="page-item <%= currentPage >= totalPages ? "disabled" : "" %>"><a class="page-link" href="?keyword=<%= encodedKeyword %>&amp;status=<%= encodedStatus %>&amp;page=<%= currentPage + 1 %>">Next</a></li>
        </ul></nav>
    <% } %>
</section>

<%@ include file="includes/footer.jsp" %>
