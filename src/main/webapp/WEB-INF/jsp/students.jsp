<%@ page import="java.util.List" %>
<%@ page import="model.Student" %>
<%@ page import="util.HtmlUtil" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%
    request.setAttribute("pageTitle", "Students");
    List<Student> students = (List<Student>) request.getAttribute("students");
    String keyword = (String) request.getAttribute("keyword");
    int currentPage = (Integer) request.getAttribute("currentPage");
    int totalPages = (Integer) request.getAttribute("totalPages");
%>
<%@ include file="includes/header.jsp" %>
<%@ include file="includes/sidebar.jsp" %>
<%@ include file="includes/alerts.jsp" %>

<section class="panel">
    <div class="panel-header">
        <div>
            <h2>Student Management</h2>
            <p>Create and maintain student records used by the issue workflow.</p>
        </div>
        <a class="btn btn-primary" href="<%= contextPath %>/students?action=new">
            <i class="bi bi-person-plus"></i> Add Student
        </a>
    </div>

    <form action="<%= contextPath %>/students" method="get" class="search-row">
        <div class="input-group">
            <span class="input-group-text"><i class="bi bi-search"></i></span>
            <input type="search" name="keyword" class="form-control"
                   placeholder="Search by roll number, name, email, phone, or department"
                   value="<%= HtmlUtil.escape(keyword) %>">
        </div>
        <button class="btn btn-outline-primary" type="submit">Search</button>
        <a class="btn btn-outline-secondary" href="<%= contextPath %>/students">Reset</a>
    </form>

    <div class="table-responsive mt-3">
        <table class="table align-middle data-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Roll No</th>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Department</th>
                    <th>Status</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <% if (students == null || students.isEmpty()) { %>
                    <tr>
                        <td colspan="8">
                            <div class="empty-state compact">
                                <i class="bi bi-person-x"></i>
                                <p>No students found.</p>
                            </div>
                        </td>
                    </tr>
                <% } else { %>
                    <% for (Student student : students) { %>
                        <tr>
                            <td><%= student.getStudentId() %></td>
                            <td><strong><%= HtmlUtil.escape(student.getRollNo()) %></strong></td>
                            <td><%= HtmlUtil.escape(student.getName()) %></td>
                            <td><%= HtmlUtil.escape(student.getEmail()) %></td>
                            <td><%= HtmlUtil.escape(student.getPhone()) %></td>
                            <td><%= HtmlUtil.escape(student.getDepartment()) %></td>
                            <td>
                                <span class="badge <%= "ACTIVE".equals(student.getStatus()) ? "text-bg-success" : "text-bg-secondary" %>">
                                    <%= HtmlUtil.escape(student.getStatus()) %>
                                </span>
                            </td>
                            <td class="text-end action-cell">
                                <a class="btn btn-sm btn-outline-secondary" href="<%= contextPath %>/students?action=view&id=<%= student.getStudentId() %>">
                                    <i class="bi bi-person-vcard"></i> Profile
                                </a>
                                <a class="btn btn-sm btn-outline-primary"
                                   href="<%= contextPath %>/students?action=edit&id=<%= student.getStudentId() %>">
                                    <i class="bi bi-pencil-square"></i> Edit
                                </a>
                                <form action="<%= contextPath %>/students" method="post" class="d-inline"
                                      onsubmit="return confirm('Delete this student?');">
                                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="studentId" value="<%= student.getStudentId() %>">
                                    <button type="submit" class="btn btn-sm btn-outline-danger">
                                        <i class="bi bi-person-dash"></i> Deactivate
                                    </button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                <% } %>
            </tbody>
        </table>
    </div>
    <% if (totalPages > 1) { String encodedKeyword = URLEncoder.encode(keyword == null ? "" : keyword, StandardCharsets.UTF_8.name()); %>
        <nav class="mt-3" aria-label="Student pages">
            <ul class="pagination justify-content-end mb-0">
                <li class="page-item <%= currentPage <= 1 ? "disabled" : "" %>"><a class="page-link" href="?keyword=<%= encodedKeyword %>&amp;page=<%= currentPage - 1 %>">Previous</a></li>
                <li class="page-item disabled"><span class="page-link">Page <%= currentPage %> of <%= totalPages %></span></li>
                <li class="page-item <%= currentPage >= totalPages ? "disabled" : "" %>"><a class="page-link" href="?keyword=<%= encodedKeyword %>&amp;page=<%= currentPage + 1 %>">Next</a></li>
            </ul>
        </nav>
    <% } %>
</section>

<%@ include file="includes/footer.jsp" %>
