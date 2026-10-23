<%@ page import="java.time.LocalDate" %>
<%@ page import="java.util.List" %>
<%@ page import="model.Book" %>
<%@ page import="model.Student" %>
<%@ page import="util.HtmlUtil" %>
<%
    request.setAttribute("pageTitle", "Issue Book");
    List<Book> books = (List<Book>) request.getAttribute("books");
    List<Student> students = (List<Student>) request.getAttribute("students");
    Integer selectedBookId = (Integer) request.getAttribute("selectedBookId");
    Integer selectedStudentId = (Integer) request.getAttribute("selectedStudentId");
    LocalDate issueDate = (LocalDate) request.getAttribute("issueDate");
    LocalDate dueDate = (LocalDate) request.getAttribute("dueDate");
    if (issueDate == null) {
        issueDate = LocalDate.now();
    }
    if (dueDate == null) {
        dueDate = LocalDate.now().plusDays(14);
    }
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel form-panel">
    <div class="panel-header">
        <div>
            <h2>Issue Book to Student</h2>
            <p>Only active students and books with available copies are shown here.</p>
        </div>
    </div>

    <form action="<%= contextPath %>/issue-book" method="post" class="row g-3 needs-validation" novalidate>
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <div class="col-md-6">
            <label for="bookId" class="form-label">Book</label>
            <select class="form-select" id="bookId" name="bookId" required>
                <option value="">Choose a book</option>
                <% if (books != null) { %>
                    <% for (Book book : books) { %>
                        <option value="<%= book.getBookId() %>"
                            <%= selectedBookId != null && selectedBookId == book.getBookId() ? "selected" : "" %>>
                            <%= HtmlUtil.escape(book.getTitle()) %> - <%= HtmlUtil.escape(book.getAuthor()) %>
                            (<%= book.getAvailableCopies() %> available)
                        </option>
                    <% } %>
                <% } %>
            </select>
            <div class="invalid-feedback">Please select a book.</div>
        </div>

        <div class="col-md-6">
            <label for="studentId" class="form-label">Student</label>
            <select class="form-select" id="studentId" name="studentId" required>
                <option value="">Choose a student</option>
                <% if (students != null) { %>
                    <% for (Student student : students) { %>
                        <option value="<%= student.getStudentId() %>"
                            <%= selectedStudentId != null && selectedStudentId == student.getStudentId() ? "selected" : "" %>>
                            <%= HtmlUtil.escape(student.getRollNo()) %> - <%= HtmlUtil.escape(student.getName()) %>
                        </option>
                    <% } %>
                <% } %>
            </select>
            <div class="invalid-feedback">Please select a student.</div>
        </div>

        <div class="col-md-6">
            <label for="issueDate" class="form-label">Issue Date</label>
            <input type="date" class="form-control" id="issueDate" name="issueDate"
                   value="<%= issueDate %>" required>
            <div class="invalid-feedback">Issue date is required.</div>
        </div>

        <div class="col-md-6">
            <label for="dueDate" class="form-label">Due Date</label>
            <input type="date" class="form-control" id="dueDate" name="dueDate"
                   value="<%= dueDate %>" required>
            <div class="invalid-feedback">Due date is required.</div>
        </div>

        <div class="col-12 form-actions">
            <a class="btn btn-outline-secondary" href="<%= contextPath %>/issued-books">
                <i class="bi bi-arrow-left"></i> Cancel
            </a>
            <button type="submit" class="btn btn-success">
                <i class="bi bi-box-arrow-up-right"></i> Issue Book
            </button>
        </div>
    </form>
</section>

<%@ include file="includes/footer.jsp" %>
