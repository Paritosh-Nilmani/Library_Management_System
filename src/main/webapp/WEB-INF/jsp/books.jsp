<%@ page import="java.util.List" %>
<%@ page import="model.Book" %>
<%@ page import="util.HtmlUtil" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%
    request.setAttribute("pageTitle", "Books");
    List<Book> books = (List<Book>) request.getAttribute("books");
    List<String> categories = (List<String>) request.getAttribute("categories");
    String keyword = (String) request.getAttribute("keyword");
    String selectedCategory = (String) request.getAttribute("category");
    String availability = (String) request.getAttribute("availability");
    String sort = (String) request.getAttribute("sort");
    int currentPage = (Integer) request.getAttribute("currentPage");
    int totalPages = (Integer) request.getAttribute("totalPages");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel">
    <div class="panel-header">
        <div>
            <h2>Book Catalog</h2>
            <p>Manage titles, copy counts, categories, and shelf locations.</p>
        </div>
        <a class="btn btn-primary" href="<%= contextPath %>/books?action=new">
            <i class="bi bi-plus-circle"></i> Add Book
        </a>
    </div>

    <form action="<%= contextPath %>/books" method="get" class="search-row">
        <div class="input-group">
            <span class="input-group-text"><i class="bi bi-search"></i></span>
            <input type="search" name="keyword" class="form-control"
                   placeholder="Search by title, author, publisher, category, or ISBN"
                   value="<%= HtmlUtil.escape(keyword) %>">
        </div>
        <select name="category" class="form-select" aria-label="Filter by category">
            <option value="">All categories</option>
            <% if (categories != null) for (String item : categories) { %>
                <option value="<%= HtmlUtil.escape(item) %>" <%= item.equals(selectedCategory) ? "selected" : "" %>><%= HtmlUtil.escape(item) %></option>
            <% } %>
        </select>
        <select name="availability" class="form-select" aria-label="Filter availability">
            <option value="" <%= availability.isEmpty() ? "selected" : "" %>>All stock</option>
            <option value="available" <%= "available".equals(availability) ? "selected" : "" %>>Available</option>
            <option value="out-of-stock" <%= "out-of-stock".equals(availability) ? "selected" : "" %>>Out of stock</option>
        </select>
        <select name="sort" class="form-select" aria-label="Sort books">
            <option value="title" <%= "title".equals(sort) ? "selected" : "" %>>Title</option>
            <option value="author" <%= "author".equals(sort) ? "selected" : "" %>>Author</option>
            <option value="year" <%= "year".equals(sort) ? "selected" : "" %>>Publication year</option>
            <option value="available" <%= "available".equals(sort) ? "selected" : "" %>>Available copies</option>
        </select>
        <button class="btn btn-outline-primary" type="submit">Search</button>
        <a class="btn btn-outline-secondary" href="<%= contextPath %>/books">Reset</a>
    </form>

    <div class="table-responsive mt-3">
        <table class="table align-middle data-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Title</th>
                    <th>Author</th>
                    <th>Category</th>
                    <th>ISBN</th>
                    <th>Year</th>
                    <th>Total</th>
                    <th>Available</th>
                    <th>Shelf</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <% if (books == null || books.isEmpty()) { %>
                    <tr>
                        <td colspan="10">
                            <div class="empty-state compact">
                                <i class="bi bi-journal-x"></i>
                                <p>No books found.</p>
                            </div>
                        </td>
                    </tr>
                <% } else { %>
                    <% for (Book book : books) { %>
                        <tr>
                            <td><%= book.getBookId() %></td>
                            <td>
                                <strong><%= HtmlUtil.escape(book.getTitle()) %></strong>
                                <div class="table-muted"><%= HtmlUtil.escape(book.getPublisher()) %></div>
                            </td>
                            <td><%= HtmlUtil.escape(book.getAuthor()) %></td>
                            <td><%= HtmlUtil.escape(book.getCategory()) %></td>
                            <td><%= HtmlUtil.escape(book.getIsbn()) %></td>
                            <td><%= book.getPublicationYear() == 0 ? "-" : book.getPublicationYear() %></td>
                            <td><%= book.getTotalCopies() %></td>
                            <td>
                                <span class="badge <%= book.getAvailableCopies() > 0 ? "text-bg-success" : "text-bg-danger" %>">
                                    <%= book.getAvailableCopies() %>
                                </span>
                            </td>
                            <td><%= HtmlUtil.escape(book.getShelfLocation()) %></td>
                            <td class="text-end action-cell">
                                <a class="btn btn-sm btn-outline-secondary" href="<%= contextPath %>/books?action=view&amp;id=<%= book.getBookId() %>">
                                    <i class="bi bi-eye"></i> View
                                </a>
                                <a class="btn btn-sm btn-outline-primary"
                                   href="<%= contextPath %>/books?action=edit&id=<%= book.getBookId() %>">
                                    <i class="bi bi-pencil-square"></i> Edit
                                </a>
                                <form action="<%= contextPath %>/books" method="post" class="d-inline"
                                      onsubmit="return confirm('Delete this book?');">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="bookId" value="<%= book.getBookId() %>">
                                    <button type="submit" class="btn btn-sm btn-outline-danger">
                                        <i class="bi bi-trash"></i> Delete
                                    </button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                <% } %>
            </tbody>
        </table>
    </div>
    <% if (totalPages > 1) {
        String encodedKeyword = URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8.name());
        String encodedCategory = URLEncoder.encode(selectedCategory == null ? "" : selectedCategory, StandardCharsets.UTF_8.name());
        String encodedAvailability = URLEncoder.encode(availability == null ? "" : availability, StandardCharsets.UTF_8.name());
        String encodedSort = URLEncoder.encode(sort == null ? "title" : sort, StandardCharsets.UTF_8.name()); %>
        <nav class="mt-3" aria-label="Book pages">
            <ul class="pagination justify-content-end mb-0">
                <li class="page-item <%= currentPage <= 1 ? "disabled" : "" %>"><a class="page-link" href="?keyword=<%= encodedKeyword %>&amp;category=<%= encodedCategory %>&amp;availability=<%= encodedAvailability %>&amp;sort=<%= encodedSort %>&amp;page=<%= currentPage - 1 %>">Previous</a></li>
                <li class="page-item disabled"><span class="page-link">Page <%= currentPage %> of <%= totalPages %></span></li>
                <li class="page-item <%= currentPage >= totalPages ? "disabled" : "" %>"><a class="page-link" href="?keyword=<%= encodedKeyword %>&amp;category=<%= encodedCategory %>&amp;availability=<%= encodedAvailability %>&amp;sort=<%= encodedSort %>&amp;page=<%= currentPage + 1 %>">Next</a></li>
            </ul>
        </nav>
    <% } %>
</section>

<%@ include file="includes/footer.jsp" %>
