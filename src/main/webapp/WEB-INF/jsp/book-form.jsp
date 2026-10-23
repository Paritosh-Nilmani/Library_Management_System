<%@ page import="model.Book" %>
<%@ page import="util.HtmlUtil" %>
<%
    Book book = (Book) request.getAttribute("book");
    if (book == null) {
        book = new Book();
        book.setTotalCopies(1);
        book.setAvailableCopies(1);
    }
    String formAction = (String) request.getAttribute("formAction");
    boolean editing = "update".equals(formAction);
    request.setAttribute("pageTitle", editing ? "Update Book" : "Add Book");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel form-panel">
    <div class="panel-header">
        <div>
            <h2><%= editing ? "Update Book Details" : "Add New Book" %></h2>
            <p>Keep catalog data complete so issue and return workflows stay accurate.</p>
        </div>
    </div>

    <form action="<%= contextPath %>/books" method="post" class="row g-3 needs-validation" novalidate>
        <input type="hidden" name="csrfToken" value="${csrfToken}">
        <input type="hidden" name="action" value="<%= HtmlUtil.escape(formAction) %>">
        <% if (editing) { %>
            <input type="hidden" name="bookId" value="<%= book.getBookId() %>">
        <% } %>

        <div class="col-md-6">
            <label for="title" class="form-label">Title</label>
            <input type="text" class="form-control" id="title" name="title"
                   value="<%= HtmlUtil.escape(book.getTitle()) %>" maxlength="150" required>
            <div class="invalid-feedback">Title is required.</div>
        </div>

        <div class="col-md-6">
            <label for="author" class="form-label">Author</label>
            <input type="text" class="form-control" id="author" name="author"
                   value="<%= HtmlUtil.escape(book.getAuthor()) %>" maxlength="100" required>
            <div class="invalid-feedback">Author is required.</div>
        </div>

        <div class="col-md-4">
            <label for="publisher" class="form-label">Publisher</label>
            <input type="text" class="form-control" id="publisher" name="publisher"
                   value="<%= HtmlUtil.escape(book.getPublisher()) %>" maxlength="100">
        </div>

        <div class="col-md-4">
            <label for="category" class="form-label">Category</label>
            <input type="text" class="form-control" id="category" name="category"
                   value="<%= HtmlUtil.escape(book.getCategory()) %>" maxlength="80">
        </div>

        <div class="col-md-4">
            <label for="isbn" class="form-label">ISBN</label>
            <input type="text" class="form-control" id="isbn" name="isbn"
                   value="<%= HtmlUtil.escape(book.getIsbn()) %>" maxlength="20">
        </div>

        <div class="col-md-4">
            <label for="publicationYear" class="form-label">Publication Year</label>
            <input type="number" class="form-control" id="publicationYear" name="publicationYear"
                   value="<%= book.getPublicationYear() == 0 ? "" : book.getPublicationYear() %>" min="1" max="<%= java.time.LocalDate.now().getYear() %>">
        </div>

        <div class="col-md-4">
            <label for="totalCopies" class="form-label">Total Copies</label>
            <input type="number" class="form-control" id="totalCopies" name="totalCopies"
                   value="<%= book.getTotalCopies() %>" min="1" required>
            <div class="invalid-feedback">Total copies must be at least 1.</div>
        </div>

        <div class="col-md-4">
            <label for="availableCopies" class="form-label">Available Copies</label>
            <input type="number" class="form-control" id="availableCopies"
                   value="<%= book.getAvailableCopies() %>" min="0" readonly aria-describedby="availableHelp">
            <div class="form-text" id="availableHelp">Available copies are calculated from stock and active loans.</div>
        </div>

        <div class="col-md-4">
            <label for="shelfLocation" class="form-label">Shelf Location</label>
            <input type="text" class="form-control" id="shelfLocation" name="shelfLocation"
                   value="<%= HtmlUtil.escape(book.getShelfLocation()) %>" maxlength="50">
        </div>

        <div class="col-12 form-actions">
            <a class="btn btn-outline-secondary" href="<%= contextPath %>/books">
                <i class="bi bi-arrow-left"></i> Cancel
            </a>
            <button type="submit" class="btn btn-primary">
                <i class="bi bi-save"></i> <%= editing ? "Update Book" : "Save Book" %>
            </button>
        </div>
    </form>
</section>

<% if (!editing) { %>
<script>
    document.getElementById('totalCopies').addEventListener('input', event => {
        document.getElementById('availableCopies').value = event.target.value;
    });
</script>
<% } %>

<%@ include file="includes/footer.jsp" %>
